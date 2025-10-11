package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "video_analyzer")
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class VideoAnalyzer {

	@Id
	// @GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private String id;

	@Column(name = "media_url")
	private String mediaUrl;

	@Column(name = "thumbnail_url")
	private String thumbnailUrl;

	@Column(name = "title")
	private String title;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", referencedColumnName = "id")
	private UserProfile player;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_id", referencedColumnName = "id")
	private UserProfile createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_id", referencedColumnName = "id")
	private UserProfile updatedBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@Column(name = "is_analysed")
	private boolean isAnalysed;

	@JsonIgnore
	@Column(name = "deleted")
	private Boolean deleted;

	@JsonManagedReference
	@OneToMany(mappedBy = "videoAnalyzer", cascade = CascadeType.ALL)
	private List<VideoAnalysis> analysis = new ArrayList<>();

	@JsonManagedReference
	@OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<VideoAnalytics> analytics;

	@JsonIgnore
	@CreationTimestamp
	@Column(name = "inserted_on", updatable = false)
	private LocalDateTime insertedOn;

	@JsonIgnore
	@UpdateTimestamp
	@Column(name = "updated_on", insertable = false)
	private LocalDateTime updatedOn;

	// @JsonIgnore
	// @Transient
	// private boolean notAnalysed;

}
