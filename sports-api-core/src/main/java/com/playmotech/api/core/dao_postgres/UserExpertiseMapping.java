package com.playmotech.api.core.dao_postgres;

import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Entity
@Table(name = "user_expertise_mappings")
@Data
public class UserExpertiseMapping {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserProfile userProfile;

	@Column
	@Enumerated(value = EnumType.STRING)
	private SkillLevel expertise;

	@Column
	@Enumerated(value = EnumType.STRING)
	private Sports sport;
}
