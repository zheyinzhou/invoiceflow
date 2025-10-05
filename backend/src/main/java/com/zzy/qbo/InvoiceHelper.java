package com.zzy.qbo;

import com.intuit.ipp.data.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class InvoiceHelper {

    public static Invoice buildInvoice(ReferenceType custRef,
                                       ReferenceType itemRef,
                                       BigDecimal amount,
                                       Integer daysUntilDue,
                                       String note) {
        LocalDate today = LocalDate.now();
        int days = (daysUntilDue == null) ? 14 : Math.max(0, daysUntilDue);
        LocalDate due = today.plusDays(days);

        Invoice inv = new Invoice();
        inv.setCustomerRef(custRef);
        inv.setTxnDate(toDate(today));
        inv.setDueDate(toDate(due));

        List<Line> lines = List.of(buildSalesLine(itemRef, BigDecimal.ONE, money(amount), note));
        inv.setLine(lines);

        if (note != null && !note.isBlank()) {
            inv.setPrivateNote(note);
        }
        return inv;
    }

    private static Line buildSalesLine(ReferenceType itemRef, BigDecimal qty, BigDecimal unitPrice, String desc) {
        SalesItemLineDetail sld = new SalesItemLineDetail();
        sld.setItemRef(itemRef);
        sld.setQty(qty);
        sld.setUnitPrice(unitPrice);
        // no tax
        // setNoTax(sld);

        Line line = new Line();
        line.setDetailType(LineDetailTypeEnum.SALES_ITEM_LINE_DETAIL);
        line.setSalesItemLineDetail(sld);
        line.setAmount(money(unitPrice.multiply(qty)));
        if (desc != null && !desc.isBlank()) {
            line.setDescription(desc);
        }
        return line;
    }

    public static String esc(String s){ return s.replace("'", "''"); }

    public static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    @SuppressWarnings("unused")
    private static void setNoTax(SalesItemLineDetail sld) {
        ReferenceType tax = new ReferenceType();
        tax.setValue("NON");
        sld.setTaxCodeRef(tax);
    }
}
