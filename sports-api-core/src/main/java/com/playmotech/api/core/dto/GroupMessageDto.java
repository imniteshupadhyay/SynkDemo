package com.playmotech.api.core.dto;

import java.util.List;

//import com.playmotech.api.core.dao.Message;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class GroupMessageDto {
	private String id;
	private String groupId;
	private List<MessageDto> messages;
}
