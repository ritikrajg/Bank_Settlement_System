package com.iispl.entity;

import com.iispl.enums.ProtocolType;
import com.iispl.enums.SourceType;

public class SourceSystem extends BaseEntity implements Validatable {

	private String systemCode; // CBS / RTGS / SWIFT / NEFT / UPI / FINTECH
	private SourceType sourceType;
	private ProtocolType protocol;
	private String connectionConfig; // JSON blob (URL, credentials, queue name …)
	private boolean active;
	private String contactEmail;

	public SourceSystem() {
	}

	public SourceSystem(String systemCode, SourceType sourceType, ProtocolType protocol, String connectionConfig,
			boolean active, String contactEmail) {
		this.systemCode = systemCode;
		this.sourceType = sourceType;
		this.protocol = protocol;
		this.connectionConfig = connectionConfig;
		this.active = active;
		this.contactEmail = contactEmail;
	}

	// ------------------------------------------------------------------ //
	// Validatable //
	// ------------------------------------------------------------------ //

	@Override
	public boolean isValid() {
		return systemCode != null && !systemCode.isBlank() && sourceType != null && protocol != null;
	}

	@Override
	public String validationErrors() {
		StringBuilder sb = new StringBuilder();
		if (systemCode == null || systemCode.isBlank())
			sb.append("systemCode is required; ");
		if (sourceType == null)
			sb.append("sourceType is required; ");
		if (protocol == null)
			sb.append("protocol is required; ");
		return sb.toString();
	}

	// ------------------------------------------------------------------ //
	// Getters / Setters //
	// ------------------------------------------------------------------ //

	public String getSystemCode() {
		return systemCode;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(SourceType sourceType) {
		this.sourceType = sourceType;
	}

	public ProtocolType getProtocol() {
		return protocol;
	}

	public void setProtocol(ProtocolType protocol) {
		this.protocol = protocol;
	}

	public String getConnectionConfig() {
		return connectionConfig;
	}

	public void setConnectionConfig(String connectionConfig) {
		this.connectionConfig = connectionConfig;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	@Override
	public String toString() {
		return "SourceSystem{id=" + getId() + ", code=" + systemCode + ", type=" + sourceType + ", protocol=" + protocol
				+ "}";
	}
}
