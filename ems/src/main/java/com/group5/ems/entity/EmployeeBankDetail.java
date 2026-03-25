package com.group5.ems.entity;

import com.group5.ems.config.CryptoConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "employee_bank_details")
public class EmployeeBankDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Size(max = 150)
    @NotNull
    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Size(max = 150)
    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Size(max = 150)
    @NotNull
    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @NotNull
    @Column(name = "account_number", nullable = false, length = 255)
    @Convert(converter = CryptoConverter.class)
    private String accountNumber;

    @ColumnDefault("1")
    @Column(name = "is_primary")
    private Boolean isPrimary;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}