package com.campusconnect.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * One entry in a complaint's status history, used to show the tracking timeline.
 */
@Entity
@Table(name = "status_updates")
public class StatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false, length = 100)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public StatusUpdate() {
    }

    public StatusUpdate(Complaint complaint, ComplaintStatus status, String remarks, String updatedBy) {
        this.complaint = complaint;
        this.status = status;
        this.remarks = remarks;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
