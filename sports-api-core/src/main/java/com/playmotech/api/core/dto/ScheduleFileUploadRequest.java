package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleFileUploadRequest {
    private FileObjectDto file;

    private Map<String, List<String>> programMapping;
}
