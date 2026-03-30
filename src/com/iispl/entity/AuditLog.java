package com.iispl.entity;

import java.time.LocalDateTime;

import com.iispl.enums.AuditAction;

/**
 * Immutable audit-trail entry — one row per entity change. Never updated after
 * creation; only INSERT is performed by the DAO.
 */
public class AuditLog {

	private Long logId;
	private String entityType; // simple class name, e.g. "Account"
	private Long entityId;
	private AuditAction action;
	private String oldValue; // JSON snapshot before change
	private String newValue; // JSON snapshot after change
	private String changedBy;
	private LocalDateTime changedAt;
	private String ipAddress;

	public AuditLog() {
	}

	public AuditLog(String entityType, Long entityId, AuditAction action, String oldValue, String newValue,
			String changedBy, String ipAddress) {
		this.entityType = entityType;
		this.entityId = entityId;
		this.action = action;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.changedBy = changedBy;
		this.changedAt = LocalDateTime.now();
		this.ipAddress = ipAddress;
	}

	// Getters / Setters
	public Long getLogId() {
		return logId;
	}

	public void setLogId(Long logId) {
		this.logId = logId;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public AuditAction getAction() {
		return action;
	}

	public void setAction(AuditAction action) {
		this.action = action;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}

	public void setChangedAt(LocalDateTime changedAt) {
		this.changedAt = changedAt;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public String toString() {
		return "AuditLog{id=" + logId + ", entity=" + entityType + "#" + entityId + ", action=" + action + ", by="
				+ changedBy + ", at=" + changedAt + "}";
	}
}
