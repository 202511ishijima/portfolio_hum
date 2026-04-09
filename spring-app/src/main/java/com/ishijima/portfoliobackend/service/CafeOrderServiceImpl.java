package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.dto.CafeSeatView;
import com.ishijima.portfoliobackend.entity.CafeMenu;
import com.ishijima.portfoliobackend.entity.CafeOrder;
import com.ishijima.portfoliobackend.entity.CafeOrderItem;
import com.ishijima.portfoliobackend.entity.CafeOrderStatus;
import com.ishijima.portfoliobackend.entity.CafeVisitSession;
import com.ishijima.portfoliobackend.entity.CafeVisitSessionStatus;
import com.ishijima.portfoliobackend.form.CafeMenuUpdateForm;
import com.ishijima.portfoliobackend.form.CafeOrderCreateForm;
import com.ishijima.portfoliobackend.mapper.CafeMenuMapper;
import com.ishijima.portfoliobackend.mapper.CafeOrderItemMapper;
import com.ishijima.portfoliobackend.mapper.CafeOrderMapper;
import com.ishijima.portfoliobackend.mapper.CafeSalesMapper;
import com.ishijima.portfoliobackend.mapper.CafeVisitSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CafeOrderServiceImpl implements CafeOrderService {

	private static final Pattern SEAT_NO_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,20}$");
	private static final long SESSION_MINUTES = 90;
	private static final List<String> SINGLE_SEATS = createSeatList("S", 12);
	private static final List<String> DOUBLE_SEATS = createSeatList("D", 8);
	private static final List<String> FOUR_SEATS = createSeatList("F", 8);

	private final CafeMenuMapper cafeMenuMapper;
	private final CafeOrderMapper cafeOrderMapper;
	private final CafeOrderItemMapper cafeOrderItemMapper;
	private final CafeSalesMapper cafeSalesMapper;
	private final CafeVisitSessionMapper cafeVisitSessionMapper;

	@Override
	public List<CafeMenu> findAllMenus() {
		return cafeMenuMapper.findAll();
	}

	@Override
	public List<CafeMenu> findAvailableMenus() {
		return cafeMenuMapper.findAvailable();
	}

	@Override
	@Transactional
	public CafeVisitSession createVisitSession(String seatNo, Integer guestCount) {
		String normalizedSeatNo = normalizeSeatNo(seatNo);
		int safeGuestCount = guestCount == null || guestCount <= 0 ? 1 : guestCount;
		ensureSeatExists(normalizedSeatNo);
		ensureSeatAvailable(normalizedSeatNo);
		LocalDateTime now = LocalDateTime.now();

		CafeVisitSession session = CafeVisitSession.builder()
			.sessionToken(UUID.randomUUID().toString())
			.seatNo(normalizedSeatNo)
			.guestCount(safeGuestCount)
			.status(CafeVisitSessionStatus.ACTIVE)
			.issuedAt(now)
			.expiresAt(now.plusMinutes(SESSION_MINUTES))
			.checkoutCompletedAt(null)
			.updatedAt(now)
			.build();
		cafeVisitSessionMapper.insert(session);
		return session;
	}

	@Override
	@Transactional
	public CafeVisitSession createVisitSessionAuto(Integer guestCount) {
		return createVisitSessionAuto(guestCount, "AUTO");
	}

	@Override
	@Transactional
	public CafeVisitSession createVisitSessionAuto(Integer guestCount, String seatPreference) {
		int safeGuestCount = guestCount == null || guestCount <= 0 ? 1 : guestCount;
		if (safeGuestCount > 4) {
			throw new IllegalArgumentException("1回の受付は4名まで対応しています。");
		}
		String seatNo = assignSeatAutomatically(safeGuestCount, seatPreference);
		return createVisitSession(seatNo, safeGuestCount);
	}

	@Override
	@Transactional
	public CafeVisitSession getVisitSession(String sessionToken) {
		CafeVisitSession session = findSessionOrThrow(sessionToken);
		return applyExpiryIfNeeded(session);
	}

	@Override
	@Transactional
	public CafeVisitSession findLatestActiveSession() {
		return cafeVisitSessionMapper.findActiveSessions(LocalDateTime.now()).stream()
			.findFirst()
			.orElse(null);
	}

	@Override
	@Transactional
	public List<CafeVisitSession> findActiveSessions() {
		return cafeVisitSessionMapper.findActiveSessions(LocalDateTime.now());
	}

	@Override
	@Transactional
	public List<CafeSeatView> buildSeatMap() {
		Set<String> occupied = getActiveSeatSet();
		List<CafeSeatView> map = new ArrayList<>();
		SINGLE_SEATS.forEach(seat -> map.add(buildSeatView(seat, "1人席", 1, occupied.contains(seat))));
		DOUBLE_SEATS.forEach(seat -> map.add(buildSeatView(seat, "2人席", 2, occupied.contains(seat))));
		FOUR_SEATS.forEach(seat -> map.add(buildSeatView(seat, "4人テーブル", 4, occupied.contains(seat))));
		return map;
	}

	@Override
	@Transactional
	public CafeOrder createOrder(CafeOrderCreateForm form) {
		if (form == null || form.getItems() == null || form.getItems().isEmpty()) {
			throw new IllegalArgumentException("注文商品を1件以上選択してください。");
		}

		CafeVisitSession session = ensureOrderAllowed(form.getSessionToken());
		LocalDateTime now = LocalDateTime.now();

		int subtotal = 0;
		Map<String, CafeMenu> menuMap = new LinkedHashMap<>();
		for (CafeOrderCreateForm.Item item : form.getItems()) {
			CafeMenu menu = cafeMenuMapper.findById(item.getMenuId())
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません: " + item.getMenuId()));
			if (!Boolean.TRUE.equals(menu.getAvailable())) {
				throw new IllegalArgumentException("現在注文できない商品が含まれています: " + menu.getName());
			}
			if (item.getQuantity() == null || item.getQuantity() <= 0) {
				throw new IllegalArgumentException("数量は1以上で入力してください。");
			}
			menuMap.put(menu.getId(), menu);
			subtotal += menu.getPrice() * item.getQuantity();
		}

		int tax = Math.round(subtotal * 0.1f);
		int total = subtotal + tax;

		CafeOrder order = CafeOrder.builder()
			.visitSessionId(session.getId())
			.seatNo(session.getSeatNo())
			.status(CafeOrderStatus.NEW)
			.subtotal(subtotal)
			.tax(tax)
			.total(total)
			.createdAt(now)
			.updatedAt(now)
			.paidAt(null)
			.build();
		cafeOrderMapper.insert(order);

		for (CafeOrderCreateForm.Item item : form.getItems()) {
			CafeMenu menu = menuMap.get(item.getMenuId());
			CafeOrderItem orderItem = CafeOrderItem.builder()
				.orderId(order.getId())
				.menuId(menu.getId())
				.menuName(menu.getName())
				.unitPrice(menu.getPrice())
				.quantity(item.getQuantity())
				.lineTotal(menu.getPrice() * item.getQuantity())
				.createdAt(now)
				.build();
			cafeOrderItemMapper.insert(orderItem);
		}

		return cafeOrderMapper.findById(order.getId()).orElse(order);
	}

	@Override
	public List<CafeOrder> findAllOrders() {
		return cafeOrderMapper.findAll();
	}

	@Override
	@Transactional
	public List<CafeOrder> findOrdersBySessionToken(String sessionToken) {
		CafeVisitSession session = findSessionOrThrow(sessionToken);
		session = applyExpiryIfNeeded(session);
		Long sessionId = session.getId();
		return cafeOrderMapper.findAll().stream()
			.filter(order -> sessionId.equals(order.getVisitSessionId()))
			.sorted(Comparator.comparing(CafeOrder::getCreatedAt))
			.toList();
	}

	@Override
	public Map<Long, List<CafeOrderItem>> findOrderItemsMap(List<CafeOrder> orders) {
		Map<Long, List<CafeOrderItem>> map = new LinkedHashMap<>();
		if (orders == null) {
			return map;
		}
		for (CafeOrder order : orders) {
			map.put(order.getId(), cafeOrderItemMapper.findByOrderId(order.getId()));
		}
		return map;
	}

	@Override
	@Transactional
	public CafeOrder updateOrderStatus(Long orderId, CafeOrderStatus status) {
		CafeOrder order = cafeOrderMapper.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("注文が見つかりません。"));
		if (status == null) {
			throw new IllegalArgumentException("ステータスが不正です。");
		}
		if (order.getStatus() == status) {
			return order;
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime paidAt = order.getPaidAt();
		boolean transitioningToPaid = status == CafeOrderStatus.PAID && order.getStatus() != CafeOrderStatus.PAID;
		if (transitioningToPaid) {
			paidAt = now;
		}
		if (status != CafeOrderStatus.PAID) {
			paidAt = null;
		}

		cafeOrderMapper.updateStatus(orderId, status, now, paidAt);
		CafeOrder updated = cafeOrderMapper.findById(orderId).orElseThrow();

		if (transitioningToPaid) {
			LocalDate salesDate = (updated.getPaidAt() != null ? updated.getPaidAt() : now).toLocalDate();
			int updatedRows = cafeSalesMapper.addToDailySales(salesDate, 1, updated.getTotal());
			if (updatedRows == 0) {
				cafeSalesMapper.insertDailySales(salesDate, 1, updated.getTotal());
			}
			if (updated.getVisitSessionId() != null) {
				CafeVisitSession session = cafeVisitSessionMapper.findById(updated.getVisitSessionId()).orElse(null);
				if (session != null) {
					markSessionCheckedOut(session);
				}
			}
		}

		return updated;
	}

	@Override
	@Transactional
	public void completeCheckout(String sessionToken) {
		CafeVisitSession session = findSessionOrThrow(sessionToken);
		markSessionCheckedOut(session);
	}

	@Override
	@Transactional
	public void updateMenu(String menuId, CafeMenuUpdateForm form) {
		if (menuId == null || menuId.isBlank()) {
			throw new IllegalArgumentException("商品IDが不正です。");
		}
		if (form == null || form.getPrice() == null || form.getPrice() < 0) {
			throw new IllegalArgumentException("価格は0以上で入力してください。");
		}
		if (form.getName() == null || form.getName().trim().isEmpty()) {
			throw new IllegalArgumentException("メニュー名を入力してください。");
		}
		int updated = cafeMenuMapper.updateNamePriceAndAvailability(
			menuId,
			form.getName().trim(),
			form.getPrice(),
			form.isAvailable()
		);
		if (updated == 0) {
			throw new IllegalArgumentException("更新対象の商品が見つかりません。");
		}
	}

	@Override
	public List<Map<String, Object>> findRecentDailySales(int limit) {
		return cafeSalesMapper.findDailySalesRecent(limit);
	}

	private CafeVisitSession ensureOrderAllowed(String sessionToken) {
		CafeVisitSession session = findSessionOrThrow(sessionToken);
		session = applyExpiryIfNeeded(session);
		if (session.getStatus() == CafeVisitSessionStatus.CHECKED_OUT) {
			throw new IllegalArgumentException("会計が完了しているため注文できません。ありがとうございました。");
		}
		if (session.getStatus() == CafeVisitSessionStatus.EXPIRED) {
			throw new IllegalArgumentException("注文可能時間を過ぎました。ありがとうございました。");
		}
		return session;
	}

	private CafeVisitSession findSessionOrThrow(String sessionToken) {
		if (sessionToken == null || sessionToken.isBlank()) {
			throw new IllegalArgumentException("セッションが不正です。");
		}
		return cafeVisitSessionMapper.findByToken(sessionToken.trim())
			.orElseThrow(() -> new IllegalArgumentException("セッションが見つかりません。"));
	}

	private CafeVisitSession applyExpiryIfNeeded(CafeVisitSession session) {
		if (session.getStatus() == CafeVisitSessionStatus.ACTIVE
			&& session.getExpiresAt() != null
			&& session.getExpiresAt().isBefore(LocalDateTime.now())) {
			cafeVisitSessionMapper.updateStatus(
				session.getId(),
				CafeVisitSessionStatus.EXPIRED,
				session.getCheckoutCompletedAt(),
				LocalDateTime.now()
			);
			session.setStatus(CafeVisitSessionStatus.EXPIRED);
		}
		return session;
	}

	private void markSessionCheckedOut(CafeVisitSession session) {
		if (session.getStatus() == CafeVisitSessionStatus.CHECKED_OUT) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		cafeVisitSessionMapper.updateStatus(
			session.getId(),
			CafeVisitSessionStatus.CHECKED_OUT,
			now,
			now
		);
	}

	private String normalizeSeatNo(String rawSeatNo) {
		if (rawSeatNo == null || rawSeatNo.isBlank()) {
			throw new IllegalArgumentException("座席番号を入力してください。");
		}
		String trimmed = rawSeatNo.trim().toUpperCase();
		if (!SEAT_NO_PATTERN.matcher(trimmed).matches()) {
			throw new IllegalArgumentException("座席番号の形式が不正です。");
		}
		return trimmed;
	}

	private CafeSeatView buildSeatView(String seatNo, String seatType, int capacity, boolean occupied) {
		return CafeSeatView.builder()
			.seatNo(seatNo)
			.seatType(seatType)
			.capacity(capacity)
			.occupied(occupied)
			.build();
	}

	private String assignSeatAutomatically(int guestCount, String rawSeatPreference) {
		String seatPreference = rawSeatPreference == null ? "AUTO" : rawSeatPreference.trim().toUpperCase();
		List<List<String>> priorityGroups = new ArrayList<>();
		if ("COUNTER".equals(seatPreference)) {
			if (guestCount > 1) {
				throw new IllegalArgumentException("カウンター席は1名のみ指定できます。");
			}
			priorityGroups.add(SINGLE_SEATS);
		} else if ("TABLE".equals(seatPreference)) {
			if (guestCount <= 2) {
				priorityGroups.add(DOUBLE_SEATS);
				priorityGroups.add(FOUR_SEATS);
			} else {
				priorityGroups.add(FOUR_SEATS);
			}
		} else {
			if (guestCount == 1) {
				priorityGroups.add(SINGLE_SEATS);
				priorityGroups.add(DOUBLE_SEATS);
				priorityGroups.add(FOUR_SEATS);
			} else if (guestCount == 2) {
				priorityGroups.add(DOUBLE_SEATS);
				priorityGroups.add(FOUR_SEATS);
			} else {
				priorityGroups.add(FOUR_SEATS);
			}
		}

		Set<String> occupied = getActiveSeatSet();
		for (List<String> seats : priorityGroups) {
			List<String> available = seats.stream().filter(seat -> !occupied.contains(seat)).toList();
			if (available.isEmpty()) {
				continue;
			}
			List<String> oneGapCandidates = available.stream()
				.filter(seat -> !isAdjacentToOccupied(seat, occupied))
				.sorted(Comparator.naturalOrder())
				.toList();
			if (!oneGapCandidates.isEmpty()) {
				return oneGapCandidates.get(0);
			}
			return available.stream().sorted().findFirst().orElseThrow();
		}

		throw new IllegalArgumentException("空席がありません。");
	}

	private boolean isAdjacentToOccupied(String seatNo, Set<String> occupied) {
		char zone = seatNo.charAt(0);
		int number = Integer.parseInt(seatNo.substring(1));
		String prev = zone + String.valueOf(number - 1);
		String next = zone + String.valueOf(number + 1);
		return occupied.contains(prev) || occupied.contains(next);
	}

	private Set<String> getActiveSeatSet() {
		Set<String> occupied = new HashSet<>();
		List<CafeVisitSession> sessions = cafeVisitSessionMapper.findActiveSessions(LocalDateTime.now());
		for (CafeVisitSession session : sessions) {
			occupied.add(session.getSeatNo());
		}
		return occupied;
	}

	private void ensureSeatExists(String seatNo) {
		boolean exists = SINGLE_SEATS.contains(seatNo) || DOUBLE_SEATS.contains(seatNo) || FOUR_SEATS.contains(seatNo);
		if (!exists) {
			throw new IllegalArgumentException("存在しない座席番号です。");
		}
	}

	private void ensureSeatAvailable(String seatNo) {
		Set<String> occupied = getActiveSeatSet();
		if (occupied.contains(seatNo)) {
			throw new IllegalArgumentException("その座席は現在利用中です。");
		}
	}

	private static List<String> createSeatList(String prefix, int count) {
		List<String> list = new ArrayList<>();
		for (int index = 1; index <= count; index++) {
			list.add(prefix + index);
		}
		return list;
	}
}
