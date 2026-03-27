package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.ChatRoom;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface ChatRoomMapper {

	Optional<ChatRoom> findGeneralRoom();

	void insert(ChatRoom room);
}
