package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberCreateForm;
import com.ishijima.portfoliobackend.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

	private final MemberMapper memberMapper;

	@Override
	@Transactional
	public Member create(MemberCreateForm form) {
		memberMapper.findByEmail(form.email()).ifPresent(member -> {
			throw new IllegalArgumentException("このメールアドレスはすでに登録されています。");
		});

		Member member = Member.builder()
			.name(form.name())
			.email(form.email())
			.password(form.password())
			.status("ACTIVE")
			.points(0)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		memberMapper.insert(member);
		return member;
	}

	@Override
	public List<Member> findAll() {
		return memberMapper.findAll();
	}

	@Override
	public Member findById(Long id) {
		return memberMapper.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("会員が見つかりません。id=" + id));
	}

	@Override
	@Transactional
	public void toggleStatus(Long id) {
		Member member = findById(id);
		String nextStatus = "ACTIVE".equals(member.getStatus()) ? "SUSPENDED" : "ACTIVE";
		memberMapper.updateStatus(id, nextStatus);
	}

	@Override
	@Transactional
	public void adjustPoints(Long id, Integer delta) {
		Member member = findById(id);
		int currentPoints = member.getPoints() == null ? 0 : member.getPoints();
		int nextPoints = Math.max(0, currentPoints + delta);
		memberMapper.updatePoints(id, nextPoints);
	}
}
