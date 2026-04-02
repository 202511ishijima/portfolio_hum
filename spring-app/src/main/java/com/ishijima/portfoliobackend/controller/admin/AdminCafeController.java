package com.ishijima.portfoliobackend.controller.admin;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ishijima.portfoliobackend.entity.CafeOrder;
import com.ishijima.portfoliobackend.entity.CafeOrderStatus;
import com.ishijima.portfoliobackend.entity.CafeVisitSession;
import com.ishijima.portfoliobackend.form.CafeAutoAssignForm;
import com.ishijima.portfoliobackend.form.CafeMenuUpdateForm;
import com.ishijima.portfoliobackend.form.CafeOrderStatusUpdateForm;
import com.ishijima.portfoliobackend.form.CafeSessionCreateForm;
import com.ishijima.portfoliobackend.service.CafeOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/admin/cafe")
@RequiredArgsConstructor
public class AdminCafeController {

	private static final String FRONT_ORDER_BASE_URL = "http://127.0.0.1:3000/pages/cafe-order.html?session=";

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
		model.addAttribute("seatMap", cafeOrderService.buildSeatMap());
		model.addAttribute("adminSection", "cafe-reception");
		return "admin/cafe-reception";
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
			redirectAttributes.addFlashAttribute("issuedQrDataUrl", buildQrDataUrl(orderUrl));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("cafeError", "受付発行でエラーが発生しました: " + ex.getMessage());
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
			redirectAttributes.addFlashAttribute("issuedQrDataUrl", buildQrDataUrl(orderUrl));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("cafeError", ex.getMessage());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("cafeError", "自動割り当て発行でエラーが発生しました: " + ex.getMessage());
		}
		return "redirect:/admin/cafe/reception";
	}

	@GetMapping(value = "/reception/qr/{token}", produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseBody
	public ResponseEntity<byte[]> receptionQr(@PathVariable("token") String token) {
		try {
			cafeOrderService.getVisitSession(token);
			String orderUrl = buildOrderUrl(token);

			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix bitMatrix = qrCodeWriter.encode(orderUrl, BarcodeFormat.QR_CODE, 320, 320);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

			return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.body(outputStream.toByteArray());
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.contentType(MediaType.TEXT_PLAIN)
				.body(("QR生成に失敗しました: " + ex.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
	}

	@GetMapping("/orders")
	public String orders(
		@RequestParam(value = "status", required = false) String status,
		Model model
	) {
		List<CafeOrder> orders = cafeOrderService.findAllOrders();
		if (status != null && !status.isBlank()) {
			try {
				CafeOrderStatus selected = CafeOrderStatus.valueOf(status.trim().toUpperCase());
				orders = orders.stream()
					.filter(order -> order.getStatus() == selected)
					.toList();
			} catch (IllegalArgumentException ignored) {
				// ignore invalid filter and show all
			}
		}

		model.addAttribute("orders", orders);
		model.addAttribute("orderItemsMap", cafeOrderService.findOrderItemsMap(orders));
		model.addAttribute("statusOptions", CafeOrderStatus.values());
		model.addAttribute("selectedStatus", status != null ? status.toUpperCase() : "");
		model.addAttribute("salesDaily", cafeOrderService.findRecentDailySales(14));
		model.addAttribute("adminSection", "cafe-orders");
		return "admin/cafe-orders";
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
			redirectAttributes.addFlashAttribute("cafeError", "注文ステータス更新の権限がありません。");
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
			redirectAttributes.addFlashAttribute("cafeError", "会計処理の権限がありません。");
			return "redirect:/admin/cafe/reception";
		}
		try {
			cafeOrderService.completeCheckout(token);
			redirectAttributes.addFlashAttribute("cafeMessage", "会計完了にしました。以後このセッションは注文できません。");
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
			redirectAttributes.addFlashAttribute("cafeError", "メニュー編集の権限がありません。");
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
		return FRONT_ORDER_BASE_URL + token;
	}

	private String buildQrDataUrl(String text) {
		try {
			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 320, 320);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
			return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
		} catch (Exception ex) {
			return null;
		}
	}
}
