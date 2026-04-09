package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.dto.ProductCatalogItem;
import com.ishijima.portfoliobackend.entity.ProductOrder;
import com.ishijima.portfoliobackend.entity.ProductStock;
import com.ishijima.portfoliobackend.form.ProductOrderBulkForm;
import com.ishijima.portfoliobackend.form.ProductOrderLineForm;
import com.ishijima.portfoliobackend.form.ProductStockAdjustForm;
import com.ishijima.portfoliobackend.service.ProductCatalogService;
import com.ishijima.portfoliobackend.service.ProductInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductInventoryController {

	private final ProductInventoryService productInventoryService;
	private final ProductCatalogService productCatalogService;

	@GetMapping("/stocks")
	public String stocks(Model model, Authentication authentication) {
		populateProductTableModel(model);
		Map<String, ProductCatalogItem> catalogMap = productCatalogService.findAll().stream()
			.collect(Collectors.toMap(ProductCatalogItem::id, item -> item, (left, right) -> left, LinkedHashMap::new));
		model.addAttribute("recentOrderGroups", buildRecentOrderGroups(productInventoryService.findRecentOrders(), catalogMap));
		model.addAttribute("canManageProductStock", canManage(authentication));
		model.addAttribute("adminSection", "products");
		return "admin/product-stocks";
	}

	@GetMapping("/orders/{groupKey}")
	public String orderDetail(
		@PathVariable String groupKey,
		Model model,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("productError", "発注履歴の閲覧権限がありません。");
			return "redirect:/admin/products/stocks";
		}

		List<ProductOrderRowView> rows;
		if (groupKey != null && groupKey.startsWith("legacy-")) {
			rows = productInventoryService.findRecentOrders().stream()
				.map(ProductOrderRowView::from)
				.filter(row -> groupKey.equals(row.groupKey()))
				.toList();
		} else {
			try {
				rows = productInventoryService.findOrdersByGroupId(groupKey).stream()
					.map(ProductOrderRowView::from)
					.toList();
			} catch (Exception ex) {
				rows = List.of();
			}
		}

		if (rows.isEmpty()) {
			redirectAttributes.addFlashAttribute("productError", "指定した発注履歴が見つかりません。");
			return "redirect:/admin/products/stocks";
		}

		Map<String, ProductCatalogItem> catalogMap = productCatalogService.findAll().stream()
			.collect(Collectors.toMap(ProductCatalogItem::id, item -> item, (left, right) -> left, LinkedHashMap::new));
		Map<String, String> productNames = rows.stream()
			.collect(Collectors.toMap(
				ProductOrderRowView::productId,
				row -> catalogMap.containsKey(row.productId()) ? catalogMap.get(row.productId()).name() : row.productId(),
				(left, right) -> left,
				LinkedHashMap::new
			));
		Map<String, String> productCategories = rows.stream()
			.collect(Collectors.toMap(
				ProductOrderRowView::productId,
				row -> toCategoryLabel(catalogMap.containsKey(row.productId()) ? catalogMap.get(row.productId()).category() : ""),
				(left, right) -> left,
				LinkedHashMap::new
			));

		ProductOrderGroupView group = buildOrderGroupView(rows, catalogMap);
		model.addAttribute("group", group);
		model.addAttribute("rows", rows);
		model.addAttribute("productNames", productNames);
		model.addAttribute("productCategories", productCategories);
		model.addAttribute("adminSection", "products");
		return "admin/product-order-detail";
	}

	@GetMapping("/inventory")
	public String inventory(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("productError", "棚卸は本部・店長・副店長のみ実行できます。");
			return "redirect:/admin/products/stocks";
		}
		populateProductTableModel(model);
		model.addAttribute("canManageProductStock", true);
		model.addAttribute("adminSection", "products");
		return "admin/product-inventory";
	}

	@PostMapping("/inventory/{productId}/adjust")
	public String adjustStock(
		@PathVariable String productId,
		@Valid @ModelAttribute("stockAdjustForm") ProductStockAdjustForm form,
		BindingResult bindingResult,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("productError", "棚卸は本部・店長・副店長のみ実行できます。");
			return "redirect:/admin/products/stocks";
		}
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("productError", "調整数を入力してください。");
			return "redirect:/admin/products/inventory";
		}
		try {
			productInventoryService.adjustStock(productId, form);
			redirectAttributes.addFlashAttribute("productMessage", "棚卸結果を反映しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("productError", ex.getMessage());
		}
		return "redirect:/admin/products/inventory";
	}

	@PostMapping("/inventory/bulk-adjust")
	public String bulkAdjustStock(
		@RequestParam("productIds") List<String> productIds,
		@RequestParam("estimatedStocks") List<Integer> estimatedStocks,
		@RequestParam("actualStocks") List<Integer> actualStocks,
		Authentication authentication,
		RedirectAttributes redirectAttributes
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("productError", "棚卸は本部・店長・副店長のみ実行できます。");
			return "redirect:/admin/products/stocks";
		}
		if (productIds == null || estimatedStocks == null || actualStocks == null
			|| productIds.size() != estimatedStocks.size()
			|| productIds.size() != actualStocks.size()) {
			redirectAttributes.addFlashAttribute("productError", "棚卸データの形式が不正です。");
			return "redirect:/admin/products/inventory";
		}

		for (int i = 0; i < productIds.size(); i++) {
			String productId = productIds.get(i);
			Integer actual = actualStocks.get(i);
			if (productId == null || productId.isBlank()) {
				redirectAttributes.addFlashAttribute("productError", "商品IDが不正です。");
				return "redirect:/admin/products/inventory";
			}
			if (actual == null || actual < 0) {
				redirectAttributes.addFlashAttribute("productError", "実際の在庫数は0以上で入力してください。");
				return "redirect:/admin/products/inventory";
			}
		}

		int updatedCount = 0;
		try {
			for (int i = 0; i < productIds.size(); i++) {
				String productId = productIds.get(i);
				int estimated = estimatedStocks.get(i);
				int actual = actualStocks.get(i);
				int delta = actual - estimated;
				if (delta != 0) {
					productInventoryService.adjustStock(productId, new ProductStockAdjustForm(delta));
					updatedCount++;
				}
			}
			if (updatedCount == 0) {
				redirectAttributes.addFlashAttribute("productMessage", "差分はありませんでした。");
			} else {
				redirectAttributes.addFlashAttribute("productMessage", "棚卸を一括反映しました。更新件数: " + updatedCount + "件");
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("productError", ex.getMessage());
		}
		return "redirect:/admin/products/inventory";
	}

	@GetMapping("/orders/new")
	public String orderForm(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("productError", "発注登録は本部・店長・副店長のみ実行できます。");
			return "redirect:/admin/products/stocks";
		}
		if (!model.containsAttribute("orderForm")) {
			model.addAttribute("orderForm", buildOrderForm());
		}
		model.addAttribute("productNames", productCatalogService.findAll().stream()
			.collect(Collectors.toMap(ProductCatalogItem::id, ProductCatalogItem::name, (left, right) -> left, LinkedHashMap::new)));
		model.addAttribute("stockMap", productInventoryService.findAllStockItems().stream()
			.collect(Collectors.toMap(ProductStock::getProductId, ProductStock::getStock, (left, right) -> left, LinkedHashMap::new)));
		model.addAttribute("catalog", productCatalogService.findAll());
		model.addAttribute("adminSection", "products");
		return "admin/product-order-form";
	}

	@PostMapping("/orders/new")
	public String createOrder(
		@Valid @ModelAttribute("orderForm") ProductOrderBulkForm form,
		BindingResult bindingResult,
		Model model,
		Authentication authentication,
		RedirectAttributes redirectAttributes,
		Principal principal
	) {
		if (!canManage(authentication)) {
			redirectAttributes.addFlashAttribute("productError", "発注登録は本部・店長・副店長のみ実行できます。");
			return "redirect:/admin/products/stocks";
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("productNames", productCatalogService.findAll().stream()
				.collect(Collectors.toMap(ProductCatalogItem::id, ProductCatalogItem::name, (left, right) -> left, LinkedHashMap::new)));
			model.addAttribute("catalog", productCatalogService.findAll());
			model.addAttribute("stockMap", productInventoryService.findAllStockItems().stream()
				.collect(Collectors.toMap(ProductStock::getProductId, ProductStock::getStock, (left, right) -> left, LinkedHashMap::new)));
			model.addAttribute("adminSection", "products");
			return "admin/product-order-form";
		}
		try {
			String orderedBy = principal != null ? principal.getName() : "system";
			int createdCount = productInventoryService.createOrders(form.getItems(), orderedBy);
			redirectAttributes.addFlashAttribute("productMessage", "発注を登録しました。登録件数: " + createdCount + "件");
			return "redirect:/admin/products/stocks";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("productError", ex.getMessage());
			model.addAttribute("productNames", productCatalogService.findAll().stream()
				.collect(Collectors.toMap(ProductCatalogItem::id, ProductCatalogItem::name, (left, right) -> left, LinkedHashMap::new)));
			model.addAttribute("catalog", productCatalogService.findAll());
			model.addAttribute("stockMap", productInventoryService.findAllStockItems().stream()
				.collect(Collectors.toMap(ProductStock::getProductId, ProductStock::getStock, (left, right) -> left, LinkedHashMap::new)));
			model.addAttribute("adminSection", "products");
			return "admin/product-order-form";
		}
	}

	private void populateProductTableModel(Model model) {
		List<ProductStock> stockItems = productInventoryService.findAllStockItems();
		Map<String, ProductStock> stockMap = stockItems.stream()
			.collect(Collectors.toMap(ProductStock::getProductId, item -> item, (left, right) -> left, LinkedHashMap::new));
		List<ProductCatalogItem> catalog = productCatalogService.findAll();

		Map<String, Integer> stocks = new LinkedHashMap<>();
		Map<String, Integer> recommendedStocks = new LinkedHashMap<>();
		Map<String, String> productNames = new LinkedHashMap<>();

		catalog.forEach(item -> productNames.put(item.id(), item.name()));
		catalog.forEach(item -> {
			ProductStock stockItem = stockMap.get(item.id());
			stocks.put(item.id(), stockItem != null ? stockItem.getStock() : productInventoryService.getDefaultStock());
			recommendedStocks.put(item.id(), stockItem != null ? stockItem.getRecommendedStock() : productInventoryService.getDefaultStock());
		});
		stockMap.forEach((id, stockItem) -> {
			if (!stocks.containsKey(id)) {
				stocks.put(id, stockItem.getStock());
				recommendedStocks.put(id, stockItem.getRecommendedStock());
			}
		});

		model.addAttribute("stocks", stocks);
		model.addAttribute("recommendedStocks", recommendedStocks);
		model.addAttribute("productNames", productNames);
		model.addAttribute("catalog", catalog);
	}

	private ProductOrderBulkForm buildOrderForm() {
		List<ProductCatalogItem> catalog = productCatalogService.findAll();
		Map<String, ProductStock> stockMap = productInventoryService.findAllStockItems().stream()
			.collect(Collectors.toMap(ProductStock::getProductId, item -> item, (left, right) -> left));

		ProductOrderBulkForm form = new ProductOrderBulkForm();
		List<ProductOrderLineForm> lines = catalog.stream()
			.map(item -> {
				ProductStock stock = stockMap.get(item.id());
				int recommended = stock != null ? stock.getRecommendedStock() : productInventoryService.getDefaultStock();
				return new ProductOrderLineForm(item.id(), 0, recommended);
			})
			.toList();
		form.setItems(lines);
		return form;
	}

	private List<ProductOrderGroupView> buildRecentOrderGroups(List<ProductOrder> recentOrders, Map<String, ProductCatalogItem> catalogMap) {
		Map<String, List<ProductOrderRowView>> grouped = recentOrders.stream()
			.map(ProductOrderRowView::from)
			.collect(Collectors.groupingBy(
				row -> row.groupKey(),
				LinkedHashMap::new,
				Collectors.toList()
			));

		return grouped.values().stream()
			.map(rows -> buildOrderGroupView(rows, catalogMap))
			.sorted(Comparator.comparing(ProductOrderGroupView::createdAt).reversed().thenComparing(ProductOrderGroupView::groupKey))
			.limit(100)
			.toList();
	}

	private ProductOrderGroupView buildOrderGroupView(List<ProductOrderRowView> rows, Map<String, ProductCatalogItem> catalogMap) {
		ProductOrderRowView first = rows.get(0);
		int totalQuantity = rows.stream().mapToInt(ProductOrderRowView::quantity).sum();
		Set<String> categories = rows.stream()
			.map(row -> catalogMap.containsKey(row.productId()) ? catalogMap.get(row.productId()).category() : "")
			.map(this::toCategoryLabel)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		String categoryLabel = String.join(" / ", categories);
		return new ProductOrderGroupView(
			first.groupKey(),
			categoryLabel.isBlank() ? "-" : categoryLabel,
			totalQuantity,
			first.orderedBy(),
			first.note(),
			first.createdAt()
		);
	}

	private String toCategoryLabel(String category) {
		if (category == null || category.isBlank()) {
			return "未分類";
		}
		return switch (category.trim().toLowerCase()) {
			case "cage" -> "ケージ";
			case "food" -> "フード";
			case "toy" -> "おもちゃ";
			case "care" -> "ケア用品";
			case "starter" -> "スターターセット";
			default -> category;
		};
	}

	public static record ProductOrderRowView(
		Long id,
		String groupKey,
		String productId,
		Integer quantity,
		String orderedBy,
		String note,
		LocalDateTime createdAt
	) {
		static ProductOrderRowView from(ProductOrder order) {
			String groupKey = (order.getOrderGroupId() != null && !order.getOrderGroupId().isBlank())
				? order.getOrderGroupId()
				: buildLegacyGroupKey(order);
			return new ProductOrderRowView(
				order.getId(),
				groupKey,
				order.getProductId(),
				order.getQuantity() == null ? 0 : order.getQuantity(),
				order.getOrderedBy(),
				order.getNote() == null ? "" : order.getNote(),
				order.getCreatedAt()
			);
		}

		private static String buildLegacyGroupKey(ProductOrder order) {
			LocalDateTime secondPrecision = order.getCreatedAt() == null ? null : order.getCreatedAt().withNano(0);
			String orderedBy = order.getOrderedBy() == null ? "" : order.getOrderedBy().trim();
			String note = order.getNote() == null ? "" : order.getNote().trim();
			int hash = Objects.hash(secondPrecision, orderedBy, note);
			return "legacy-" + Integer.toUnsignedString(hash, 16);
		}
	}

	public static record ProductOrderGroupView(
		String groupKey,
		String categoryLabel,
		Integer totalQuantity,
		String orderedBy,
		String note,
		LocalDateTime createdAt
	) {}

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
}
