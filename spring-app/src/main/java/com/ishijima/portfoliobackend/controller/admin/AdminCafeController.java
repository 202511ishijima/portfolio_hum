package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.CafeOrder;
import com.ishijima.portfoliobackend.entity.CafeOrderStatus;
import com.ishijima.portfoliobackend.entity.CafeVisitSession;
import com.ishijima.portfoliobackend.form.CafeAutoAssignForm;
import com.ishijima.portfoliobackend.form.CafeCustomerIssueForm;
import com.ishijima.portfoliobackend.form.CafeMenuUpdateForm;
import com.ishijima.portfoliobackend.form.CafeOrderStatusUpdateForm;
import com.ishijima.portfoliobackend.form.CafeSessionCreateForm;
import com.ishijima.portfoliobackend.service.CafeOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/cafe")
@RequiredArgsConstructor
public class AdminCafeController {

	private final CafeOrderService cafeOrderService;

	@GetMapping("/reception")
	public String reception(Model model) {
		if (!model.containsAttribute("cafeSessionCreateForm")) {
			CafeSessionCreateForm form = new CafeSessionCreateForm();
			form.setGuestCount(1);
			model.addAttribute("cafeSessionCreateForm", form);
		}
		if (!model.containsAttribute("cafeAutoAssignForm")) {
			CafeAutoAssignForm autoForm = new CafeAutoAssignForm();
			autoForm.setGuestCount(1);
			model.addAttribute("cafeAutoAssignForm", autoForm);
		}
		List<ReceptionSessionView> activeSessionViews = new ArrayList<>();
		for (CafeVisitSession session : cafeOrderService.findActiveSessions()) {
			String orderUrl = buildOrderUrl(session.getSessionToken());
			activeSessionViews.add(new ReceptionSessionView(
				session,
				orderUrl,
				buildQrImageUrl(orderUrl)
			));
		}
		model.addAttribute("activeSessionViews", activeSessionViews);
		model.addAttribute("seatMap", cafeOrderService.buildSeatMap());
		model.addAttribute("adminSection", "cafe-reception");
		return "admin/cafe-reception";
	}

	@GetMapping("/customer-screen")
	public String customerScreen(Model model) {
		if (!model.containsAttribute("cafeCustomerIssueForm")) {
			CafeCustomerIssueForm form = new CafeCustomerIssueForm();
			form.setGuestCount(1);
			form.setSeatPreference("AUTO");
			model.addAttribute("cafeCustomerIssueForm", form);
		}
		model.addAttribute("adminSection", "cafe-customer");
		return "admin/cafe-customer-screen";
	}

	@GetMapping("/customer-screen/receipt/{token}")
	public String customerReceipt(
		@PathVariable("token") String token,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		try {
			CafeVisitSession session = cafeOrderService.getVisitSession(token);
			String orderUrl = buildOrderUrl(session.getSessionToken());
			model.addAttribute("issuedSession", session);
			model.addAttribute("issuedOrderUrl", orderUrl);
			model.addAttribute("issuedQrDataUrl", buildQrImageUrl(orderUrl));
			model.addAttribute("adminSection", "cafe-customer");
			return "admin/cafe-customer-receipt";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
			return "redirect:/admin/cafe/customer-screen";
		}
	}

	@PostMapping("/customer-screen/issue")
	public String issueFromCustomerScreen(
		@Valid CafeCustomerIssueForm form,
		BindingResult bindingResult,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "受付発行の権限がありません。");
			return "redirect:/admin/cafe/customer-screen";
		}
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("cafeError", "入力内容を確認してください。");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cafeCustomerIssueForm", bindingResult);
			redirectAttributes.addFlashAttribute("cafeCustomerIssueForm", form);
			return "redirect:/admin/cafe/customer-screen";
		}
		try {
			CafeVisitSession session = cafeOrderService.createVisitSessionAuto(form.getGuestCount(), form.getSeatPreference());
			return "redirect:/admin/cafe/customer-screen/receipt/" + session.getSessionToken();
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
			redirectAttributes.addFlashAttribute("cafeCustomerIssueForm", form);
			return "redirect:/admin/cafe/customer-screen";
		}
	}

	@PostMapping("/reception")
	public String createSession(
		@Valid CafeSessionCreateForm form,
		BindingResult bindingResult,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "受付発行の権限がありません。");
			return "redirect:/admin/cafe/reception";
		}
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("cafeError", "入力内容を確認してください。");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cafeSessionCreateForm", bindingResult);
			redirectAttributes.addFlashAttribute("cafeSessionCreateForm", form);
			return "redirect:/admin/cafe/reception";
		}
		try {
			CafeVisitSession session = cafeOrderService.createVisitSession(form.getSeatNo(), form.getGuestCount());
			String orderUrl = buildOrderUrl(session.getSessionToken());
			redirectAttributes.addFlashAttribute("cafeMessage", "受付を発行しました。");
			redirectAttributes.addFlashAttribute("issuedSession", session);
			redirectAttributes.addFlashAttribute("issuedOrderUrl", orderUrl);
			redirectAttributes.addFlashAttribute("issuedQrDataUrl", buildQrImageUrl(orderUrl));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("cafeError", "受付処理でエラーが発生しました: " + ex.getMessage());
		}
		return "redirect:/admin/cafe/reception";
	}

	@PostMapping("/reception/auto")
	public String createSessionAuto(
		@Valid CafeAutoAssignForm form,
		BindingResult bindingResult,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "受付発行の権限がありません。");
			return "redirect:/admin/cafe/reception";
		}
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("cafeError", "入力内容を確認してください。");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cafeAutoAssignForm", bindingResult);
			redirectAttributes.addFlashAttribute("cafeAutoAssignForm", form);
			return "redirect:/admin/cafe/reception";
		}
		try {
			CafeVisitSession session = cafeOrderService.createVisitSessionAuto(form.getGuestCount());
			String orderUrl = buildOrderUrl(session.getSessionToken());
			redirectAttributes.addFlashAttribute("cafeMessage", "自動割り当てで受付を発行しました。");
			redirectAttributes.addFlashAttribute("issuedSession", session);
			redirectAttributes.addFlashAttribute("issuedOrderUrl", orderUrl);
			redirectAttributes.addFlashAttribute("issuedQrDataUrl", buildQrImageUrl(orderUrl));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("cafeError", "自動割り当て受付でエラーが発生しました: " + ex.getMessage());
		}
		return "redirect:/admin/cafe/reception";
	}

	@GetMapping("/reception/qr/{token}")
	public String receptionQrFallback(@PathVariable("token") String token, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("cafeError", "QR画像表示は現在調整中です。注文URLをご利用ください。");
		return "redirect:/admin/cafe/reception";
	}

	@GetMapping("/orders")
	public String orders(Model model) {
		List<CafeOrder> allOrders = cafeOrderService.findAllOrders();
		List<CafeOrder> activeOrders = allOrders.stream()
			.filter(order -> order.getStatus() == CafeOrderStatus.NEW || order.getStatus() == CafeOrderStatus.PREPARING)
			.toList();
		List<CafeOrder> historyOrders = allOrders.stream()
			.filter(order -> order.getStatus() != CafeOrderStatus.NEW && order.getStatus() != CafeOrderStatus.PREPARING)
			.toList();

		Map<Long, String> elapsedMap = activeOrders.stream()
			.collect(java.util.stream.Collectors.toMap(
				CafeOrder::getId,
				order -> formatElapsed(order.getCreatedAt()),
				(existing, replacement) -> existing,
				java.util.LinkedHashMap::new
			));

		Map<Long, Long> pendingCountBySessionId = activeOrders.stream()
			.filter(order -> order.getVisitSessionId() != null)
			.collect(Collectors.groupingBy(CafeOrder::getVisitSessionId, Collectors.counting()));

		List<CheckoutSessionRow> checkoutSessionRows = cafeOrderService.findActiveSessions().stream()
			.map(session -> new CheckoutSessionRow(
				session.getSessionToken(),
				session.getSeatNo(),
				session.getGuestCount(),
				session.getExpiresAt(),
				pendingCountBySessionId.getOrDefault(session.getId(), 0L).intValue()
			))
			.toList();

		model.addAttribute("activeOrders", activeOrders);
		model.addAttribute("historyOrders", historyOrders);
		model.addAttribute("orderItemsMap", cafeOrderService.findOrderItemsMap(allOrders));
		model.addAttribute("elapsedMap", elapsedMap);
		model.addAttribute("checkoutSessionRows", checkoutSessionRows);
		model.addAttribute("salesDaily", cafeOrderService.findRecentDailySales(14));
		model.addAttribute("adminSection", "cafe-orders");
		return "admin/cafe-orders";
	}

	@PostMapping("/orders/sessions/{token}/checkout")
	public String checkoutFromOrders(
		@PathVariable String token,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "会計処理の権限がありません。");
			return "redirect:/admin/cafe/orders";
		}
		try {
			cafeOrderService.completeCheckout(token);
			redirectAttributes.addFlashAttribute("cafeMessage", "会計を確定しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		}
		return "redirect:/admin/cafe/orders";
	}

	@PostMapping("/orders/{id}/serve")
	public String serveOrder(
		@PathVariable Long id,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "提供処理の権限がありません。");
			return "redirect:/admin/cafe/orders";
		}
		try {
			cafeOrderService.updateOrderStatus(id, CafeOrderStatus.SERVED);
			redirectAttributes.addFlashAttribute("cafeMessage", "提供済みに更新しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		}
		return "redirect:/admin/cafe/orders";
	}

	@PostMapping("/orders/{id}/status")
	public String updateStatus(
		@PathVariable Long id,
		@Valid CafeOrderStatusUpdateForm form,
		BindingResult bindingResult,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "注文ステータス変更の権限がありません。");
			return "redirect:/admin/cafe/orders";
		}
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("cafeError", "ステータスを選択してください。");
			return "redirect:/admin/cafe/orders";
		}

		try {
			CafeOrderStatus status = CafeOrderStatus.valueOf(form.getStatus().trim().toUpperCase());
			cafeOrderService.updateOrderStatus(id, status);
			redirectAttributes.addFlashAttribute("cafeMessage", "注文ステータスを更新しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		}
		return "redirect:/admin/cafe/orders";
	}

	@PostMapping("/sessions/{token}/checkout")
	public String checkout(
		@PathVariable String token,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "会計完了処理の権限がありません。");
			return "redirect:/admin/cafe/reception";
		}
		try {
			cafeOrderService.completeCheckout(token);
			redirectAttributes.addFlashAttribute("cafeMessage", "会計を完了しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		}
		return "redirect:/admin/cafe/reception";
	}

	@GetMapping("/menu")
	public String menu(Model model) {
		model.addAttribute("menus", cafeOrderService.findAllMenus());
		model.addAttribute("adminSection", "cafe-menu");
		return "admin/cafe-menu";
	}

	@PostMapping("/menu/{id}/update")
	public String updateMenu(
		@PathVariable String id,
		@Valid CafeMenuUpdateForm form,
		BindingResult bindingResult,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "メニュー設定変更の権限がありません。");
			return "redirect:/admin/cafe/menu";
		}
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("cafeError", "価格を確認してください。");
			return "redirect:/admin/cafe/menu";
		}
		try {
			cafeOrderService.updateMenu(id, form);
			redirectAttributes.addFlashAttribute("cafeMessage", "メニュー価格を更新しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		}
		return "redirect:/admin/cafe/menu";
	}

	@PostMapping("/menu/bulk-update")
	public String bulkUpdateMenu(
		@RequestParam Map<String, String> params,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("cafeError", "メニュー設定変更の権限がありません。");
			return "redirect:/admin/cafe/menu";
		}
		Set<String> menuIds = new HashSet<>();
		for (String key : params.keySet()) {
			if (key.startsWith("name_")) {
				menuIds.add(key.substring("name_".length()));
			}
		}
		if (menuIds.isEmpty()) {
			redirectAttributes.addFlashAttribute("cafeError", "更新対象のメニューがありません。");
			return "redirect:/admin/cafe/menu";
		}
		try {
			int updatedCount = 0;
			for (String menuId : menuIds) {
				CafeMenuUpdateForm form = new CafeMenuUpdateForm();
				form.setName(params.get("name_" + menuId));
				String priceRaw = params.get("price_" + menuId);
				if (priceRaw == null || priceRaw.isBlank()) {
					throw new IllegalArgumentException("価格を入力してください。");
				}
				form.setPrice(Integer.parseInt(priceRaw.trim()));
				form.setAvailable(params.containsKey("available_" + menuId));
				cafeOrderService.updateMenu(menuId, form);
				updatedCount++;
			}
			redirectAttributes.addFlashAttribute("cafeMessage", updatedCount + "件のメニューを更新しました。");
		} catch (NumberFormatException ex) {
			redirectAttributes.addFlashAttribute("cafeError", "価格は数値で入力してください。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		}
		return "redirect:/admin/cafe/menu";
	}

	private boolean canManage(Authentication authentication) {
		if (authentication == null || authentication.getAuthorities() == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
			.anyMatch(authority ->
				"ROLE_ADMIN".equals(authority.getAuthority()) ||
				"ROLE_STAFF_MANAGER".equals(authority.getAuthority())
			);
	}

	private String buildOrderUrl(String token) {
		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
			.build()
			.toUriString();
		return baseUrl + "/pages/cafe-order.html?session=" + token;
	}

	private String buildQrImageUrl(String text) {
		String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
		return "https://api.qrserver.com/v1/create-qr-code/?size=320x320&margin=8&data=" + encoded;
	}

	private String formatElapsed(LocalDateTime createdAt) {
		if (createdAt == null) {
			return "-";
		}
		long minutes = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toMinutes());
		long hours = minutes / 60;
		long remain = minutes % 60;
		if (hours > 0) {
			return hours + "時間" + remain + "分";
		}
		return remain + "分";
	}

	private record ReceptionSessionView(
		CafeVisitSession session,
		String orderUrl,
		String qrDataUrl
	) {}

	private record CheckoutSessionRow(
		String sessionToken,
		String seatNo,
		Integer guestCount,
		LocalDateTime expiresAt,
		int pendingCount
	) {}

	@ExceptionHandler(Exception.class)
	public String handleReceptionException(Exception ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("cafeError", "受付処理でエラーが発生しました: " + ex.getMessage());
		return "redirect:/admin/cafe/reception";
	}
}
