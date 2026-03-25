package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.entity.InquiryReply;
import com.ishijima.portfoliobackend.form.InquiryForm;
import com.ishijima.portfoliobackend.mapper.InquiryMapper;
import com.ishijima.portfoliobackend.mapper.InquiryReplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

	private final InquiryMapper inquiryMapper;
	private final InquiryReplyMapper inquiryReplyMapper;

	@Override
	@Transactional
	public Inquiry create(InquiryForm form) {
		Inquiry inquiry = Inquiry.builder()
			.name(form.name())
			.email(form.email())
			.subject(form.subject())
			.message(form.message())
			.status("NEW")
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		inquiryMapper.insert(inquiry);
		return inquiry;
	}

	@Override
	public List<Inquiry> findAll() {
		return inquiryMapper.findAll();
	}

	@Override
	public Inquiry findById(Long id) {
		return inquiryMapper.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("問い合わせが見つかりません。id=" + id));
	}

	@Override
	@Transactional
	public void updateStatus(Long id, String status) {
		findById(id);
		inquiryMapper.updateStatus(id, status);
	}

	@Override
	@Transactional
	public void sendReply(Long id, String reply) {
		Inquiry inquiry = findById(id);
		LocalDateTime now = LocalDateTime.now();

		InquiryReply inquiryReply = InquiryReply.builder()
			.inquiryId(inquiry.getId())
			.recipientEmail(inquiry.getEmail())
			.reply(reply)
			.sentAt(now)
			.createdAt(now)
			.build();

		inquiryReplyMapper.insert(inquiryReply);
		inquiryMapper.updateReply(id, reply, now);
	}

	@Override
	public List<InquiryReply> findRepliesByInquiryId(Long inquiryId) {
		return inquiryReplyMapper.findByInquiryId(inquiryId);
	}
}
