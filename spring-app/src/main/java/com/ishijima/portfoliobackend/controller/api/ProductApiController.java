package com.ishijima.portfoliobackend.controller.api;

import com.ishijima.portfoliobackend.dto.ProductCatalogItem;
import com.ishijima.portfoliobackend.form.ProductPurchaseForm;
import com.ishijima.portfoliobackend.service.ProductCatalogService;
import com.ishijima.portfoliobackend.service.ProductInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

	private final ProductInventoryService productInventoryService;
	private final ProductCatalogService productCatalogService;

	@GetMapping("/stocks")
	public ResponseEntity<Map<String, Object>> getStocks() {
		Map<String, Integer> dbStocks = productInventoryService.findAllStockItems().stream()
			.collect(Collectors.toMap(
				item -> item.getProductId(),
				item -> item.getStock(),
				(left, right) -> left
			));
		Map<String, Integer> dbRecommendedStocks = productInventoryService.findAllStockItems().stream()
			.collect(Collectors.toMap(
				item -> item.getProductId(),
				item -> item.getRecommendedStock(),
				(left, right) -> left
			));
		int defaultStock = productInventoryService.getDefaultStock();

		Map<String, Integer> stocks = new LinkedHashMap<>();
		Map<String, Integer> recommendedStocks = new LinkedHashMap<>();
		for (ProductCatalogItem item : productCatalogService.findAll()) {
			stocks.put(item.id(), dbStocks.getOrDefault(item.id(), defaultStock));
			recommendedStocks.put(item.id(), dbRecommendedStocks.getOrDefault(item.id(), defaultStock));
		}
		dbStocks.forEach(stocks::putIfAbsent);
		dbRecommendedStocks.forEach(recommendedStocks::putIfAbsent);

		return ResponseEntity.ok(Map.of(
			"defaultStock", defaultStock,
			"stocks", stocks,
			"recommendedStocks", recommendedStocks
		));
	}

	@PostMapping("/purchase")
	public ResponseEntity<Map<String, Object>> purchase(@Valid @RequestBody ProductPurchaseForm form) {
		Map<String, Integer> purchased = productInventoryService.purchase(form.items());
		return ResponseEntity.ok(Map.of(
			"message", "購入を受け付けました。",
			"purchased", purchased,
			"stocks", productInventoryService.findAllStocks()
		));
	}
}
