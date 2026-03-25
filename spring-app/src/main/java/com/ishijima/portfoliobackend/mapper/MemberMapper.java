package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MemberMapper {

	void insert(Member member);

	List<Member> findAll();

	Optional<Member> findById(@Param("id") Long id);

	Optional<Member> findByEmail(@Param("email") String email);

	void updateStatus(@Param("id") Long id, @Param("status") String status);

	void updatePoints(@Param("id") Long id, @Param("points") Integer points);

	void deleteById(@Param("id") Long id);

	void updateMember(Member member);
}
