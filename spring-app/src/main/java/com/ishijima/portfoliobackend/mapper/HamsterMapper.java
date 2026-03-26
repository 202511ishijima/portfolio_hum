package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.entity.HamsterSex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface HamsterMapper {

	List<Hamster> findAll(
		@Param("species") String species,
		@Param("sex") HamsterSex sex,
		@Param("status") HamsterStatus status
	);

	Optional<Hamster> findById(@Param("id") Long id);

	List<String> findSpeciesOptions();

	void insert(Hamster hamster);

	void update(Hamster hamster);

	void deleteById(@Param("id") Long id);
}
