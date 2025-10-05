package com.zzy.web;

import com.zzy.domain.dto.InvoiceView;
import com.zzy.domain.dto.SummaryView;
import com.zzy.service.InvoiceQueryService;
import com.zzy.service.InvoiceSyncService;
import com.zzy.service.KpiService;
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

    // 同步保持原样
    @PostMapping("/admin/sync-qbo")
    public Map<String, Object> sync(@RequestParam(defaultValue = "200") int batch) throws Exception {
        var upserts = syncSvc.syncFromQbo(batch);
        return Map.of("upserts", upserts);
    }

    // 列表：原有
    @GetMapping("/invoices")
    public Page<InvoiceView> page(@RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String q) {
        return querySvc.pageViews(page, size, status, q);
    }

    // 通用 aging：原有
    @GetMapping("/invoices/aging")
    public Map<String, Object> aging() {
        return querySvc.aging();
    }

    // 总览：原有
    @GetMapping("/summary")
    public SummaryView invoiceSummary() {
        return querySvc.summary();
    }

    // ✅ 新增：逾期列表（点击 Overdue 卡片/某个桶时调用）
    // bucket 取值建议严格限定："0-7"|"8-30"|"31-60"|"61-90"|">90"
    @GetMapping("/invoices/overdue")
    public Page<InvoiceView> overduePage(@RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "10") @Min(1) @Max(200) int size,
                                         @RequestParam(required = false) String bucket,
                                         @RequestParam(required = false) String q) {
        return querySvc.pageOverdue(page, size, bucket, q);
    }

    // ✅ 新增：仅逾期的 aging 聚合（喂 Overdue 柱状图）
    @GetMapping("/invoices/aging/overdue")
    public Map<String, Object> overdueAging() {
        return querySvc.overdueAging();
    }
}
