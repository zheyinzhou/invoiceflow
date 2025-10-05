package com.zzy.domain.repository;

import com.zzy.domain.dto.InvoiceView;
import com.zzy.domain.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    /**
     * 仅做基础过滤与排序，返回 Entity，后续在 Service 做派生字段计算。
     */
    @Query("""
        select i from InvoiceEntity i
        where (:status is null or :status = '' or i.status = :status)
          and (:q is null or :q = '' or lower(i.customerName) like lower(concat('%', :q, '%')))
        order by i.id desc
    """)
    Page<InvoiceEntity> pageEntities(@Param("status") String status,
                                     @Param("q") String q,
                                     Pageable pageable);

    /**
     * 状态聚合：保留（它不依赖时间函数）。
     */
    @Query("select i.status, count(i), coalesce(sum(i.balance),0) from InvoiceEntity i group by i.status")
    List<Object[]> aggregateByStatus();

    /**
     * 仅拿可能逾期的候选（减少全表扫描量）：balance>0 且 dueDate 非空。
     */
    @Query("""
        select i from InvoiceEntity i
        where i.balance > 0 and i.dueDate is not null
    """)
    List<InvoiceEntity> findOverdueCandidates();

    /**
     * Aging 分桶也在 Java 做，这里保留一个取 balance>0 的集合接口，按需使用。
     */
    List<InvoiceEntity> findByBalanceGreaterThan(BigDecimal minBalance);

    /** 逾期计数 */
    @Query("""
            select count(i) from InvoiceEntity i
            where i.balance > 0 and i.dueDate < current_date
            """)
    long overdueCountDerived();

    Optional<InvoiceEntity> findByQboId(String qboId);
}
