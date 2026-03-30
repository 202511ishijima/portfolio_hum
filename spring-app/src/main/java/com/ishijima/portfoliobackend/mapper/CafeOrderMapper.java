package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.CafeOrder;
import com.ishijima.portfoliobackend.entity.CafeOrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CafeOrderMapper {

	void insert(CafeOrder order);

	Optional<CafeOrder> findById(@Param("id") Long id);

	List<CafeOrder> findAll();

	int updateStatus(
		@Param("id") Long id,
		@Param("status") CafeOrderStatus status,
		@Param("updatedAt") LocalDateTime updatedAt,
		@Param("paidAt") LocalDateTime paidAt
	);
}

