package com.zzy.service;

import com.zzy.domain.dto.InvoiceView;
import com.zzy.domain.dto.SummaryView;
import com.zzy.domain.entity.InvoiceEntity;
import com.zzy.domain.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceQueryService {

    private final InvoiceRepository repo;

    public InvoiceQueryService(InvoiceRepository repo) {
        this.repo = repo;
    }

    public Page<InvoiceView> pageViews(int page, int size, String status, String q) {
        var pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> entityPage = repo.pageEntities(status, q, pageable);

        LocalDate today = LocalDate.now();

        List<InvoiceView> views = entityPage.getContent().stream()
                .map(i -> {
                    boolean overdue = isOverdue(i, today);
                    long daysUntilDue = calcDaysUntilDue(i, today);
                    String bucket = computeBucket(i, today);        // NOT_OVERDUE / 0-7 / 8-30 / 31-60 / 61-90 / >90

                    return new InvoiceView(
                            i.getId(),
                            i.getQboId(),
                            i.getCustomerName(),
                            i.getStatus(),
                            i.getTotalAmt(),
                            i.getBalance(),
                            i.getTxnDate(),
                            i.getDueDate(),
                            overdue,
                            (int) daysUntilDue,
                            bucket
                    );
                })
                .toList();

        return new PageImpl<>(views, pageable, entityPage.getTotalElements());
    }

    /** Aging view by（count、sum(balance)）。 */
    public Map<String, Object> aging() {
        LocalDate today = LocalDate.now();

        List<InvoiceEntity> rows = repo.findByBalanceGreaterThan(BigDecimal.ZERO);

        // buckets：NOT_OVERDUE / 0-7 / 8-30 / 31-60 / >60
        Map<String, List<InvoiceEntity>> grouped = rows.stream()
                .collect(Collectors.groupingBy(i -> computeBucket(i, today),
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, Object> res = new LinkedHashMap<>();
        for (var e : grouped.entrySet()) {
            long cnt = e.getValue().size();
            BigDecimal amt = e.getValue().stream()
                    .map(InvoiceEntity::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> v = new LinkedHashMap<>();
            v.put("count", cnt);
            v.put("amount", amt);
            res.put(e.getKey(), v);
        }
        return res;
    }

    /** overview cards */
    public SummaryView summary() {
        long openCount = 0, partialCount = 0, paidCount = 0;
        BigDecimal openAmt = BigDecimal.ZERO, partialAmt = BigDecimal.ZERO, paidAmt = BigDecimal.ZERO;

        List<Object[]> rows = repo.aggregateByStatus();
        for (Object[] r : rows) {
            String status = (String) r[0];
            long cnt = ((Number) r[1]).longValue();
            BigDecimal amt = (BigDecimal) r[2];

            if (status == null) continue;
            switch (status.toUpperCase()) {
                case "OPEN" -> { openCount += cnt; openAmt = openAmt.add(amt); }
                case "PARTIAL_PAID" -> { partialCount += cnt; partialAmt = partialAmt.add(amt); }
                case "PAID" -> { paidCount += cnt; paidAmt = paidAmt.add(amt); }
                default -> {}
            }
        }

        long totalCount = openCount + partialCount + paidCount;
        BigDecimal totalAmount = openAmt.add(partialAmt).add(paidAmt);

        long overdueCount = repo.overdueCountDerived();

        return new SummaryView(
                totalCount,
                openCount, partialCount, paidCount,
                overdueCount,
                totalAmount,
                openAmt, partialAmt, paidAmt
        );
    }

    private static boolean isOverdue(InvoiceEntity i, LocalDate today) {
        if (i.getBalance() == null || i.getBalance().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (i.getDueDate() == null) return false;
        return i.getDueDate().isBefore(today);
    }

    private static long calcDaysUntilDue(InvoiceEntity i, LocalDate today) {
        if (i.getDueDate() == null) return 0;
        return ChronoUnit.DAYS.between(today, i.getDueDate());
    }

    /**
     * Aging buckets：
     * - NOT_OVERDUE（ including balance<=0 or dueDate>=today）
     * - 0-7 / 8-30 / 31-60 / 61-90 / >90：
     */
    private static String computeBucket(InvoiceEntity i, LocalDate today) {
        if (!isOverdue(i, today)) return "NOT_OVERDUE";
        long overdueDays = ChronoUnit.DAYS.between(i.getDueDate(), today);
        if (overdueDays > 90) return ">90";
        if (overdueDays > 60) return "61-90";
        if (overdueDays > 30) return "31-60";
        if (overdueDays > 7)  return "8-30";
        return "0-7";
    }

    public Map<String, Object> overdueAging() {
        LocalDate today = LocalDate.now();
        List<InvoiceEntity> rows = repo.findOverdueCandidates();

        List<InvoiceEntity> overdue = rows.stream()
                .filter(i -> isOverdue(i, today))
                .toList();

        // computeBucket buckets（0-7 / 8-30 / 31-60 / 61-90 / >90）
        Map<String, List<InvoiceEntity>> grouped = overdue.stream()
                .collect(Collectors.groupingBy(i -> computeBucket(i, today),
                        LinkedHashMap::new, Collectors.toList()));

        List<String> buckets = List.of("0-7","8-30","31-60","61-90",">90");
        Map<String, Object> res = new LinkedHashMap<>();
        for (String b : buckets) {
            List<InvoiceEntity> list = grouped.getOrDefault(b, List.of());
            long cnt = list.size();
            BigDecimal amt = list.stream()
                    .map(InvoiceEntity::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("count", cnt);
            v.put("amount", amt);
            res.put(b, v);
        }
        return res;
    }

    public Page<InvoiceView> pageOverdue(int page, int size, String bucket, String q) {
        LocalDate today = LocalDate.now();

        List<InvoiceEntity> candidates = repo.findOverdueCandidates();

        List<InvoiceView> all = candidates.stream()
                .filter(i -> isOverdue(i, today))
                .filter(i -> q == null || q.isBlank()
                        || (i.getCustomerName() != null
                        && i.getCustomerName().toLowerCase().contains(q.toLowerCase())))
                .map(i -> new AbstractMap.SimpleEntry<>(i, computeBucket(i, today)))
                .filter(e -> bucket == null || bucket.isBlank() || bucket.equals(e.getValue()))
                .map(e -> {
                    InvoiceEntity i = e.getKey();
                    boolean overdue = true;
                    long daysUntilDue = calcDaysUntilDue(i, today); // 逾期会是 <=0
                    String bkt = e.getValue();
                    return new InvoiceView(
                            i.getId(),
                            i.getQboId(),
                            i.getCustomerName(),
                            i.getStatus(),
                            i.getTotalAmt(),
                            i.getBalance(),
                            i.getTxnDate(),
                            i.getDueDate(),
                            overdue,
                            (int) daysUntilDue,
                            bkt
                    );
                })
                .sorted(Comparator
                        .comparing(InvoiceView::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(InvoiceView::id))
                .toList();

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<InvoiceView> slice = all.subList(from, to);

        return new PageImpl<>(slice, PageRequest.of(page, size), all.size());
    }
}
