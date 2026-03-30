package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.CafeOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CafeOrderItemMapper {

	void insert(CafeOrderItem item);

	List<CafeOrderItem> findByOrderId(@Param("orderId") Long orderId);
}

