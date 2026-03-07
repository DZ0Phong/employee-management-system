package com.group5.ems.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payslips")
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "actual_base_salary", precision = 15, scale = 2)
    private BigDecimal actualBaseSalary;

    @Column(name = "total_ot_amount", precision = 15, scale = 2)
    private BigDecimal totalOtAmount;

    @Column(name = "total_bonus", precision = 15, scale = 2)
    private BigDecimal totalBonus;

    @Column(name = "total_deduction", precision = 15, scale = 2)
    private BigDecimal totalDeduction;

    @Column(name = "net_salary", precision = 15, scale = 2)
    private BigDecimal netSalary;

    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "approved_by")
    private Long approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", insertable = false, updatable = false)
    private TimesheetPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    private User approvedByUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public BigDecimal getActualBaseSalary() { return actualBaseSalary; }
    public void setActualBaseSalary(BigDecimal actualBaseSalary) { this.actualBaseSalary = actualBaseSalary; }
    public BigDecimal getTotalOtAmount() { return totalOtAmount; }
    public void setTotalOtAmount(BigDecimal totalOtAmount) { this.totalOtAmount = totalOtAmount; }
    public BigDecimal getTotalBonus() { return totalBonus; }
    public void setTotalBonus(BigDecimal totalBonus) { this.totalBonus = totalBonus; }
    public BigDecimal getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public TimesheetPeriod getPeriod() { return period; }
    public void setPeriod(TimesheetPeriod period) { this.period = period; }
    public User getApprovedByUser() { return approvedByUser; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
}
