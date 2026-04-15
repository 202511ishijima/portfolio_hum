package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.Shift;
import com.ishijima.portfoliobackend.entity.ShiftRequestSlot;
import com.ishijima.portfoliobackend.entity.ShiftSlot;
import com.ishijima.portfoliobackend.form.ShiftForm;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.ShiftRequestService;
import com.ishijima.portfoliobackend.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/shifts")
@RequiredArgsConstructor
public class AdminShiftController {

	private final ShiftService shiftService;
	private final EmployeeService employeeService;
	private final ShiftRequestService shiftRequestService;

	@GetMapping
	public String list(
		@RequestParam(required = false) Integer year,
		@RequestParam(required = false) Integer month,
		Model model
	) {
		YearMonth currentMonth = resolveYearMonth(year, month);
		LocalDate firstDay = currentMonth.atDay(1);
		LocalDate lastDay = currentMonth.atEndOfMonth();
		LocalDate today = LocalDate.now();

		List<Employee> employees = employeeService.findAll().stream()
			.filter(employee -> Boolean.TRUE.equals(employee.getActive()))
			.filter(employee -> !isCustomerEmployee(employee))
			.sorted(Comparator.comparing(Employee::getName))
			.toList();
		Set<Long> employeeIds = employees.stream()
			.map(Employee::getId)
			.collect(Collectors.toSet());

		List<Shift> monthShifts = shiftService.findByWorkDateBetween(firstDay, lastDay).stream()
			.filter(shift -> employeeIds.contains(shift.getEmployeeId()))
			.toList();
		Map<Long, Long> shiftCountByEmployee = monthShifts.stream()
			.collect(Collectors.groupingBy(Shift::getEmployeeId, Collectors.counting()));
		List<Shift> todayShifts = monthShifts.stream()
			.filter(shift -> today.equals(shift.getWorkDate()))
			.sorted(Comparator.comparing(Shift::getStartTime).thenComparing(Shift::getEmployeeName))
			.toList();

		List<LocalDate> monthDays = firstDay.datesUntil(lastDay.plusDays(1)).toList();
		Map<Long, Map<LocalDate, List<Shift>>> shiftMatrix = createMatrix(employees, monthDays);
		for (Shift shift : monthShifts) {
			Map<LocalDate, List<Shift>> employeeRow = shiftMatrix.get(shift.getEmployeeId());
			if (employeeRow != null) {
				employeeRow.computeIfAbsent(shift.getWorkDate(), key -> new ArrayList<>()).add(shift);
			}
		}
		shiftMatrix.values().forEach(dayMap ->
			dayMap.values().forEach(list ->
				list.sort(Comparator.comparing(Shift::getStartTime).thenComparing(Shift::getId))
			)
		);
		List<ShiftEmployeeRow> shiftRows = employees.stream()
			.map(employee -> {
				Map<LocalDate, List<Shift>> rowMap = shiftMatrix.getOrDefault(employee.getId(), Map.of());
				List<ShiftDayCell> dayCells = monthDays.stream()
					.map(day -> new ShiftDayCell(day, rowMap.getOrDefault(day, List.of())))
					.toList();
				long shiftCount = shiftCountByEmployee.getOrDefault(employee.getId(), 0L);
				return new ShiftEmployeeRow(employee, shiftCount, dayCells);
			})
			.toList();

		YearMonth prevMonth = currentMonth.minusMonths(1);
		YearMonth nextMonth = currentMonth.plusMonths(1);

		model.addAttribute("employees", employees);
		model.addAttribute("monthDays", monthDays);
		model.addAttribute("shiftRows", shiftRows);
		model.addAttribute("shiftCountByEmployee", shiftCountByEmployee);
		model.addAttribute("monthShiftCount", monthShifts.size());
		model.addAttribute("todayShifts", todayShifts);
		model.addAttribute("today", today);
		model.addAttribute("targetYear", currentMonth.getYear());
		model.addAttribute("targetMonth", currentMonth.getMonthValue());
		model.addAttribute("monthLabel", currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")));
		model.addAttribute("prevYear", prevMonth.getYear());
		model.addAttribute("prevMonth", prevMonth.getMonthValue());
		model.addAttribute("nextYear", nextMonth.getYear());
		model.addAttribute("nextMonth", nextMonth.getMonthValue());
		model.addAttribute("holidayMap", buildHolidayMap(monthDays));
		model.addAttribute("holidayNameMap", buildHolidayNameMap(monthDays));
		model.addAttribute("adminSection", "shifts");
		return "admin/shifts";
	}

	@GetMapping("/requests")
	public String requestForm(
		@RequestParam(required = false) Integer year,
		@RequestParam(required = false) Integer month,
		@RequestParam(required = false) Long employeeId,
		Model model
	) {
		YearMonth currentMonth = resolveYearMonth(year, month);
		LocalDate firstDay = currentMonth.atDay(1);
		LocalDate lastDay = currentMonth.atEndOfMonth();

		List<Employee> employees = employeeService.findAll().stream()
			.filter(employee -> Boolean.TRUE.equals(employee.getActive()))
			.filter(employee -> !isCustomerEmployee(employee))
			.sorted(Comparator.comparing(Employee::getName))
			.toList();

		if (employees.isEmpty()) {
			model.addAttribute("employees", employees);
			model.addAttribute("adminSection", "shifts");
			model.addAttribute("monthLabel", currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")));
			model.addAttribute("targetYear", currentMonth.getYear());
			model.addAttribute("targetMonth", currentMonth.getMonthValue());
			return "admin/shift-requests";
		}

		Long selectedEmployeeId = employeeId != null ? employeeId : employees.get(0).getId();
		List<LocalDate> monthDays = firstDay.datesUntil(lastDay.plusDays(1)).toList();
		Map<LocalDate, String> requestMap = shiftRequestService.findRequestMap(
			selectedEmployeeId,
			currentMonth.getYear(),
			currentMonth.getMonthValue()
		);

		model.addAttribute("employees", employees);
		model.addAttribute("selectedEmployeeId", selectedEmployeeId);
		model.addAttribute("monthDays", monthDays);
		model.addAttribute("requestMap", requestMap);
		model.addAttribute("weeklyDays", shiftRequestService.findWeeklyDays(
			selectedEmployeeId,
			currentMonth.getYear(),
			currentMonth.getMonthValue()
		));
		model.addAttribute("holidayMap", buildHolidayMap(monthDays));
		model.addAttribute("holidayNameMap", buildHolidayNameMap(monthDays));
		model.addAttribute("leadingBlankCount", firstDay.getDayOfWeek().getValue() - 1);
		model.addAttribute("trailingBlankCount", (7 - lastDay.getDayOfWeek().getValue()) % 7);
		model.addAttribute("targetYear", currentMonth.getYear());
		model.addAttribute("targetMonth", currentMonth.getMonthValue());
		model.addAttribute("monthLabel", currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")));
		model.addAttribute("adminSection", "shifts");
		return "admin/shift-requests";
	}

	@GetMapping("/requests/save")
	public String saveRequestsGetFallback(
		@RequestParam(required = false) Integer year,
		@RequestParam(required = false) Integer month,
		@RequestParam(required = false) Long employeeId
	) {
		YearMonth currentMonth = resolveYearMonth(year, month);
		String redirect = "redirect:/admin/shifts/requests?year=" + currentMonth.getYear() + "&month=" + currentMonth.getMonthValue();
		if (employeeId != null) {
			redirect += "&employeeId=" + employeeId;
		}
		return redirect;
	}

	@PostMapping("/requests/save")
	public String saveRequests(
		@RequestParam Integer year,
		@RequestParam Integer month,
		@RequestParam Long employeeId,
		@RequestParam Integer weeklyDays,
		@RequestParam Map<String, String> allParams,
		RedirectAttributes redirectAttributes
	) {
		YearMonth currentMonth = resolveYearMonth(year, month);
		Map<LocalDate, String> requestMap = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : allParams.entrySet()) {
			if (!entry.getKey().startsWith("day_")) {
				continue;
			}
			String rawDate = entry.getKey().substring(4).replace('/', '-');
			LocalDate date;
			try {
				date = LocalDate.parse(rawDate);
			} catch (Exception ex) {
				continue;
			}
			if (YearMonth.from(date).equals(currentMonth)) {
				requestMap.put(date, entry.getValue());
			}
		}
		shiftRequestService.saveMonthlyRequests(employeeId, currentMonth.getYear(), currentMonth.getMonthValue(), weeklyDays, requestMap);
		redirectAttributes.addFlashAttribute("shiftMessage", "シフト希望を保存しました。");
		return "redirect:/admin/shifts/requests?year=" + currentMonth.getYear() + "&month=" + currentMonth.getMonthValue() + "&employeeId=" + employeeId;
	}

	// legacy handler kept temporarily for reference
	public String autoGenerateLegacy(
		@RequestParam Integer year,
		@RequestParam Integer month,
		RedirectAttributes redirectAttributes
	) {
		YearMonth currentMonth = resolveYearMonth(year, month);
		int count = 0;
		try {
			try {
				int safeCount = shiftRequestService.autoGenerateShifts(currentMonth.getYear(), currentMonth.getMonthValue());
				redirectAttributes.addFlashAttribute("shiftMessage", "自動作成が完了しました。件数: " + safeCount);
			} catch (Throwable ex) {
				redirectAttributes.addFlashAttribute("shiftError", "自動作成でエラーが発生しました: " + ex.getClass().getSimpleName() + " / " + ex.getMessage());
			}
			return "redirect:/admin/shifts?year=" + currentMonth.getYear() + "&month=" + currentMonth.getMonthValue();
		} catch (Throwable ignore) {
			// fall through
		}
		redirectAttributes.addFlashAttribute("shiftMessage", "希望から " + count + " 件のシフトを自動作成しました。");
		return "redirect:/admin/shifts?year=" + currentMonth.getYear() + "&month=" + currentMonth.getMonthValue();
	}

	@PostMapping("/auto-generate")
	public String autoGenerate(
		@RequestParam Integer year,
		@RequestParam Integer month,
		RedirectAttributes redirectAttributes
	) {
		YearMonth currentMonth = resolveYearMonth(year, month);
		try {
			int count = shiftRequestService.autoGenerateShifts(currentMonth.getYear(), currentMonth.getMonthValue());
			redirectAttributes.addFlashAttribute("shiftMessage", "希望から " + count + " 件のシフトを自動作成しました。");
		} catch (Throwable ex) {
			redirectAttributes.addFlashAttribute(
				"shiftError",
				"自動作成でエラーが発生しました: " + ex.getClass().getSimpleName() + " / " + ex.getMessage()
			);
		}
		return "redirect:/admin/shifts?year=" + currentMonth.getYear() + "&month=" + currentMonth.getMonthValue();
	}

	@GetMapping("/create-disabled")
	public String newForm(Model model) {
		if (!model.containsAttribute("shiftForm")) {
			model.addAttribute("shiftForm", new ShiftForm(null, LocalDate.now(), ShiftSlot.FULL.name(), ""));
		}
		prepareShiftFormModel(model, "create");
		model.addAttribute("adminSection", "shifts");
		return "admin/shift-form";
	}

	@PostMapping("/create-disabled")
	public String create(
		@Valid @ModelAttribute("shiftForm") ShiftForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			prepareShiftFormModel(model, "create");
			model.addAttribute("adminSection", "shifts");
			return "admin/shift-form";
		}
		try {
			shiftService.create(form);
			redirectAttributes.addFlashAttribute("shiftMessage", "シフトを登録しました。");
			return "redirect:/admin/shifts";
		} catch (IllegalArgumentException ex) {
			prepareShiftFormModel(model, "create");
			model.addAttribute("adminSection", "shifts");
			model.addAttribute("shiftError", ex.getMessage());
			return "admin/shift-form";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Shift shift = shiftService.findById(id);
		if (!model.containsAttribute("shiftForm")) {
			model.addAttribute("shiftForm", new ShiftForm(
				shift.getEmployeeId(),
				shift.getWorkDate(),
				shift.getShiftSlot(),
				shift.getNote()
			));
		}
		model.addAttribute("shift", shift);
		prepareShiftFormModel(model, "edit");
		model.addAttribute("adminSection", "shifts");
		return "admin/shift-form";
	}

	@PostMapping("/{id}/edit")
	public String update(
		@PathVariable Long id,
		@Valid @ModelAttribute("shiftForm") ShiftForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		Shift shift = shiftService.findById(id);
		if (bindingResult.hasErrors()) {
			model.addAttribute("shift", shift);
			prepareShiftFormModel(model, "edit");
			model.addAttribute("adminSection", "shifts");
			return "admin/shift-form";
		}
		try {
			shiftService.update(id, form);
			redirectAttributes.addFlashAttribute("shiftMessage", "シフトを更新しました。");
			return "redirect:/admin/shifts";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("shift", shift);
			prepareShiftFormModel(model, "edit");
			model.addAttribute("adminSection", "shifts");
			model.addAttribute("shiftError", ex.getMessage());
			return "admin/shift-form";
		}
	}

	private void prepareShiftFormModel(Model model, String mode) {
		model.addAttribute("employeeOptions", employeeService.findAll().stream()
			.filter(employee -> Boolean.TRUE.equals(employee.getActive()))
			.sorted(Comparator.comparing(Employee::getName))
			.toList());
		model.addAttribute("shiftSlots", List.of(
			new ShiftSlotOption(ShiftSlot.EARLY.name(), ShiftSlot.EARLY.getLabel()),
			new ShiftSlotOption(ShiftSlot.LATE.name(), ShiftSlot.LATE.getLabel()),
			new ShiftSlotOption(ShiftSlot.FULL.name(), ShiftSlot.FULL.getLabel())
		));
		model.addAttribute("mode", mode);
	}

	private YearMonth resolveYearMonth(Integer year, Integer month) {
		YearMonth now = YearMonth.now();
		int y = (year == null) ? now.getYear() : year;
		int m = (month == null) ? now.getMonthValue() : month;
		try {
			return YearMonth.of(y, m);
		} catch (RuntimeException ex) {
			return now;
		}
	}

	private boolean isCustomerEmployee(Employee employee) {
		String role = employee.getRole() == null ? "" : employee.getRole().trim();
		String position = employee.getPosition() == null ? "" : employee.getPosition().trim();
		return "CUSTOMER".equalsIgnoreCase(role) || "顧客".equals(position);
	}

	private Map<Long, Map<LocalDate, List<Shift>>> createMatrix(List<Employee> employees, List<LocalDate> monthDays) {
		Map<Long, Map<LocalDate, List<Shift>>> matrix = new LinkedHashMap<>();
		for (Employee employee : employees) {
			Map<LocalDate, List<Shift>> row = new LinkedHashMap<>();
			for (LocalDate day : monthDays) {
				row.put(day, new ArrayList<>());
			}
			matrix.put(employee.getId(), row);
		}
		return matrix;
	}

	private record ShiftDayCell(LocalDate day, List<Shift> shifts) {
	}

	private record ShiftEmployeeRow(Employee employee, long shiftCount, List<ShiftDayCell> dayCells) {
	}

	private Map<String, Boolean> buildHolidayMap(List<LocalDate> monthDays) {
		Map<String, Boolean> result = new LinkedHashMap<>();
		if (monthDays.isEmpty()) {
			return result;
		}
		Set<LocalDate> holidays = buildJapaneseHolidayNameMap(monthDays.get(0).getYear()).keySet();
		for (LocalDate day : monthDays) {
			result.put(day.toString(), holidays.contains(day));
		}
		return result;
	}

	private Map<String, String> buildHolidayNameMap(List<LocalDate> monthDays) {
		Map<String, String> result = new LinkedHashMap<>();
		if (monthDays.isEmpty()) {
			return result;
		}
		Map<LocalDate, String> all = buildJapaneseHolidayNameMap(monthDays.get(0).getYear());
		for (LocalDate day : monthDays) {
			if (all.containsKey(day)) {
				result.put(day.toString(), all.get(day));
			}
		}
		return result;
	}

	private Map<LocalDate, String> buildJapaneseHolidayNameMap(int year) {
		Map<LocalDate, String> holidays = new LinkedHashMap<>();
		holidays.put(LocalDate.of(year, 1, 1), "元日");
		holidays.put(nthMonday(year, 1, 2), "成人の日");
		holidays.put(LocalDate.of(year, 2, 11), "建国記念の日");
		holidays.put(LocalDate.of(year, 2, 23), "天皇誕生日");
		holidays.put(LocalDate.of(year, 3, springEquinoxDay(year)), "春分の日");
		holidays.put(LocalDate.of(year, 4, 29), "昭和の日");
		holidays.put(LocalDate.of(year, 5, 3), "憲法記念日");
		holidays.put(LocalDate.of(year, 5, 4), "みどりの日");
		holidays.put(LocalDate.of(year, 5, 5), "こどもの日");
		holidays.put(nthMonday(year, 7, 3), "海の日");
		holidays.put(LocalDate.of(year, 8, 11), "山の日");
		holidays.put(nthMonday(year, 9, 3), "敬老の日");
		holidays.put(LocalDate.of(year, 9, autumnEquinoxDay(year)), "秋分の日");
		holidays.put(nthMonday(year, 10, 2), "スポーツの日");
		holidays.put(LocalDate.of(year, 11, 3), "文化の日");
		holidays.put(LocalDate.of(year, 11, 23), "勤労感謝の日");

		Set<LocalDate> current = new HashSet<>(holidays.keySet());
		Map<LocalDate, String> substitute = new LinkedHashMap<>();
		for (LocalDate holiday : current) {
			if (holiday.getDayOfWeek().getValue() == 7) {
				LocalDate next = holiday.plusDays(1);
				while (current.contains(next) || substitute.containsKey(next)) {
					next = next.plusDays(1);
				}
				substitute.put(next, "振替休日");
			}
		}
		holidays.putAll(substitute);
		return holidays;
	}

	private LocalDate nthMonday(int year, int month, int nth) {
		LocalDate first = LocalDate.of(year, month, 1);
		int diff = (8 - first.getDayOfWeek().getValue()) % 7;
		return first.plusDays(diff + (long) (nth - 1) * 7);
	}

	private int springEquinoxDay(int year) {
		return (int) Math.floor(20.8431 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0));
	}

	private int autumnEquinoxDay(int year) {
		return (int) Math.floor(23.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0));
	}

	private record ShiftSlotOption(String value, String label) {
	}

	@ExceptionHandler(Throwable.class)
	public String handleShiftRuntimeError(Throwable ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("shiftError", "シフト処理でエラーが発生しました: " + ex.getMessage());
		return "redirect:/admin/shifts";
	}
}
