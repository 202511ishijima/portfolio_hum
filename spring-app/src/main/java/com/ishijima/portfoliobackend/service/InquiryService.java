package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.entity.InquiryReply;
import com.ishijima.portfoliobackend.form.InquiryForm;

import java.util.List;

public interface InquiryService {

	Inquiry create(InquiryForm form);

	List<Inquiry> findAll();

	Inquiry findById(Long id);

	List<Inquiry> findByEmail(String email);

	void updateStatus(Long id, String status);

	void sendReply(Long id, String reply);

	List<InquiryReply> findRepliesByInquiryId(Long inquiryId);

	List<InquiryReply> findRepliesByRecipientEmail(String recipientEmail);
}
