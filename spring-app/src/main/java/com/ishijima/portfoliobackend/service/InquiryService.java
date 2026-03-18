package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.form.InquiryForm;

import java.util.List;

public interface InquiryService {

	Inquiry create(InquiryForm form);

	List<Inquiry> findAll();

	Inquiry findById(Long id);

	void updateStatus(Long id, String status);
}
