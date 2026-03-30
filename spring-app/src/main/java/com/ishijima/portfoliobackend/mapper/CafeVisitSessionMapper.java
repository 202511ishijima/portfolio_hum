package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.CafeVisitSession;
import com.ishijima.portfoliobackend.entity.CafeVisitSessionStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CafeVisitSessionMapper {

	void insert(CafeVisitSession session);

	Optional<CafeVisitSession> findById(@Param("id") Long id);

	Optional<CafeVisitSession> findByToken(@Param("sessionToken") String sessionToken);

	List<CafeVisitSession> findActiveSessions(@Param("now") LocalDateTime now);

	int updateStatus(
		@Param("id") Long id,
		@Param("status") CafeVisitSessionStatus status,
		@Param("checkoutCompletedAt") LocalDateTime checkoutCompletedAt,
		@Param("updatedAt") LocalDateTime updatedAt
	);
}
