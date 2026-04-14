package com.group5.ems.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_reports")
public class HrReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // OVERVIEW, ATTENDANCE, LEAVE, PAYROLL, PERFORMANCE

    @Column(nullable = false, length = 10)
    private String format; // PDF, CSV

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT"; // DRAFT, FINALIZED

    @Column(columnDefinition = "TEXT")
    private String remarks; // Executive Summary

    @Column(name = "is_published")
    private boolean isPublished = false;

    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "generated_by_id")
    private Long generatedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id", insertable = false, updatable = false)
    private Employee generatedBy;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean isPublished) { this.isPublished = isPublished; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Long getGeneratedById() { return generatedById; }
    public void setGeneratedById(Long generatedById) { this.generatedById = generatedById; }
    public Employee getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Employee generatedBy) { this.generatedBy = generatedBy; }
}
