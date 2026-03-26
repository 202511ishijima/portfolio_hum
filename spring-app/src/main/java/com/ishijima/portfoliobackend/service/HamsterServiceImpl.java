package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterSex;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.form.HamsterForm;
import com.ishijima.portfoliobackend.mapper.HamsterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HamsterServiceImpl implements HamsterService {

	private static final String DEFAULT_HEALTH_CONDITION = "店頭で個別管理";

	private final HamsterMapper hamsterMapper;

	@Override
	public List<Hamster> findAll(String species, HamsterSex sex, HamsterStatus status) {
		return hamsterMapper.findAll(normalize(species), sex, status);
	}

	@Override
	public Hamster findById(Long id) {
		return hamsterMapper.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("対象のハムスター情報が見つかりません。id=" + id));
	}

	@Override
	public List<String> findSpeciesOptions() {
		return hamsterMapper.findSpeciesOptions();
	}

	@Override
	@Transactional
	public Hamster create(HamsterForm form) {
		return buildHamster(form);
	}

	@Override
	@Transactional
	public List<Hamster> createMultiple(HamsterForm form) {
		int quantity = form.getQuantity() == null ? 1 : form.getQuantity();
		List<Hamster> hamsters = new ArrayList<>();
		for (int i = 0; i < quantity; i++) {
			hamsters.add(buildHamster(form));
		}
		return hamsters;
	}

	@Override
	@Transactional
	public Hamster update(Long id, HamsterForm form) {
		Hamster hamster = findById(id);
		hamster.setSpecies(form.getSpecies().trim());
		hamster.setSex(form.getSex());
		hamster.setBirthDate(form.getBirthDate());
		hamster.setHealthCondition(DEFAULT_HEALTH_CONDITION);
		hamster.setArrivalDate(form.getArrivalDate());
		hamster.setStatus(form.getStatus());
		hamster.setNotes(normalizeNullable(form.getNotes()));
		hamster.setUpdatedAt(LocalDateTime.now());

		hamsterMapper.update(hamster);
		return hamster;
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		findById(id);
		hamsterMapper.deleteById(id);
	}

	private Hamster buildHamster(HamsterForm form) {
		Hamster hamster = Hamster.builder()
			.species(form.getSpecies().trim())
			.sex(form.getSex())
			.birthDate(form.getBirthDate())
			.healthCondition(DEFAULT_HEALTH_CONDITION)
			.arrivalDate(form.getArrivalDate())
			.status(form.getStatus())
			.notes(normalizeNullable(form.getNotes()))
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		hamsterMapper.insert(hamster);
		return hamster;
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String normalizeNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
