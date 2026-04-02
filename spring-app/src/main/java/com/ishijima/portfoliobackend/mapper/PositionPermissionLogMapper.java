package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.PositionPermissionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PositionPermissionLogMapper {

	void insert(PositionPermissionLog log);

	List<PositionPermissionLog> findRecent(@Param("limit") int limit);
}
