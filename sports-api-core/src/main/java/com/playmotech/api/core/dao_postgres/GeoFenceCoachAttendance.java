package com.playmotech.api.core.dao_postgres;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing coach attendance records
 */
@Entity
@Table(name = "coach_attendances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceCoachAttendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id", nullable = false)
	private Academy academy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "program_id", referencedColumnName = "id", nullable = false)
	private Course program;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coach_id", referencedColumnName = "id", nullable = false)
	private UserProfile coach;

	// ✅ Geo-fence definition (renamed to avoid confusion)
	@Column(name = "geofence_latitude", nullable = false)
	private Double geofenceLatitude;

	@Column(name = "geofence_longitude", nullable = false)
	private Double geofenceLongitude;

	@Column(name = "geofence_radius_meters", nullable = false)
	private Long geofenceRadiusMeters;

	@Column(name = "check_in_latitude", nullable = false)
	private Double checkInLatitude;

	@Column(name = "check_in_longitude", nullable = false)
	private Double checkInLongitude;

	@Column(name = "check_in_distance_meters")
	private Double checkInDistanceMeters;

	@Column(name = "check_out_latitude")
	private Double checkOutLatitude;

	@Column(name = "check_out_longitude")
	private Double checkOutLongitude;

	@Column(name = "check_out_distance_meters")
	private Double checkOutDistanceMeters;

	@Column(name = "check_in_time", nullable = false)
	private Instant checkInTime;

	@Column(name = "check_out_time")
	private Instant checkOutTime;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_on")
	@CreationTimestamp
	private Instant createdOn;

	@Column(name = "updated_on")
	@UpdateTimestamp
	private Instant updatedOn;

	@Column(name = "deleted")
	private Boolean deleted;

	@Builder.Default
	@Column(name = "auto_checked_out")
	private Boolean autoCheckedOut = false;
}
