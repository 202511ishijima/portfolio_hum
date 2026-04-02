package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.Shift;
import com.ishijima.portfoliobackend.entity.ShiftSlot;
import com.ishijima.portfoliobackend.form.ShiftForm;
import com.ishijima.portfoliobackend.mapper.ShiftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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
	public List<Shift> findByWorkDateBetween(LocalDate fromDate, LocalDate toDate) {
		return shiftMapper.findByWorkDateBetween(fromDate, toDate);
	}

	@Override
	public List<Shift> findByEmployeeId(Long employeeId) {
		return shiftMapper.findByEmployeeId(employeeId).stream()
			.sorted(Comparator
				.comparing(Shift::getWorkDate)
				.thenComparing(Shift::getStartTime))
			.toList();
	}

	@Override
	public Shift findById(Long id) {
		return shiftMapper.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Shift not found. id=" + id));
	}

	@Override
	@Transactional
	public Shift create(ShiftForm form) {
		Employee employee = employeeService.findById(form.employeeId());
		ShiftSlot slot = validateAndResolveSlot(employee, form.shiftSlot());

		Shift shift = Shift.builder()
			.employeeId(form.employeeId())
			.workDate(form.workDate())
			.shiftSlot(slot.name())
			.startTime(slot.getStartTime())
			.endTime(slot.getEndTime())
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
		Employee employee = employeeService.findById(form.employeeId());
		ShiftSlot slot = validateAndResolveSlot(employee, form.shiftSlot());

		Shift shift = findById(id);
		shift.setEmployeeId(form.employeeId());
		shift.setWorkDate(form.workDate());
		shift.setShiftSlot(slot.name());
		shift.setStartTime(slot.getStartTime());
		shift.setEndTime(slot.getEndTime());
		shift.setNote(form.note());
		shift.setUpdatedAt(LocalDateTime.now());
		shiftMapper.update(shift);
		return shift;
	}

	private ShiftSlot validateAndResolveSlot(Employee employee, String shiftSlot) {
		ShiftSlot slot = ShiftSlot.from(shiftSlot);
		if (isFullDayOnly(employee) && slot != ShiftSlot.FULL) {
			throw new IllegalArgumentException("副店長以上は終日シフトのみ登録できます。");
		}
		return slot;
	}

	private boolean isFullDayOnly(Employee employee) {
		String role = employee.getRole();
		return "ADMIN".equals(role) || "STAFF_MANAGER".equals(role);
	}
}
