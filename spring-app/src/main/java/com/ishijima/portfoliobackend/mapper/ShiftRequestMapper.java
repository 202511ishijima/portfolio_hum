package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.ShiftRequestDay;
import com.ishijima.portfoliobackend.entity.ShiftRequestSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ShiftRequestMapper {

	Optional<ShiftRequestSetting> findSetting(
		@Param("employeeId") Long employeeId,
		@Param("targetYear") Integer targetYear,
		@Param("targetMonth") Integer targetMonth
	);

	void upsertSetting(ShiftRequestSetting setting);

	List<ShiftRequestDay> findDaysByMonth(
		@Param("employeeId") Long employeeId,
		@Param("targetYear") Integer targetYear,
		@Param("targetMonth") Integer targetMonth
	);

	List<ShiftRequestDay> findAllDaysByMonth(
		@Param("targetYear") Integer targetYear,
		@Param("targetMonth") Integer targetMonth
	);

	List<ShiftRequestSetting> findAllSettingsByMonth(
		@Param("targetYear") Integer targetYear,
		@Param("targetMonth") Integer targetMonth
	);

	void deleteDaysByMonth(
		@Param("employeeId") Long employeeId,
		@Param("targetYear") Integer targetYear,
		@Param("targetMonth") Integer targetMonth
	);

	void insertDay(ShiftRequestDay day);
}
