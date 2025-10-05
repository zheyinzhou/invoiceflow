package com.zzy.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceView(
        Long id,
        String qboId,
        String customerName,
        String status,
        BigDecimal totalAmt,
        BigDecimal balance,
        LocalDate txnDate,
        LocalDate dueDate,

        Boolean overdue,        // balance>0 && dueDate < today
        Integer daysUntilDue,
        String agingBucket      // 0–7 / 8–30 / 31–60 / >60 / NOT_OVERDUE
) {}
