package com.zzy.domain.dto;

import java.math.BigDecimal;

public class CreateInvoiceReq {
    public BigDecimal amount;        // 必填 > 0
    public String customerName;      // 可选：不传则自动挑一个
    public String itemName;          // 可选：不传则自动挑一个服务型 item
    public Integer daysUntilDue;     // 默认 14
    public String note;
}
