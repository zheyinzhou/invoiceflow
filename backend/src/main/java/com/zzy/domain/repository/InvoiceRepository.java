package com.zzy.domain.repository;

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
     * aggregateByStatus
     */
    @Query("select i.status, count(i), coalesce(sum(i.balance),0) from InvoiceEntity i group by i.status")
    List<Object[]> aggregateByStatus();

    /**
     * overdue candidates：balance>0 and not dueDate
     */
    @Query("""
        select i from InvoiceEntity i
        where i.balance > 0 and i.dueDate is not null
    """)
    List<InvoiceEntity> findOverdueCandidates();

    /**
     * Aging buckets
     */
    List<InvoiceEntity> findByBalanceGreaterThan(BigDecimal minBalance);

    /** overdue counter */
    @Query("""
            select count(i) from InvoiceEntity i
            where i.balance > 0 and i.dueDate < current_date
            """)
    long overdueCountDerived();

    Optional<InvoiceEntity> findByQboId(String qboId);
}
