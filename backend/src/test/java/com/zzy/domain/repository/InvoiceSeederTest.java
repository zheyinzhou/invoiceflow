package com.zzy.domain.repository;

import com.zzy.domain.entity.InvoiceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Random;

//generate mock data to H2
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:~/invoiceflow-db/demo;AUTO_SERVER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
// 确保不回滚（以防你在类/方法上加了 @Transactional）
@Transactional
@Rollback(false)
@SpringBootTest
class InvoiceSeederTest {

    @Autowired
    InvoiceRepository repo;  // ← 别写成 repq

    // ===== parameters =====
    int TOTAL = 30;
    double R_OPEN = 0.40;         // OPEN rate
    double R_PARTIAL = 0.30;      // PARTIAL rate（0 < Balance < TotalAmt）
    double OVERDUE_RATE = 0.35;   // overdue_rate

    BigDecimal MIN_AMT = bd(5000);
    BigDecimal MAX_AMT = bd(200000);

    int MIN_TERM_DAYS = 7;
    int MAX_TERM_DAYS = 45;

    @Test
    void seed() {
        Random rnd = new Random();

        int openTarget    = (int) Math.round(TOTAL * R_OPEN);
        int partialTarget = (int) Math.round(TOTAL * R_PARTIAL);

        int open=0, partial=0, paid=0, overdue=0;

        for (int i = 0; i < TOTAL; i++) {
            BigDecimal total = money(randAmount(rnd, MIN_AMT, MAX_AMT));
            BigDecimal balance;
            String status;

            if (open < openTarget) {
                balance = total;          // OPEN
                status = "OPEN";
                open++;
            } else if (partial < partialTarget) {
                BigDecimal collected = money(total.multiply(bd(0.2 + rnd.nextDouble()*0.6)));
                balance = total.subtract(collected);
                if (balance.compareTo(BigDecimal.ZERO) <= 0) balance = bd(1);
                status = "PARTIAL_PAID";
                partial++;
            } else {
                balance = bd(0);          // PAID
                status = "PAID";
                paid++;
            }

            LocalDate issue = LocalDate.now().minusDays(rnd.nextInt(60));
            int term = MIN_TERM_DAYS + rnd.nextInt(MAX_TERM_DAYS - MIN_TERM_DAYS + 1);
            LocalDate due = issue.plusDays(term);

            boolean isOverdue = balance.compareTo(BigDecimal.ZERO) > 0 && due.isBefore(LocalDate.now());
            if (!isOverdue && balance.compareTo(BigDecimal.ZERO) > 0 && rnd.nextDouble() < OVERDUE_RATE) {
                due = LocalDate.now().minusDays(1 + rnd.nextInt(10));
                isOverdue = true;
            }

            InvoiceEntity e = new InvoiceEntity();
            e.setCustomerName("SeedCustomer-" + (10000 + i));
            e.setTotalAmt(total);
            e.setBalance(balance);
            e.setTxnDate(issue);
            e.setDueDate(due);
            e.setStatus(status);

            repo.save(e);
            if (isOverdue) overdue++;
        }

        System.out.printf("Seed done. total=%d, OPEN=%d, PARTIAL=%d, PAID=%d, OVERDUE=%d%n",
                TOTAL, open, partial, paid, overdue);
    }

    // ===== helpers =====
    private static BigDecimal randAmount(Random rnd, BigDecimal min, BigDecimal max) {
        double x = min.doubleValue() + rnd.nextDouble() * (max.doubleValue() - min.doubleValue());
        return BigDecimal.valueOf(x);
    }
    private static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal bd(double v) { return BigDecimal.valueOf(v); }
}
