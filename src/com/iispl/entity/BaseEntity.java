package com.iispl.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Abstract superclass for every persistent domain entity.
 *
 * Provides:
 *   - surrogate primary key (id)
 *   - optimistic-locking version counter
 *   - full audit timestamps (createdAt, updatedAt, createdBy)
 *
 * Implements Auditable and Serializable so that all subclasses
 * automatically satisfy those contracts.
 */
public abstract class BaseEntity implements Auditable, Serializable {

    private static final long serialVersionUID = 1L;

    /** Surrogate primary key — set by the DAO after INSERT. */
    private Long id;

    /** Optimistic locking version — incremented on each UPDATE. */
    private int version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    // ------------------------------------------------------------------ //
    //  Lifecycle helpers (called by DAO layer, not by application code)   //
    // ------------------------------------------------------------------ //

    /**
     * Stamps the entity as newly created.
     * DAOs must call this before the first INSERT.
     */
    public void prePersist(String actor) {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = actor;
        this.version   = 0;
    }

    /**
     * Refreshes the updatedAt timestamp.
     * DAOs must call this before every UPDATE.
     */
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    // ------------------------------------------------------------------ //
    //  Getters / setters                                                   //
    // ------------------------------------------------------------------ //

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public int getVersion()                    { return version; }
    public void setVersion(int version)        { this.version = version; }

    @Override
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }

    @Override
    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)  { this.updatedAt = t; }

    @Override
    public String getCreatedBy()               { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    // ------------------------------------------------------------------ //
    //  Object identity based on id                                         //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", version=" + version + "}";
    }
}
