package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.dto.ProductCatalogItem;

import java.util.List;

public interface ProductCatalogService {

	List<ProductCatalogItem> findAll();
}
