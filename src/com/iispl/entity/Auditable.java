package com.iispl.entity;
import java.time.LocalDateTime;

/**
 * Marker interface for all entities that require audit trail fields.
 */
public interface Auditable {
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    String getCreatedBy();
}