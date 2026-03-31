package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.PositionPermission;

import java.util.List;
import java.util.Optional;

public interface PositionPermissionService {

	List<PositionPermission> findAll();

	Optional<PositionPermission> findByPosition(String position);

	String resolveRoleByPosition(String position);

	void update(PositionPermission permission, String actorLoginId);
}
