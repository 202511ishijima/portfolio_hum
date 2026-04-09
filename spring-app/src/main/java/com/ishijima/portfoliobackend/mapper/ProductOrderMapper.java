package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.ProductOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductOrderMapper {

	void insert(ProductOrder order);

	List<ProductOrder> findRecent(@Param("limit") int limit);

	List<ProductOrder> findByOrderGroupId(@Param("orderGroupId") String orderGroupId);

	ProductOrder findById(@Param("id") Long id);
}
