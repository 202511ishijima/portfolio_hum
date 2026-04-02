package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Shift;
import com.ishijima.portfoliobackend.form.ShiftForm;

import java.time.LocalDate;
import java.util.List;

public interface ShiftService {

	List<Shift> findAll();

	List<Shift> findByWorkDateBetween(LocalDate fromDate, LocalDate toDate);

	List<Shift> findByEmployeeId(Long employeeId);

	Shift findById(Long id);

	Shift create(ShiftForm form);

	Shift update(Long id, ShiftForm form);
}
