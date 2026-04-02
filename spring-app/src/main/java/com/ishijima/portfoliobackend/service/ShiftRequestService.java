package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.ShiftRequestDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ShiftRequestService {

	int findWeeklyDays(Long employeeId, int targetYear, int targetMonth);

	Map<LocalDate, String> findRequestMap(Long employeeId, int targetYear, int targetMonth);

	void saveMonthlyRequests(Long employeeId, int targetYear, int targetMonth, int weeklyDays, Map<LocalDate, String> requestMap);

	int autoGenerateShifts(int targetYear, int targetMonth);

	List<ShiftRequestDay> findAllByMonth(int targetYear, int targetMonth);
}
