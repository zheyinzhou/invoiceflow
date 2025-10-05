package com.zzy.web;

import com.zzy.domain.dto.InvoiceView;
import com.zzy.domain.dto.PageResp;
import com.zzy.domain.dto.SummaryView;
import com.zzy.service.InvoiceQueryService;
import com.zzy.service.InvoiceSyncService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final InvoiceQueryService querySvc;
    private final InvoiceSyncService syncSvc;


    public InvoiceController(InvoiceQueryService querySvc, InvoiceSyncService syncSvc) {
        this.querySvc = querySvc;
        this.syncSvc = syncSvc;
    }

    @PostMapping("/admin/sync-qbo")
    public Map<String, Object> sync(@RequestParam(defaultValue = "200") int batch) throws Exception {
        var upserts = syncSvc.syncFromQbo(batch);
        return Map.of("upserts", upserts);
    }

    @GetMapping("/invoices")
    public PageResp<InvoiceView> page(@RequestParam(defaultValue = "0") @Min(0) int page,
                                      @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String q) {
        Page<InvoiceView> pages =  querySvc.pageViews(page, size, status, q);
        return PageResp.of(pages);
    }

    @GetMapping("/invoices/aging")
    public Map<String, Object> aging() {
        return querySvc.aging();
    }

    @GetMapping("/summary")
    public SummaryView invoiceSummary() {
        return querySvc.summary();
    }

    @GetMapping("/invoices/overdue")
    public PageResp<InvoiceView> overduePage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
                                         @RequestParam(required = false) String bucket,
                                         @RequestParam(required = false) String q) {
        Page<InvoiceView> pages = querySvc.pageOverdue(page, size, bucket, q);
        return PageResp.of(pages);
    }

    @GetMapping("/invoices/aging/overdue")
    public Map<String, Object> overdueAging() {
        return querySvc.overdueAging();
    }
}
