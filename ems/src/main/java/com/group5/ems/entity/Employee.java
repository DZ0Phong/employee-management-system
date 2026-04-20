package com.group5.ems.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "employee_code", unique = true, length = 50)
    private String employeeCode;

    @Column(name = "department_id", nullable = true)
    private Long departmentId;

    @Column(name = "position_id", nullable = true)
    private Long positionId;

    @Column(name = "previous_position_id")
    private Long previousPositionId;

    @Column(name = "promotion_date")
    private LocalDate promotionDate;

    @Column(name = "line_manager_id")
    private Long lineManagerId;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(length = 30)
    private String status = "ACTIVE"; // ACTIVE, ON_LEAVE, TERMINATED

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 255)
    private String terminationReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "department_id", insertable = false, updatable = false, nullable = true)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "position_id", insertable = false, updatable = false, nullable = true)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_manager_id", insertable = false, updatable = false)
    private Employee lineManager;

    @OneToMany(mappedBy = "lineManager")
    private List<Employee> subordinates = new ArrayList<>();

    @OneToMany(mappedBy = "manager")
    private List<Department> managedDepartments = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contract> contracts = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Salary> salaries = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bonus> bonuses = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeBenefit> employeeBenefits = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Request> requests = new ArrayList<>();


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
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public Long getPreviousPositionId() { return previousPositionId; }
    public void setPreviousPositionId(Long previousPositionId) { this.previousPositionId = previousPositionId; }
    public LocalDate getPromotionDate() { return promotionDate; }
    public void setPromotionDate(LocalDate promotionDate) { this.promotionDate = promotionDate; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public Long getLineManagerId() { return lineManagerId; }
    public void setLineManagerId(Long lineManagerId) { this.lineManagerId = lineManagerId; }
    public Employee getLineManager() { return lineManager; }
    public void setLineManager(Employee lineManager) { this.lineManager = lineManager; }
    public List<Employee> getSubordinates() { return subordinates; }
    public void setSubordinates(List<Employee> subordinates) { this.subordinates = subordinates; }
    public List<Department> getManagedDepartments() { return managedDepartments; }
    public void setManagedDepartments(List<Department> managedDepartments) { this.managedDepartments = managedDepartments; }
    public List<Contract> getContracts() { return contracts; }
    public void setContracts(List<Contract> contracts) { this.contracts = contracts; }
    public List<Attendance> getAttendances() { return attendances; }
    public void setAttendances(List<Attendance> attendances) { this.attendances = attendances; }
    public List<Salary> getSalaries() { return salaries; }
    public void setSalaries(List<Salary> salaries) { this.salaries = salaries; }
    public List<Bonus> getBonuses() { return bonuses; }
    public void setBonuses(List<Bonus> bonuses) { this.bonuses = bonuses; }
    public List<EmployeeBenefit> getEmployeeBenefits() { return employeeBenefits; }
    public void setEmployeeBenefits(List<EmployeeBenefit> employeeBenefits) { this.employeeBenefits = employeeBenefits; }
    public List<Request> getRequests() { return requests; }
    public void setRequests(List<Request> requests) { this.requests = requests; }
}

