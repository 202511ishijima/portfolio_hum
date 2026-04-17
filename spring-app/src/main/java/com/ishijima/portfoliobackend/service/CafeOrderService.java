package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.CafeMenu;
import com.ishijima.portfoliobackend.entity.CafeOrder;
import com.ishijima.portfoliobackend.entity.CafeOrderItem;
import com.ishijima.portfoliobackend.entity.CafeOrderStatus;
import com.ishijima.portfoliobackend.entity.CafeVisitSession;
import com.ishijima.portfoliobackend.dto.CafeSeatView;
import com.ishijima.portfoliobackend.form.CafeMenuUpdateForm;
import com.ishijima.portfoliobackend.form.CafeOrderCreateForm;

import java.util.List;
import java.util.Map;

public interface CafeOrderService {

	List<CafeMenu> findAllMenus();

	List<CafeMenu> findAvailableMenus();

	CafeVisitSession createVisitSession(String seatNo, Integer guestCount);

	CafeVisitSession createVisitSessionAuto(Integer guestCount);

	CafeVisitSession createVisitSessionAuto(Integer guestCount, String seatPreference);

	CafeVisitSession getVisitSession(String sessionToken);

	CafeVisitSession findLatestActiveSession();

	List<CafeVisitSession> findActiveSessions();

	List<CafeSeatView> buildSeatMap();

	CafeOrder createOrder(CafeOrderCreateForm form);

	List<CafeOrder> findAllOrders();

	List<CafeOrder> findOrdersBySessionToken(String sessionToken);

	Map<Long, List<CafeOrderItem>> findOrderItemsMap(List<CafeOrder> orders);

	CafeOrder updateOrderStatus(Long orderId, CafeOrderStatus status);

	void completeCheckout(String sessionToken);

	void updateMenu(String menuId, CafeMenuUpdateForm form);

	List<Map<String, Object>> findRecentDailySales(int limit);

	Map<String, Object> findSalesTarget();

	void updateSalesTarget(Integer dailyTarget, Integer monthlyTarget);
}
