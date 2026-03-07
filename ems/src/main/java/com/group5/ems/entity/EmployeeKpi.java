package com.group5.ems.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_kpis")
public class EmployeeKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 30)
    private String period;

    @Column(name = "kpi_name", nullable = false, length = 200)
    private String kpiName;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ZERO;

    @Column(name = "target_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "achieved_value", precision = 15, scale = 2)
    private BigDecimal achievedValue = BigDecimal.ZERO;

    @Column(name = "completion_rate", precision = 5, scale = 2, insertable = false, updatable = false)
    private BigDecimal completionRate;

    @Column(length = 30)
    private String status = "IN_PROGRESS";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getKpiName() { return kpiName; }
    public void setKpiName(String kpiName) { this.kpiName = kpiName; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public BigDecimal getAchievedValue() { return achievedValue; }
    public void setAchievedValue(BigDecimal achievedValue) { this.achievedValue = achievedValue; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
}
