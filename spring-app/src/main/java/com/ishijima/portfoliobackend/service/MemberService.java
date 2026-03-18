package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberCreateForm;

import java.util.List;

public interface MemberService {

	Member create(MemberCreateForm form);

	List<Member> findAll();

	Member findById(Long id);

	void toggleStatus(Long id);

	void adjustPoints(Long id, Integer delta);
}
