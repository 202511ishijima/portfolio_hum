package com.ishijima.portfoliobackend.controller.api;

import com.ishijima.portfoliobackend.dto.HamsterSummaryResponse;
import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.entity.HamsterSex;
import com.ishijima.portfoliobackend.form.HamsterForm;
import com.ishijima.portfoliobackend.service.HamsterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hamsters")
@RequiredArgsConstructor
public class HamsterApiController {

	private static final List<String> SPECIES_ORDER = List.of(
		"ゴールデンハムスター",
		"ジャンガリアンハムスター",
		"ロボロフスキーハムスター",
		"キャンベルハムスター",
		"チャイニーズハムスター",
		"シベリアンハムスター",
		"ドワーフハムスター"
	);

	private final HamsterService hamsterService;

	@GetMapping
	public List<Hamster> findAll(
		@RequestParam(required = false) String species,
		@RequestParam(required = false) HamsterSex sex,
		@RequestParam(required = false) HamsterStatus status
	) {
		return hamsterService.findAll(species, sex, status);
	}

	@GetMapping("/{id}")
	public Hamster findById(@PathVariable Long id) {
		return hamsterService.findById(id);
	}

	@GetMapping("/summary")
	public List<HamsterSummaryResponse> findSummary() {
		Map<String, List<Hamster>> grouped = hamsterService.findAll(null, null, null).stream()
			.filter(hamster -> hamster.getStatus() == HamsterStatus.AVAILABLE || hamster.getStatus() == HamsterStatus.NEGOTIATING)
			.collect(Collectors.groupingBy(Hamster::getSpecies));

		return grouped.entrySet().stream()
			.map(entry -> toSummary(entry.getKey(), entry.getValue()))
			.sorted(Comparator.comparingInt((HamsterSummaryResponse summary) -> {
				int index = SPECIES_ORDER.indexOf(summary.getSpecies());
				return index >= 0 ? index : Integer.MAX_VALUE;
			}).thenComparing(HamsterSummaryResponse::getSpecies))
			.toList();
	}

	@PostMapping
	public ResponseEntity<Hamster> create(@Valid @RequestBody HamsterForm form) {
		return ResponseEntity.status(HttpStatus.CREATED).body(hamsterService.create(form));
	}

	@PutMapping("/{id}")
	public Hamster update(@PathVariable Long id, @Valid @RequestBody HamsterForm form) {
		return hamsterService.update(id, form);
	}

	private HamsterSummaryResponse toSummary(String species, List<Hamster> hamsters) {
		int maleCount = (int) hamsters.stream().filter(hamster -> hamster.getSex() == HamsterSex.MALE).count();
		int femaleCount = (int) hamsters.stream().filter(hamster -> hamster.getSex() == HamsterSex.FEMALE).count();

		List<Integer> months = hamsters.stream()
			.map(Hamster::getBirthDate)
			.filter(birthDate -> birthDate != null)
			.map(this::toMonthsOld)
			.toList();

		return new HamsterSummaryResponse(
			species,
			hamsters.size(),
			maleCount,
			femaleCount,
			buildAgeRange(months)
		);
	}

	private int toMonthsOld(LocalDate birthDate) {
		Period period = Period.between(birthDate, LocalDate.now());
		return Math.max(0, period.getYears() * 12 + period.getMonths());
	}

	private String buildAgeRange(List<Integer> months) {
		if (months.isEmpty()) {
			return "詳細は店頭またはお問い合わせにてご案内します";
		}

		int min = months.stream().min(Integer::compareTo).orElse(0);
		int max = months.stream().max(Integer::compareTo).orElse(min);
		if (min == max) {
			return "生後" + min + "ヶ月前後の子がいます";
		}
		return "生後" + min + "〜" + max + "ヶ月の子がいます";
	}
}
