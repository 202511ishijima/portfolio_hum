package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductStockMapper {

	List<ProductStock> findAll();

	Optional<ProductStock> findByProductId(@Param("productId") String productId);

	void insertDefaultIfMissing(
		@Param("productId") String productId,
		@Param("defaultStock") Integer defaultStock,
		@Param("defaultRecommendedStock") Integer defaultRecommendedStock
	);

	int decreaseStock(@Param("productId") String productId, @Param("quantity") Integer quantity);

	int increaseStock(@Param("productId") String productId, @Param("quantity") Integer quantity);

	int updateRecommendedStock(@Param("productId") String productId, @Param("recommendedStock") Integer recommendedStock);
}
