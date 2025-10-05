package com.zzy.domain.dto;

import java.math.BigDecimal;

/** 概览卡片 + aging 数据模型 */
public record SummaryView(
        long totalCount,
        long openCount,
        long partialCount,
        long paidCount,
        long overdueCount,
        BigDecimal totalAmount,
        BigDecimal openAmount,
        BigDecimal partialAmount,
        BigDecimal paidAmount
) {}
