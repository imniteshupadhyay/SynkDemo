package com.playmotech.api.core.dao_postgres;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "video_analysis")
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class VideoAnalysis {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private String id;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "video_analyzer_id")
	private VideoAnalyzer videoAnalyzer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "analysed_by_user_id", referencedColumnName = "id")
	private UserProfile analysedByUser;

	@Column(name = "video_frame_time")
	private String videoFrameTime;

	@Column(name = "audio_media_url", nullable = true)
	private String audioMediaUrl;

	@Column(name = "image_url", nullable = true)
	private String imageUrl;

	@Column(name = "comment", nullable = false)
	private String comment;

}
