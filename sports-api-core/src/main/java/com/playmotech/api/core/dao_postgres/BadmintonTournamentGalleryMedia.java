package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import com.playmotech.api.core.constants.MediaType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Created By: deep.patel
 **/

@Data
@Entity
@Table(name = "badminton_tournament_gallery_media")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BadmintonTournamentGalleryMedia {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "tournament_id", referencedColumnName = "id")
	private BadmintonTournament badmintonTournament;

	@Column(name = "media_path")
	private String mediaPath;

	@Column(name = "media_type")
	@Enumerated(EnumType.STRING)
	private MediaType mediaType;

	@Column(name = "created_on")
	private Timestamp createdOn;
}
