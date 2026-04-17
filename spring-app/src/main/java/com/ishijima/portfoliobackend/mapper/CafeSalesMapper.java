package com.ishijima.portfoliobackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CafeSalesMapper {

	int addToDailySales(
		@Param("salesDate") LocalDate salesDate,
		@Param("orderCount") Integer orderCount,
		@Param("totalAmount") Integer totalAmount
	);

	void insertDailySales(
		@Param("salesDate") LocalDate salesDate,
		@Param("orderCount") Integer orderCount,
		@Param("totalAmount") Integer totalAmount
	);

	List<Map<String, Object>> findDailySalesRecent(@Param("limit") Integer limit);

	Map<String, Object> findSalesTarget();

	void upsertSalesTarget(
		@Param("dailyTarget") Integer dailyTarget,
		@Param("monthlyTarget") Integer monthlyTarget
	);
}
