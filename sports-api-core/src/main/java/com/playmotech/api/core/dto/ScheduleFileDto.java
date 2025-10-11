package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class ScheduleFileDto {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String createdAt;
}
