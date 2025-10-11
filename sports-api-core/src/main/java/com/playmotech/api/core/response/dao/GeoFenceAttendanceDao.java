package com.playmotech.api.core.response.dao;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for Attendance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceAttendanceDao {
    private Long id;
    private String academyId;
    private String programId;
    private String coachId;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkInDistanceMeters;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
    private Double checkOutDistanceMeters;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Kolkata")
    private Timestamp checkInTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Kolkata")
    private Timestamp checkOutTime;
    private Boolean isActive;
}
