package com.ishijima.portfoliobackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hamster {

	private Long id;
	private String species;
	private HamsterSex sex;
	private LocalDate birthDate;
	private String healthCondition;
	private LocalDate arrivalDate;
	private HamsterStatus status;
	private String notes;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
