package com.zzy.domain.dto;

public record CustomerRiskDTO(
        String customer,            // 先用 name；等你有 customer_id 再替换
        int invoices,
        java.math.BigDecimal total, // 合计开票额（这些逾期单）
        java.math.BigDecimal overdue,
        int maxDpd,                 // 最大逾期天数
        double ratio                // overdue / total (0..1)
) {}
