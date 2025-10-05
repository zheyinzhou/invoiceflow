package com.zzy.domain.dto;

import java.math.BigDecimal;

public class CreateInvoiceReq {
    public BigDecimal amount;        // required > 0
    public String customerName;
    public String itemName;
    public Integer daysUntilDue;     // default 14
    public String note;
}
