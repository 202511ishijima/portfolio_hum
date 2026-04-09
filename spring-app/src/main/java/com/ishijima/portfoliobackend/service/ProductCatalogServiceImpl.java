package com.ishijima.portfoliobackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishijima.portfoliobackend.dto.ProductCatalogItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

	private final ObjectMapper objectMapper;

	@Override
	public List<ProductCatalogItem> findAll() {
		Path filePath = resolveProductsJsonPath();
		if (filePath == null) {
			return List.of();
		}

		try {
			JsonNode root = objectMapper.readTree(filePath.toFile());
			if (root == null || !root.isArray()) {
				return List.of();
			}
			List<ProductCatalogItem> items = new ArrayList<>();
			for (JsonNode node : root) {
				String id = node.path("id").asText("");
				String category = node.path("category").asText("");
				String name = node.path("name").asText("");
				if (!id.isBlank()) {
					items.add(new ProductCatalogItem(id, category.isBlank() ? "uncategorized" : category, name.isBlank() ? id : name));
				}
			}
			return items;
		} catch (IOException ex) {
			return List.of();
		}
	}

	private Path resolveProductsJsonPath() {
		List<Path> candidates = List.of(
			Paths.get("..", "assets", "data", "products.json"),
			Paths.get("assets", "data", "products.json")
		);
		for (Path candidate : candidates) {
			if (Files.exists(candidate)) {
				return candidate.normalize();
			}
		}
		return null;
	}
}
