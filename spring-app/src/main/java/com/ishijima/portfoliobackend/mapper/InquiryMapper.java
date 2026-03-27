package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.Inquiry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface InquiryMapper {

	void insert(Inquiry inquiry);

	List<Inquiry> findAll();

	Optional<Inquiry> findById(@Param("id") Long id);

	List<Inquiry> findByEmail(@Param("email") String email);

	void updateStatus(@Param("id") Long id, @Param("status") String status);

	void updateReply(@Param("id") Long id, @Param("reply") String reply, @Param("repliedAt") java.time.LocalDateTime repliedAt);
}
