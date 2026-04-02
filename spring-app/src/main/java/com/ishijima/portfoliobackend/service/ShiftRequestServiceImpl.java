package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.Shift;
import com.ishijima.portfoliobackend.entity.ShiftRequestDay;
import com.ishijima.portfoliobackend.entity.ShiftRequestSetting;
import com.ishijima.portfoliobackend.entity.ShiftRequestSlot;
import com.ishijima.portfoliobackend.entity.ShiftSlot;
import com.ishijima.portfoliobackend.form.ShiftForm;
import com.ishijima.portfoliobackend.mapper.ShiftRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftRequestServiceImpl implements ShiftRequestService {

	private final ShiftRequestMapper shiftRequestMapper;
	private final EmployeeService employeeService;
	private final ShiftService shiftService;

	@Override
	public int findWeeklyDays(Long employeeId, int targetYear, int targetMonth) {
		return shiftRequestMapper.findSetting(employeeId, targetYear, targetMonth)
			.map(ShiftRequestSetting::getWeeklyDays)
			.orElse(3);
	}

	@Override
	public Map<LocalDate, String> findRequestMap(Long employeeId, int targetYear, int targetMonth) {
		Map<LocalDate, String> map = new LinkedHashMap<>();
		for (ShiftRequestDay day : shiftRequestMapper.findDaysByMonth(employeeId, targetYear, targetMonth)) {
			map.put(day.getRequestDate(), day.getRequestSlot());
		}
		return map;
	}

	@Override
	@Transactional
	public void saveMonthlyRequests(Long employeeId, int targetYear, int targetMonth, int weeklyDays, Map<LocalDate, String> requestMap) {
		employeeService.findById(employeeId);
		YearMonth ym = YearMonth.of(targetYear, targetMonth);
		int safeWeeklyDays = Math.max(1, Math.min(7, weeklyDays));

		ShiftRequestSetting setting = ShiftRequestSetting.builder()
			.employeeId(employeeId)
			.targetYear(targetYear)
			.targetMonth(targetMonth)
			.weeklyDays(safeWeeklyDays)
			.updatedAt(LocalDateTime.now())
			.build();
		shiftRequestMapper.upsertSetting(setting);
		shiftRequestMapper.deleteDaysByMonth(employeeId, targetYear, targetMonth);

		for (Map.Entry<LocalDate, String> entry : requestMap.entrySet()) {
			LocalDate day = entry.getKey();
			if (day == null || !YearMonth.from(day).equals(ym)) {
				continue;
			}
			ShiftRequestSlot slot = ShiftRequestSlot.from(entry.getValue());
			if (slot == ShiftRequestSlot.NONE) {
				continue;
			}
			shiftRequestMapper.insertDay(ShiftRequestDay.builder()
				.employeeId(employeeId)
				.requestDate(day)
				.requestSlot(slot.name())
				.targetYear(targetYear)
				.targetMonth(targetMonth)
				.updatedAt(LocalDateTime.now())
				.build());
		}
	}

	@Override
	@Transactional
	public int autoGenerateShifts(int targetYear, int targetMonth) {
		YearMonth ym = YearMonth.of(targetYear, targetMonth);
		LocalDate from = ym.atDay(1);
		LocalDate to = ym.atEndOfMonth();

		List<Shift> existing = shiftService.findByWorkDateBetween(from, to);
		Set<String> existingByEmployeeDate = existing.stream()
			.map(shift -> shift.getEmployeeId() + ":" + shift.getWorkDate())
			.collect(Collectors.toSet());

		Map<LocalDate, Integer> earlyCountByDate = new HashMap<>();
		Map<LocalDate, Integer> lateCountByDate = new HashMap<>();
		for (Shift shift : existing) {
			ShiftSlot slot = shift.getShiftSlotEnum();
			if (slot == ShiftSlot.EARLY) {
				earlyCountByDate.merge(shift.getWorkDate(), 1, Integer::sum);
			}
			if (slot == ShiftSlot.LATE) {
				lateCountByDate.merge(shift.getWorkDate(), 1, Integer::sum);
			}
		}

		Map<Long, Integer> weeklyDaysMap = shiftRequestMapper.findAllSettingsByMonth(targetYear, targetMonth).stream()
			.collect(Collectors.toMap(ShiftRequestSetting::getEmployeeId, ShiftRequestSetting::getWeeklyDays, (a, b) -> b));

		List<Employee> activeEmployees = employeeService.findAll().stream()
			.filter(employee -> Boolean.TRUE.equals(employee.getActive()))
			.toList();
		Map<Long, Employee> employees = activeEmployees.stream()
			.collect(Collectors.toMap(Employee::getId, e -> e));

		Map<Long, List<ShiftRequestDay>> byEmployee = shiftRequestMapper.findAllDaysByMonth(targetYear, targetMonth).stream()
			.collect(Collectors.groupingBy(ShiftRequestDay::getEmployeeId));

		Map<Integer, List<LocalDate>> monthDatesByWeek = from.datesUntil(to.plusDays(1))
			.collect(Collectors.groupingBy(
				date -> date.get(ChronoField.ALIGNED_WEEK_OF_MONTH),
				LinkedHashMap::new,
				Collectors.toList()
			));

		// 希望未提出の従業員も自動作成の対象に含める（週3日をデフォルト）
		for (Employee employee : activeEmployees) {
			byEmployee.putIfAbsent(employee.getId(), new ArrayList<>());
			weeklyDaysMap.putIfAbsent(employee.getId(), 3);
		}

		int created = 0;
		for (Map.Entry<Long, List<ShiftRequestDay>> entry : byEmployee.entrySet()) {
			Long employeeId = entry.getKey();
			Employee employee = employees.get(employeeId);
			if (employee == null || !Boolean.TRUE.equals(employee.getActive())) {
				continue;
			}

			int weeklyLimit = Math.max(1, Math.min(7, weeklyDaysMap.getOrDefault(employeeId, 3)));
			Map<Integer, List<ShiftRequestDay>> byWeek = entry.getValue().stream()
				.sorted(Comparator.comparing(ShiftRequestDay::getRequestDate))
				.collect(Collectors.groupingBy(day -> day.getRequestDate().get(ChronoField.ALIGNED_WEEK_OF_MONTH)));

			for (Map.Entry<Integer, List<LocalDate>> weekEntry : monthDatesByWeek.entrySet()) {
				int week = weekEntry.getKey();
				List<ShiftRequestDay> weekRequestsRaw = byWeek.getOrDefault(week, Collections.emptyList());
				Set<LocalDate> offDates = weekRequestsRaw.stream()
					.filter(day -> ShiftRequestSlot.from(day.getRequestSlot()) == ShiftRequestSlot.OFF)
					.map(ShiftRequestDay::getRequestDate)
					.collect(Collectors.toSet());

				List<ShiftRequestDay> weekRequests = weekRequestsRaw.stream()
					.filter(day -> {
						ShiftRequestSlot slot = ShiftRequestSlot.from(day.getRequestSlot());
						return slot != ShiftRequestSlot.OFF && slot != ShiftRequestSlot.NONE;
					})
					.toList();

				List<ShiftRequestDay> selected = new ArrayList<>(weekRequests.stream()
					.sorted(Comparator.comparing(ShiftRequestDay::getRequestDate))
					.limit(weeklyLimit)
					.toList());

				if (selected.size() < weeklyLimit) {
					Set<LocalDate> usedDates = selected.stream().map(ShiftRequestDay::getRequestDate).collect(Collectors.toSet());
					for (LocalDate candidateDate : weekEntry.getValue()) {
						if (selected.size() >= weeklyLimit) {
							break;
						}
						if (usedDates.contains(candidateDate)) {
							continue;
						}
						if (offDates.contains(candidateDate)) {
							continue;
						}
						if (candidateDate.getDayOfWeek().getValue() >= 6) {
							continue;
						}
						selected.add(ShiftRequestDay.builder()
							.employeeId(employeeId)
							.requestDate(candidateDate)
							.requestSlot(ShiftRequestSlot.FLEX.name())
							.targetYear(targetYear)
							.targetMonth(targetMonth)
							.build());
						usedDates.add(candidateDate);
					}
				}

				for (ShiftRequestDay request : selected) {
					String key = employeeId + ":" + request.getRequestDate();
					if (existingByEmployeeDate.contains(key)) {
						continue;
					}
					ShiftSlot slot = resolveSlot(employee, request, earlyCountByDate, lateCountByDate);
					if (slot == null) {
						continue;
					}
					shiftService.create(new ShiftForm(
						employeeId,
						request.getRequestDate(),
						slot.name(),
						"希望シフトから自動作成"
					));
					existingByEmployeeDate.add(key);
					if (slot == ShiftSlot.EARLY) {
						earlyCountByDate.merge(request.getRequestDate(), 1, Integer::sum);
					}
					if (slot == ShiftSlot.LATE) {
						lateCountByDate.merge(request.getRequestDate(), 1, Integer::sum);
					}
					created++;
				}
			}
		}
		return created;
	}

	@Override
	public List<ShiftRequestDay> findAllByMonth(int targetYear, int targetMonth) {
		return shiftRequestMapper.findAllDaysByMonth(targetYear, targetMonth);
	}

	private ShiftSlot resolveSlot(
		Employee employee,
		ShiftRequestDay request,
		Map<LocalDate, Integer> earlyCountByDate,
		Map<LocalDate, Integer> lateCountByDate
	) {
		boolean fullOnly = isFullDayOnly(employee);
		ShiftRequestSlot requested = ShiftRequestSlot.from(request.getRequestSlot());
		if (requested == ShiftRequestSlot.OFF || requested == ShiftRequestSlot.NONE) {
			return null;
		}
		if (fullOnly) {
			return ShiftSlot.FULL;
		}
		return switch (requested) {
			case EARLY -> ShiftSlot.EARLY;
			case LATE -> ShiftSlot.LATE;
			case FULL -> ShiftSlot.FULL;
			case FLEX -> chooseBalancedSlot(request.getRequestDate(), earlyCountByDate, lateCountByDate);
			case NONE, OFF -> null;
		};
	}

	private ShiftSlot chooseBalancedSlot(LocalDate date, Map<LocalDate, Integer> earlyCountByDate, Map<LocalDate, Integer> lateCountByDate) {
		int early = earlyCountByDate.getOrDefault(date, 0);
		int late = lateCountByDate.getOrDefault(date, 0);
		return early <= late ? ShiftSlot.EARLY : ShiftSlot.LATE;
	}

	private boolean isFullDayOnly(Employee employee) {
		String role = employee.getRole();
		return "ADMIN".equals(role) || "STAFF_MANAGER".equals(role);
	}
}
