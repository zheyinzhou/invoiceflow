package com.zzy.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "invoices")
public class InvoiceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qbo_id", unique = true)
    private String qboId;

    private String customerName;
    private String status;
    private BigDecimal totalAmt;
    private BigDecimal balance;
    private LocalDate txnDate;
    private LocalDate dueDate;
}