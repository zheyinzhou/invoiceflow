package com.zzy.domain.dto;

import java.math.BigDecimal;

public record CustomerRiskDTO(
        String customer,            // TODO: need customer ID
        int invoices,
        BigDecimal total,
        BigDecimal overdue,
        int maxDpd,
        double ratio                // overdue / total (0..1)
) {}
