package com.ishijima.portfoliobackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishijima.portfoliobackend.dto.ProductCatalogItem;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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
		List<ProductCatalogItem> fromClasspath = loadFromClasspath();
		if (!fromClasspath.isEmpty()) {
			return fromClasspath;
		}

		Path filePath = resolveProductsJsonPath();
		if (filePath == null) {
			return defaultCatalog();
		}

		try {
			return parseCatalog(objectMapper.readTree(filePath.toFile()));
		} catch (IOException ex) {
			return defaultCatalog();
		}
	}

	private List<ProductCatalogItem> loadFromClasspath() {
		List<String> candidates = List.of(
			"product-catalog.json",
			"static/assets/data/products.json",
			"assets/data/products.json"
		);
		for (String candidate : candidates) {
			ClassPathResource resource = new ClassPathResource(candidate);
			if (!resource.exists()) {
				continue;
			}
			try (InputStream in = resource.getInputStream()) {
				List<ProductCatalogItem> parsed = parseCatalog(objectMapper.readTree(in));
				if (!parsed.isEmpty()) {
					return parsed;
				}
			} catch (IOException ignored) {
				// Try next candidate.
			}
		}
		return List.of();
	}

	private List<ProductCatalogItem> parseCatalog(JsonNode root) {
		if (root == null || !root.isArray()) {
			return List.of();
		}
		List<ProductCatalogItem> items = new ArrayList<>();
		for (JsonNode node : root) {
			String id = node.path("id").asText("");
			String category = node.path("category").asText("");
			String name = node.path("name").asText("");
			if (!id.isBlank()) {
				items.add(new ProductCatalogItem(
					id,
					category.isBlank() ? "uncategorized" : category,
					name.isBlank() ? id : name
				));
			}
		}
		return items;
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

	private List<ProductCatalogItem> defaultCatalog() {
		return List.of(
			new ProductCatalogItem("cage-01", "cage", "ケージ 01"),
			new ProductCatalogItem("cage-02", "cage", "ケージ 02"),
			new ProductCatalogItem("cage-03", "cage", "ケージ 03"),
			new ProductCatalogItem("cage-04", "cage", "ケージ 04"),
			new ProductCatalogItem("cage-05", "cage", "ケージ 05"),
			new ProductCatalogItem("cage-06", "cage", "ケージ 06"),
			new ProductCatalogItem("food-01", "food", "フード 01"),
			new ProductCatalogItem("food-02", "food", "フード 02"),
			new ProductCatalogItem("food-03", "food", "フード 03"),
			new ProductCatalogItem("food-04", "food", "フード 04"),
			new ProductCatalogItem("food-05", "food", "フード 05"),
			new ProductCatalogItem("food-06", "food", "フード 06"),
			new ProductCatalogItem("toy-01", "toy", "おもちゃ 01"),
			new ProductCatalogItem("toy-02", "toy", "おもちゃ 02"),
			new ProductCatalogItem("toy-03", "toy", "おもちゃ 03"),
			new ProductCatalogItem("toy-04", "toy", "おもちゃ 04"),
			new ProductCatalogItem("toy-05", "toy", "おもちゃ 05"),
			new ProductCatalogItem("toy-06", "toy", "おもちゃ 06"),
			new ProductCatalogItem("care-01", "care", "ケア用品 01"),
			new ProductCatalogItem("care-02", "care", "ケア用品 02"),
			new ProductCatalogItem("care-03", "care", "ケア用品 03"),
			new ProductCatalogItem("care-04", "care", "ケア用品 04"),
			new ProductCatalogItem("care-05", "care", "ケア用品 05"),
			new ProductCatalogItem("starter-01", "starter", "スターターセット 01"),
			new ProductCatalogItem("starter-02", "starter", "スターターセット 02"),
			new ProductCatalogItem("starter-03", "starter", "スターターセット 03")
		);
	}
}
