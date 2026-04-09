package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.ProductOrder;
import com.ishijima.portfoliobackend.entity.ProductStock;
import com.ishijima.portfoliobackend.form.ProductOrderLineForm;
import com.ishijima.portfoliobackend.form.ProductPurchaseItemForm;
import com.ishijima.portfoliobackend.form.ProductStockAdjustForm;
import com.ishijima.portfoliobackend.mapper.ProductOrderMapper;
import com.ishijima.portfoliobackend.mapper.ProductStockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductInventoryServiceImpl implements ProductInventoryService {

	private static final int DEFAULT_STOCK = 30;

	private final ProductStockMapper productStockMapper;
	private final ProductOrderMapper productOrderMapper;

	@Override
	public int getDefaultStock() {
		return DEFAULT_STOCK;
	}

	@Override
	public List<ProductStock> findAllStockItems() {
		return productStockMapper.findAll();
	}

	@Override
	public Map<String, Integer> findAllStocks() {
		Map<String, Integer> stocks = new LinkedHashMap<>();
		productStockMapper.findAll().forEach(stock -> stocks.put(stock.getProductId(), stock.getStock()));
		return stocks;
	}

	@Override
	@Transactional
	public Map<String, Integer> adjustStock(String productId, ProductStockAdjustForm form) {
		if (productId == null || productId.isBlank()) {
			throw new IllegalArgumentException("商品IDが不正です。");
		}
		Integer delta = form.delta();
		if (delta == null || delta == 0) {
			throw new IllegalArgumentException("調整数は0以外で入力してください。");
		}

		productStockMapper.insertDefaultIfMissing(productId, DEFAULT_STOCK, DEFAULT_STOCK);
		if (delta > 0) {
			productStockMapper.increaseStock(productId, delta);
		} else {
			int updated = productStockMapper.decreaseStock(productId, Math.abs(delta));
			if (updated == 0) {
				Integer currentStock = productStockMapper.findByProductId(productId)
					.map(stock -> stock.getStock())
					.orElse(0);
				throw new IllegalArgumentException("在庫不足です: " + productId + "（現在庫 " + currentStock + "）");
			}
		}
		return findAllStocks();
	}

	@Override
	@Transactional
	public int createOrders(List<ProductOrderLineForm> items, String orderedBy) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("発注対象がありません。");
		}

		String orderGroupId = UUID.randomUUID().toString();
		int createdCount = 0;
		for (ProductOrderLineForm item : items) {
			String productId = item.getProductId();
			Integer quantity = item.getQuantity();
			Integer recommendedStock = item.getRecommendedStock();

			if (productId == null || productId.isBlank()) {
				throw new IllegalArgumentException("商品IDが不正です。");
			}
			if (recommendedStock == null || recommendedStock <= 0) {
				throw new IllegalArgumentException("推奨在庫数は1以上で入力してください。");
			}
			if (quantity == null || quantity < 0) {
				throw new IllegalArgumentException("発注数は0以上で入力してください。");
			}

			productStockMapper.insertDefaultIfMissing(productId, DEFAULT_STOCK, DEFAULT_STOCK);
			productStockMapper.updateRecommendedStock(productId, recommendedStock);

			if (quantity > 0) {
					productStockMapper.increaseStock(productId, quantity);
					ProductOrder order = ProductOrder.builder()
						.orderGroupId(orderGroupId)
						.productId(productId)
					.quantity(quantity)
					.note("一括発注")
					.orderedBy(orderedBy)
					.createdAt(LocalDateTime.now())
					.build();
				productOrderMapper.insert(order);
				createdCount++;
			}
		}
		return createdCount;
	}

	@Override
	public List<ProductOrder> findRecentOrders() {
		return productOrderMapper.findRecent(1000);
	}

	@Override
	public List<ProductOrder> findOrdersByGroupId(String orderGroupId) {
		if (orderGroupId == null || orderGroupId.isBlank()) {
			return List.of();
		}
		return productOrderMapper.findByOrderGroupId(orderGroupId);
	}

	@Override
	public ProductOrder findOrderById(Long id) {
		if (id == null) {
			return null;
		}
		return productOrderMapper.findById(id);
	}

	@Override
	@Transactional
	public Map<String, Integer> purchase(List<ProductPurchaseItemForm> items) {
		Map<String, Integer> purchased = new LinkedHashMap<>();
		for (ProductPurchaseItemForm item : items) {
			String productId = item.productId();
			Integer quantity = item.quantity();

			if (productId == null || productId.isBlank()) {
				throw new IllegalArgumentException("商品IDが不正です。");
			}
			if (quantity == null || quantity <= 0) {
				throw new IllegalArgumentException("購入数量が不正です。");
			}

			productStockMapper.insertDefaultIfMissing(productId, DEFAULT_STOCK, DEFAULT_STOCK);
			int updated = productStockMapper.decreaseStock(productId, quantity);
			if (updated == 0) {
				Integer currentStock = productStockMapper.findByProductId(productId)
					.map(stock -> stock.getStock())
					.orElse(0);
				throw new IllegalArgumentException("在庫不足です: " + productId + "（現在庫 " + currentStock + "）");
			}

			purchased.merge(productId, quantity, Integer::sum);
		}
		return purchased;
	}
}
