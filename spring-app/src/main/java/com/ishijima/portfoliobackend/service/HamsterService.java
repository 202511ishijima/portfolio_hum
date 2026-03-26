package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterSex;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.form.HamsterForm;

import java.util.List;

public interface HamsterService {

	List<Hamster> findAll(String species, HamsterSex sex, HamsterStatus status);

	Hamster findById(Long id);

	List<String> findSpeciesOptions();

	Hamster create(HamsterForm form);

	List<Hamster> createMultiple(HamsterForm form);

	Hamster update(Long id, HamsterForm form);

	void deleteById(Long id);
}
