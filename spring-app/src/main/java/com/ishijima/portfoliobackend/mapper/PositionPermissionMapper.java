package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.PositionPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PositionPermissionMapper {

	List<PositionPermission> findAll();

	Optional<PositionPermission> findByPosition(@Param("position") String position);

	void upsert(PositionPermission permission);
}
