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
			return List.of();
		}

		try {
			return parseCatalog(objectMapper.readTree(filePath.toFile()));
		} catch (IOException ex) {
			return List.of();
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
}
