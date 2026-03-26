package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.InquiryReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InquiryReplyMapper {

	void insert(InquiryReply inquiryReply);

	List<InquiryReply> findByInquiryId(@Param("inquiryId") Long inquiryId);

	List<InquiryReply> findByRecipientEmail(@Param("recipientEmail") String recipientEmail);
}
