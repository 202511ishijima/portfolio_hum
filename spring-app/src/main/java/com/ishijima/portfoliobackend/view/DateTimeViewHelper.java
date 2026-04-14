package com.ishijima.portfoliobackend.view;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component("dateTimeViewHelper")
public class DateTimeViewHelper {

	private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

	public String formatUtcToJst(LocalDateTime utcDateTime, String pattern) {
		if (utcDateTime == null) {
			return "-";
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		return utcDateTime
			.atOffset(ZoneOffset.UTC)
			.atZoneSameInstant(JST)
			.toLocalDateTime()
			.format(formatter);
	}
}

