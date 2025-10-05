package com.zzy.service;

import com.zzy.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KpiService {
    private final InvoiceRepository repo;

    public record OverdueBucket(java.time.LocalDate bucketDate,
                                java.math.BigDecimal amount,
                                long count) {}

    /** 把“今天仍逾期”的票，按到期日分组：gran=day|week */
    public java.util.List<OverdueBucket> overdueByDueDate(java.time.LocalDate from,
                                                          java.time.LocalDate to,
                                                          String gran) {
        var today = java.time.LocalDate.now();
        var rows = repo.findOverdueCandidates(); // 你已有：balance>0 && dueDate not null

        // 仅保留“今天仍逾期”的
        var overdue = rows.stream()
                .filter(i -> i.getDueDate() != null && i.getDueDate().isBefore(today))
                .filter(i -> i.getBalance() != null && i.getBalance().signum() > 0)
                .toList();

        // 归一化到“天”或“周一”
        java.util.function.Function<java.time.LocalDate, java.time.LocalDate> bucketFn =
                "week".equalsIgnoreCase(gran)
                        ? d -> d.with(java.time.DayOfWeek.MONDAY)
                        : d -> d;

        // 只取时间窗口内
        var filtered = overdue.stream()
                .filter(i -> {
                    var d = i.getDueDate();
                    return (d.isEqual(from) || d.isAfter(from)) && (d.isEqual(to) || d.isBefore(to));
                });

        // 分组聚合
        var grouped = new java.util.LinkedHashMap<java.time.LocalDate,
                java.util.concurrent.atomic.AtomicReference<java.math.BigDecimal>>();
        var counts  = new java.util.LinkedHashMap<java.time.LocalDate, java.util.concurrent.atomic.AtomicLong>();

        filtered.forEach(i -> {
            var k = bucketFn.apply(i.getDueDate());
            grouped.computeIfAbsent(k, __ -> new java.util.concurrent.atomic.AtomicReference<>(java.math.BigDecimal.ZERO))
                    .updateAndGet(v -> v.add(nz(i.getBalance())));
            counts.computeIfAbsent(k, __ -> new java.util.concurrent.atomic.AtomicLong(0)).incrementAndGet();
        });

        // 出结果（按时间排序）
        return grouped.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> new OverdueBucket(
                        e.getKey(),
                        e.getValue().get(),
                        counts.get(e.getKey()).get()
                ))
                .toList();
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
