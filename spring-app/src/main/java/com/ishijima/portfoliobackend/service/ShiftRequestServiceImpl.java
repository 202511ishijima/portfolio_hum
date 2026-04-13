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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftRequestServiceImpl implements ShiftRequestService {

	private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.JAPAN);

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
		Map<String, Shift> existingShiftByEmployeeDate = existing.stream()
			.collect(Collectors.toMap(
				shift -> shift.getEmployeeId() + ":" + shift.getWorkDate(),
				shift -> shift,
				(left, right) -> left
			));

		Map<LocalDate, Integer> earlyCountByDate = new HashMap<>();
		Map<LocalDate, Integer> lateCountByDate = new HashMap<>();
		Map<LocalDate, Integer> totalCountByDate = new HashMap<>();
		Map<DayOfWeek, Integer> totalCountByWeekday = new HashMap<>();
		for (Shift shift : existing) {
			totalCountByDate.merge(shift.getWorkDate(), 1, Integer::sum);
			totalCountByWeekday.merge(shift.getWorkDate().getDayOfWeek(), 1, Integer::sum);
			if (shift.getShiftSlotEnum() == ShiftSlot.EARLY) {
				earlyCountByDate.merge(shift.getWorkDate(), 1, Integer::sum);
			}
			if (shift.getShiftSlotEnum() == ShiftSlot.LATE) {
				lateCountByDate.merge(shift.getWorkDate(), 1, Integer::sum);
			}
		}

		Map<Long, Integer> weeklyDaysMap = shiftRequestMapper.findAllSettingsByMonth(targetYear, targetMonth).stream()
			.collect(Collectors.toMap(ShiftRequestSetting::getEmployeeId, ShiftRequestSetting::getWeeklyDays, (a, b) -> b));

		List<Employee> activeEmployees = employeeService.findAll().stream()
			.filter(employee -> Boolean.TRUE.equals(employee.getActive()))
			.filter(employee -> !isCustomerEmployee(employee))
			.toList();
		Map<Long, Employee> employees = activeEmployees.stream()
			.collect(Collectors.toMap(Employee::getId, e -> e));

		Map<Long, List<ShiftRequestDay>> byEmployee = shiftRequestMapper.findAllDaysByMonth(targetYear, targetMonth).stream()
			.collect(Collectors.groupingBy(ShiftRequestDay::getEmployeeId));

		Map<WeekKey, List<LocalDate>> monthDatesByWeek = from.datesUntil(to.plusDays(1))
			.collect(Collectors.groupingBy(this::toWeekKey, LinkedHashMap::new, Collectors.toList()));

		for (Employee employee : activeEmployees) {
			byEmployee.putIfAbsent(employee.getId(), new ArrayList<>());
			weeklyDaysMap.putIfAbsent(employee.getId(), 3);
		}

		Map<Long, Map<WeekKey, Integer>> existingCountByEmployeeWeek = new HashMap<>();
		for (Shift shift : existing) {
			existingCountByEmployeeWeek
				.computeIfAbsent(shift.getEmployeeId(), k -> new HashMap<>())
				.merge(toWeekKey(shift.getWorkDate()), 1, Integer::sum);
		}

		int changed = 0;
		for (Map.Entry<Long, List<ShiftRequestDay>> entry : byEmployee.entrySet()) {
			Long employeeId = entry.getKey();
			Employee employee = employees.get(employeeId);
			if (employee == null || !Boolean.TRUE.equals(employee.getActive())) {
				continue;
			}

			int weeklyLimit = Math.max(1, Math.min(7, weeklyDaysMap.getOrDefault(employeeId, 3)));
			Map<WeekKey, List<ShiftRequestDay>> byWeek = entry.getValue().stream()
				.sorted(Comparator.comparing(ShiftRequestDay::getRequestDate))
				.collect(Collectors.groupingBy(day -> toWeekKey(day.getRequestDate())));

			for (Map.Entry<WeekKey, List<LocalDate>> weekEntry : monthDatesByWeek.entrySet()) {
				WeekKey week = weekEntry.getKey();
				int existingInWeek = existingCountByEmployeeWeek
					.getOrDefault(employeeId, Collections.emptyMap())
					.getOrDefault(week, 0);
				int remainingLimit = weeklyLimit - existingInWeek;
				if (remainingLimit <= 0) {
					continue;
				}

				List<ShiftRequestDay> weekRequests = byWeek.getOrDefault(week, Collections.emptyList()).stream()
					.filter(day -> {
						ShiftRequestSlot slot = ShiftRequestSlot.from(day.getRequestSlot());
						return slot != ShiftRequestSlot.OFF && slot != ShiftRequestSlot.NONE;
					})
					.sorted(buildBalancedRequestComparator(totalCountByDate, totalCountByWeekday))
					.limit(remainingLimit)
					.toList();

				for (ShiftRequestDay request : weekRequests) {
					String key = employeeId + ":" + request.getRequestDate();
					ShiftSlot slot = resolveSlot(request, earlyCountByDate, lateCountByDate);
					if (slot == null) {
						continue;
					}

					if (existingByEmployeeDate.contains(key)) {
						Shift existingShift = existingShiftByEmployeeDate.get(key);
						if (existingShift != null && existingShift.getShiftSlotEnum() != slot) {
							shiftService.update(existingShift.getId(), new ShiftForm(
								existingShift.getEmployeeId(),
								existingShift.getWorkDate(),
								slot.name(),
								existingShift.getNote()
							));
							adjustEarlyLateCounts(existingShift.getWorkDate(), existingShift.getShiftSlotEnum(), slot, earlyCountByDate, lateCountByDate);
							existingShift.setShiftSlot(slot.name());
							changed++;
						}
						continue;
					}

					shiftService.create(new ShiftForm(
						employeeId,
						request.getRequestDate(),
						slot.name(),
						"希望シフトから自動作成"
					));
					existingByEmployeeDate.add(key);
					existingShiftByEmployeeDate.put(key, Shift.builder()
						.employeeId(employeeId)
						.workDate(request.getRequestDate())
						.shiftSlot(slot.name())
						.note("希望シフトから自動作成")
						.build());
					totalCountByDate.merge(request.getRequestDate(), 1, Integer::sum);
					totalCountByWeekday.merge(request.getRequestDate().getDayOfWeek(), 1, Integer::sum);
					if (slot == ShiftSlot.EARLY) {
						earlyCountByDate.merge(request.getRequestDate(), 1, Integer::sum);
					}
					if (slot == ShiftSlot.LATE) {
						lateCountByDate.merge(request.getRequestDate(), 1, Integer::sum);
					}
					existingCountByEmployeeWeek
						.computeIfAbsent(employeeId, k -> new HashMap<>())
						.merge(week, 1, Integer::sum);
					changed++;
				}
			}
		}
		return changed;
	}

	@Override
	public List<ShiftRequestDay> findAllByMonth(int targetYear, int targetMonth) {
		return shiftRequestMapper.findAllDaysByMonth(targetYear, targetMonth);
	}

	private ShiftSlot resolveSlot(
		ShiftRequestDay request,
		Map<LocalDate, Integer> earlyCountByDate,
		Map<LocalDate, Integer> lateCountByDate
	) {
		ShiftRequestSlot requested = ShiftRequestSlot.from(request.getRequestSlot());
		if (requested == ShiftRequestSlot.OFF || requested == ShiftRequestSlot.NONE) {
			return null;
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

	private Comparator<ShiftRequestDay> buildBalancedRequestComparator(
		Map<LocalDate, Integer> totalCountByDate,
		Map<DayOfWeek, Integer> totalCountByWeekday
	) {
		return Comparator
			.comparingInt((ShiftRequestDay request) -> totalCountByDate.getOrDefault(request.getRequestDate(), 0))
			.thenComparingInt(request -> totalCountByWeekday.getOrDefault(request.getRequestDate().getDayOfWeek(), 0))
			.thenComparingInt(this::requestPriority)
			.thenComparing(ShiftRequestDay::getRequestDate);
	}

	private int requestPriority(ShiftRequestDay request) {
		ShiftRequestSlot slot = ShiftRequestSlot.from(request.getRequestSlot());
		return slot == ShiftRequestSlot.FLEX ? 1 : 0;
	}

	private void adjustEarlyLateCounts(
		LocalDate date,
		ShiftSlot oldSlot,
		ShiftSlot newSlot,
		Map<LocalDate, Integer> earlyCountByDate,
		Map<LocalDate, Integer> lateCountByDate
	) {
		if (oldSlot == ShiftSlot.EARLY) {
			earlyCountByDate.compute(date, (d, count) -> count == null || count <= 1 ? 0 : count - 1);
		}
		if (oldSlot == ShiftSlot.LATE) {
			lateCountByDate.compute(date, (d, count) -> count == null || count <= 1 ? 0 : count - 1);
		}
		if (newSlot == ShiftSlot.EARLY) {
			earlyCountByDate.merge(date, 1, Integer::sum);
		}
		if (newSlot == ShiftSlot.LATE) {
			lateCountByDate.merge(date, 1, Integer::sum);
		}
	}

	private boolean isCustomerEmployee(Employee employee) {
		String role = employee.getRole() == null ? "" : employee.getRole().trim();
		String position = employee.getPosition() == null ? "" : employee.getPosition().trim();
		return "CUSTOMER".equalsIgnoreCase(role) || "顧客".equals(position);
	}

	private WeekKey toWeekKey(LocalDate date) {
		return new WeekKey(
			date.get(WEEK_FIELDS.weekBasedYear()),
			date.get(WEEK_FIELDS.weekOfWeekBasedYear())
		);
	}

	private record WeekKey(int weekBasedYear, int weekOfYear) {
	}
}
