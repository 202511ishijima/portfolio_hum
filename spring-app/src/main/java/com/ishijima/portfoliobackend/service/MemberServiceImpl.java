package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberCreateForm;
import com.ishijima.portfoliobackend.form.MemberUpdateForm;
import com.ishijima.portfoliobackend.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public Member create(MemberCreateForm form) {
		memberMapper.findByEmail(form.email()).ifPresent(member -> {
			throw new IllegalArgumentException("このメールアドレスは既に登録されています。");
		});

		Member member = Member.builder()
			.name(form.name())
			.email(form.email())
			.password(passwordEncoder.encode(form.password()))
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
	public Member findByEmail(String email) {
		return memberMapper.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("会員が見つかりません。email=" + email));
	}

	@Override
	@Transactional
	public Member authenticate(String email, String rawPassword) {
		Member member = findByEmail(email);
		String storedPassword = member.getPassword() == null ? "" : member.getPassword();

		boolean matched;
		if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
			matched = passwordEncoder.matches(rawPassword, storedPassword);
		} else {
			matched = storedPassword.equals(rawPassword);
			if (matched) {
				member.setPassword(passwordEncoder.encode(rawPassword));
				member.setUpdatedAt(LocalDateTime.now());
				memberMapper.updateMember(member);
			}
		}

		if (!matched) {
			throw new IllegalArgumentException("メールアドレスまたはパスワードが正しくありません。");
		}
		return member;
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

	@Override
	@Transactional
	public Member adjustPointsByEmail(String email, Integer delta) {
		Member member = findByEmail(email);
		int currentPoints = member.getPoints() == null ? 0 : member.getPoints();
		int nextPoints = Math.max(0, currentPoints + delta);
		memberMapper.updatePoints(member.getId(), nextPoints);
		member.setPoints(nextPoints);
		return member;
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		findById(id);
		memberMapper.deleteById(id);
	}

	@Override
	@Transactional
	public Member update(Long id, MemberUpdateForm form) {
		Member member = findById(id);

		memberMapper.findByEmail(form.email()).ifPresent(existingMember -> {
			if (!existingMember.getId().equals(id)) {
				throw new IllegalArgumentException("このメールアドレスは既に登録されています。");
			}
		});

		member.setName(form.name());
		member.setEmail(form.email());
		if (form.password() != null && !form.password().isBlank()) {
			member.setPassword(passwordEncoder.encode(form.password()));
		}
		member.setUpdatedAt(LocalDateTime.now());

		memberMapper.updateMember(member);
		return member;
	}
}
