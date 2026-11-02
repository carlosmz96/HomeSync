package com.homesync.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "email_normalized", nullable = false, length = 255)
	private String emailNormalized;

	@Column(name = "password_hash", nullable = false, columnDefinition = "text")
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	private UserStatus status;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public User(
			UUID id,
			String email,
			String emailNormalized,
			String passwordHash,
			String displayName,
			UserStatus status,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {

		this.id = id;
		this.email = email;
		this.emailNormalized = emailNormalized;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

}