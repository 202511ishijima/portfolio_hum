package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.Shift;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ShiftMapper {

	List<Shift> findAll();

	List<Shift> findByWorkDateBetween(@Param("fromDate") java.time.LocalDate fromDate, @Param("toDate") java.time.LocalDate toDate);

	List<Shift> findByEmployeeId(@Param("employeeId") Long employeeId);

	Optional<Shift> findById(@Param("id") Long id);

	void insert(Shift shift);

	void update(Shift shift);
}
