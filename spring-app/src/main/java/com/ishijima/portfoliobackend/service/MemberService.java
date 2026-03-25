package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberCreateForm;
import com.ishijima.portfoliobackend.form.MemberUpdateForm;

import java.util.List;

public interface MemberService {

	Member create(MemberCreateForm form);

	List<Member> findAll();

	Member findById(Long id);

	Member findByEmail(String email);

	void toggleStatus(Long id);

	void adjustPoints(Long id, Integer delta);

	Member adjustPointsByEmail(String email, Integer delta);

	void deleteById(Long id);

	Member update(Long id, MemberUpdateForm form);
}
