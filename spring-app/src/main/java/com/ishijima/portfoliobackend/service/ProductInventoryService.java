package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.ProductOrder;
import com.ishijima.portfoliobackend.entity.ProductStock;
import com.ishijima.portfoliobackend.form.ProductOrderLineForm;
import com.ishijima.portfoliobackend.form.ProductStockAdjustForm;
import com.ishijima.portfoliobackend.form.ProductPurchaseItemForm;

import java.util.List;
import java.util.Map;

public interface ProductInventoryService {

	int getDefaultStock();

	List<ProductStock> findAllStockItems();

	Map<String, Integer> findAllStocks();

	Map<String, Integer> adjustStock(String productId, ProductStockAdjustForm form);

	int createOrders(List<ProductOrderLineForm> items, String orderedBy);

	List<ProductOrder> findRecentOrders();

	Map<String, Integer> purchase(List<ProductPurchaseItemForm> items);
}
