package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Shift;
import com.ishijima.portfoliobackend.form.ShiftForm;
import com.ishijima.portfoliobackend.mapper.ShiftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

	private final ShiftMapper shiftMapper;
	private final EmployeeService employeeService;

	@Override
	public List<Shift> findAll() {
		return shiftMapper.findAll();
	}

	@Override
	public List<Shift> findByEmployeeId(Long employeeId) {
		return shiftMapper.findByEmployeeId(employeeId);
	}

	@Override
	public Shift findById(Long id) {
		return shiftMapper.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Shift not found. id=" + id));
	}

	@Override
	@Transactional
	public Shift create(ShiftForm form) {
		validate(form);
		Shift shift = Shift.builder()
			.employeeId(form.employeeId())
			.workDate(form.workDate())
			.startTime(form.startTime())
			.endTime(form.endTime())
			.note(form.note())
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		shiftMapper.insert(shift);
		return shift;
	}

	@Override
	@Transactional
	public Shift update(Long id, ShiftForm form) {
		validate(form);
		Shift shift = findById(id);
		shift.setEmployeeId(form.employeeId());
		shift.setWorkDate(form.workDate());
		shift.setStartTime(form.startTime());
		shift.setEndTime(form.endTime());
		shift.setNote(form.note());
		shift.setUpdatedAt(LocalDateTime.now());
		shiftMapper.update(shift);
		return shift;
	}

	private void validate(ShiftForm form) {
		employeeService.findById(form.employeeId());
		if (form.endTime().isBefore(form.startTime()) || form.endTime().equals(form.startTime())) {
			throw new IllegalArgumentException("End time must be later than start time.");
		}
	}
}
