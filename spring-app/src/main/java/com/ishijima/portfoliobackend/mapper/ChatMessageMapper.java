package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

	List<ChatMessage> findByRoomId(@Param("roomId") Long roomId);

	void insert(ChatMessage message);
}
