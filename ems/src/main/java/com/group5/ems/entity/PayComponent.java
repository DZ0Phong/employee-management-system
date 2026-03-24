package com.group5.ems.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "pay_components")
public class PayComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 30)
    @NotNull
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @ColumnDefault("1")
    @Column(name = "is_taxable")
    private Boolean isTaxable;

    @Size(max = 50)
    @ColumnDefault("'FLAT_AMOUNT'")
    @Column(name = "calculation_method", length = 50)
    private String calculationMethod;

    @Column(name = "default_value", precision = 15, scale = 2)
    private BigDecimal defaultValue;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;


}