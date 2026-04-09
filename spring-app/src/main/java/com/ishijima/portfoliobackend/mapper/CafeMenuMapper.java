package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.CafeMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CafeMenuMapper {

	List<CafeMenu> findAll();

	List<CafeMenu> findAvailable();

	Optional<CafeMenu> findById(@Param("id") String id);

	int updateNamePriceAndAvailability(
		@Param("id") String id,
		@Param("name") String name,
		@Param("price") Integer price,
		@Param("available") boolean available
	);
}
