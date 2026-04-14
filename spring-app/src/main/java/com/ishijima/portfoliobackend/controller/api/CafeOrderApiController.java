package com.ishijima.portfoliobackend.controller.api;

import com.ishijima.portfoliobackend.entity.CafeOrder;
import com.ishijima.portfoliobackend.entity.CafeOrderItem;
import com.ishijima.portfoliobackend.entity.CafeOrderStatus;
import com.ishijima.portfoliobackend.entity.CafeVisitSession;
import com.ishijima.portfoliobackend.entity.CafeVisitSessionStatus;
import com.ishijima.portfoliobackend.form.CafeAutoAssignForm;
import com.ishijima.portfoliobackend.form.CafeOrderCreateForm;
import com.ishijima.portfoliobackend.service.CafeOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cafe")
@RequiredArgsConstructor
public class CafeOrderApiController {

	private final CafeOrderService cafeOrderService;
	@Value("${app.frontend-base-url:}")
	private String frontendBaseUrl;
	@Value("${app.backend-base-url:}")
	private String backendBaseUrlProperty;

	@GetMapping("/menu")
	public ResponseEntity<Object> menu() {
		return ResponseEntity.ok(cafeOrderService.findAvailableMenus());
	}

	@PostMapping("/checkin")
	public ResponseEntity<Map<String, Object>> checkin(@Valid @RequestBody CafeAutoAssignForm form) {
		CafeVisitSession session = cafeOrderService.createVisitSessionAuto(form.getGuestCount());
		long remainingSeconds = calculateRemainingSeconds(session);
		return ResponseEntity.ok(Map.of(
			"message", "受付を発行しました。",
			"orderUrl", buildOrderUrl(session.getSessionToken()),
			"session", toSessionResponse(session, remainingSeconds)
		));
	}

	@GetMapping("/order-menu")
	public ResponseEntity<Map<String, Object>> orderMenu(@RequestParam("session") String sessionToken) {
		CafeVisitSession session = cafeOrderService.getVisitSession(sessionToken);
		long remainingSeconds = calculateRemainingSeconds(session);
		return ResponseEntity.ok(Map.of(
			"session", toSessionResponse(session, remainingSeconds),
			"menus", cafeOrderService.findAvailableMenus()
		));
	}

	@GetMapping("/sessions/{token}")
	public ResponseEntity<Map<String, Object>> sessionStatus(@PathVariable("token") String token) {
		CafeVisitSession session = cafeOrderService.getVisitSession(token);
		long remainingSeconds = calculateRemainingSeconds(session);
		return ResponseEntity.ok(toSessionResponse(session, remainingSeconds));
	}

	@PostMapping("/sessions/{token}/checkout")
	public ResponseEntity<Map<String, Object>> checkoutSession(@PathVariable("token") String token) {
		cafeOrderService.completeCheckout(token);
		CafeVisitSession session = cafeOrderService.getVisitSession(token);
		long remainingSeconds = calculateRemainingSeconds(session);
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", "お会計を確定しました。");
		response.put("session", toSessionResponse(session, remainingSeconds));
		return ResponseEntity.ok(response);
	}

	@GetMapping("/orders/history")
	public ResponseEntity<Map<String, Object>> orderHistory(@RequestParam("session") String sessionToken) {
		CafeVisitSession session = cafeOrderService.getVisitSession(sessionToken);
		List<CafeOrder> orders = cafeOrderService.findOrdersBySessionToken(sessionToken);
		Map<Long, List<CafeOrderItem>> itemMap = cafeOrderService.findOrderItemsMap(orders);

		List<Map<String, Object>> rows = orders.stream()
			.map(order -> Map.<String, Object>of(
				"orderId", order.getId(),
				"status", order.getStatus(),
				"createdAt", order.getCreatedAt(),
				"createdAtEpochMs", order.getCreatedAt() == null ? 0L : order.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli(),
				"total", order.getTotal(),
				"items", itemMap.getOrDefault(order.getId(), List.of()).stream()
					.map(item -> Map.of(
						"menuId", item.getMenuId(),
						"menuName", item.getMenuName(),
						"unitPrice", item.getUnitPrice(),
						"quantity", item.getQuantity(),
						"lineTotal", item.getLineTotal()
					))
					.toList()
			))
			.toList();

		int grandTotal = orders.stream()
			.filter(order -> order.getStatus() != CafeOrderStatus.CANCELLED)
			.mapToInt(order -> order.getTotal() != null ? order.getTotal() : 0)
			.sum();

		return ResponseEntity.ok(Map.of(
			"seatNo", session.getSeatNo(),
			"status", session.getStatus(),
			"orderCount", orders.size(),
			"grandTotal", grandTotal,
			"orders", rows
		));
	}

	@PostMapping("/orders")
	public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody CafeOrderCreateForm form) {
		CafeOrder order = cafeOrderService.createOrder(form);
		Map<String, Integer> itemSummary = form.getItems().stream()
			.collect(Collectors.toMap(
				CafeOrderCreateForm.Item::getMenuId,
				CafeOrderCreateForm.Item::getQuantity,
				Integer::sum,
				LinkedHashMap::new
			));

		return ResponseEntity.ok(Map.of(
			"message", "注文を受け付けました。",
			"orderId", order.getId(),
			"seatNo", order.getSeatNo(),
			"status", order.getStatus(),
			"total", order.getTotal(),
			"items", itemSummary
		));
	}

	private Map<String, Object> toSessionResponse(CafeVisitSession session, long remainingSeconds) {
		boolean canOrder = session.getStatus() == CafeVisitSessionStatus.ACTIVE && remainingSeconds > 0;
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("sessionToken", session.getSessionToken());
		response.put("seatNo", session.getSeatNo());
		response.put("status", session.getStatus());
		response.put("issuedAt", session.getIssuedAt());
		response.put("expiresAt", session.getExpiresAt());
		response.put("checkoutCompletedAt", session.getCheckoutCompletedAt());
		response.put("remainingSeconds", remainingSeconds);
		response.put("canOrder", canOrder);
		return response;
	}

	private long calculateRemainingSeconds(CafeVisitSession session) {
		if (session.getExpiresAt() == null) {
			return 0;
		}
		long seconds = Duration.between(LocalDateTime.now(), session.getExpiresAt()).getSeconds();
		return Math.max(0, seconds);
	}

	private String buildOrderUrl(String token) {
		String backendBaseUrl = (backendBaseUrlProperty != null && !backendBaseUrlProperty.isBlank())
			? backendBaseUrlProperty.trim().replaceAll("/+$", "")
			: ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		String frontBase = (frontendBaseUrl != null && !frontendBaseUrl.isBlank())
			? frontendBaseUrl.trim().replaceAll("/+$", "")
			: backendBaseUrl;
		return UriComponentsBuilder
			.fromUriString(frontBase + "/pages/cafe-order.html")
			.queryParam("session", token)
			.queryParam("apiBase", backendBaseUrl)
			.build()
			.toUriString();
	}
}
