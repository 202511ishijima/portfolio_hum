package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.ProductOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductOrderMapper {

	void insert(ProductOrder order);

	List<ProductOrder> findRecent();
}
