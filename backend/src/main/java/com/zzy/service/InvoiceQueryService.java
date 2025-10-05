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

    /** 分页查询：仓库只返回实体；这里计算派生字段并映射为 InvoiceView。 */
    public Page<InvoiceView> pageViews(int page, int size, String status, String q) {
        var pageable = PageRequest.of(page, size);
        Page<InvoiceEntity> entityPage = repo.pageEntities(status, q, pageable);

        LocalDate today = LocalDate.now();

        List<InvoiceView> views = entityPage.getContent().stream()
                .map(i -> {
                    boolean overdue = isOverdue(i, today);
                    long daysUntilDue = calcDaysUntilDue(i, today); // 未来到期为正，逾期为负或0
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

    /** Aging 视图：全部在 Java 分桶统计（count、sum(balance)）。 */
    public Map<String, Object> aging() {
        LocalDate today = LocalDate.now();

        // 只拿 balance>0 的，减少计算量
        List<InvoiceEntity> rows = repo.findByBalanceGreaterThan(BigDecimal.ZERO);

        // 分桶：NOT_OVERDUE / 0-7 / 8-30 / 31-60 / >60
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

    /** 概览卡片：状态聚合 + 逾期计数在 Java 算（避免 current_date 方言差异）。 */
    public SummaryView summary() {
        long openCount = 0, partialCount = 0, paidCount = 0;
        BigDecimal openAmt = BigDecimal.ZERO, partialAmt = BigDecimal.ZERO, paidAmt = BigDecimal.ZERO;

        // 状态聚合（数据库按 status 分组，兼容性好）
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

//        // 逾期计数在 Java 侧算，避免 current_date 时区/方言问题
//        LocalDate today = LocalDate.now();
//        long overdueCount = repo.findOverdueCandidates().stream()
//                .filter(i -> isOverdue(i, today))
//                .count();
        long overdueCount = repo.overdueCountDerived();

        return new SummaryView(
                totalCount,
                openCount, partialCount, paidCount,
                overdueCount,
                totalAmount,
                openAmt, partialAmt, paidAmt
        );
    }

    /* ===================== 纯 Java 的派生字段计算 ===================== */

    private static boolean isOverdue(InvoiceEntity i, LocalDate today) {
        if (i.getBalance() == null || i.getBalance().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (i.getDueDate() == null) return false;
        return i.getDueDate().isBefore(today);
    }

    /**
     * 未来到期：正数；今天到期：0；已逾期：负数。
     */
    private static long calcDaysUntilDue(InvoiceEntity i, LocalDate today) {
        if (i.getDueDate() == null) return 0;
        return ChronoUnit.DAYS.between(today, i.getDueDate());
    }

    /**
     * Aging 分桶：
     * - NOT_OVERDUE：未逾期（含 balance<=0 或 dueDate>=today）
     * - 0-7 / 8-30 / 31-60 / 61-90 / >90：已逾期天数的分段（统一 ASCII 连字符）
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

    /** NEW: 逾期 aging（只返回已逾期分桶），与前端“Overdue”柱状图口径一致 */
    public Map<String, Object> overdueAging() {
        LocalDate today = LocalDate.now();
        // 只拿 balance>0 & dueDate 非空 的候选，减少计算量
        List<InvoiceEntity> rows = repo.findOverdueCandidates();

        // 先筛出真正逾期（dueDate < today）
        List<InvoiceEntity> overdue = rows.stream()
                .filter(i -> isOverdue(i, today))
                .toList();

        // 按 computeBucket 分桶（你现有口径：0-7 / 8-30 / 31-60 / 61-90 / >90）
        Map<String, List<InvoiceEntity>> grouped = overdue.stream()
                .collect(Collectors.groupingBy(i -> computeBucket(i, today),
                        LinkedHashMap::new, Collectors.toList()));

        // 固定桶顺序，缺桶返回 0（避免前端“柱子有/列表空”的错觉）
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

    /** NEW: 逾期列表查询（支持 bucket / q 搜索 / Java 端分页；排序按 dueDate asc, id asc） */
    public Page<InvoiceView> pageOverdue(int page, int size, String bucket, String q) {
        LocalDate today = LocalDate.now();

        // 候选：balance>0 & dueDate 非空（来自仓库的“瘦”接口）
        List<InvoiceEntity> candidates = repo.findOverdueCandidates();

        // 过滤：真正逾期 + （可选）bucket + （可选）搜索
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
                // 稳定排序：越早到期的越前；再按 id
                .sorted(Comparator
                        .comparing(InvoiceView::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(InvoiceView::id))
                .toList();

        // Java 端分页（逾期量通常远小于总量；若后期要 DB 分页，可换 Specification/JPQL）
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<InvoiceView> slice = all.subList(from, to);

        return new PageImpl<>(slice, PageRequest.of(page, size), all.size());
    }
}
