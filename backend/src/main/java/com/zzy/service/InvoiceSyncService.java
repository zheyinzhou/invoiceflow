package com.zzy.service;

import com.intuit.ipp.data.Invoice;
import com.intuit.ipp.services.QueryResult;
import com.zzy.domain.repository.InvoiceRepository;
import com.zzy.qbo.QboDataServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.zzy.domain.entity.InvoiceEntity;

import java.math.BigDecimal;
import java.time.ZoneId;

@Service
public class InvoiceSyncService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceSyncService.class);

    private final QboDataServiceFactory factory;
    private final InvoiceRepository repo;

    public InvoiceSyncService(QboDataServiceFactory factory, InvoiceRepository repo) {
        this.factory = factory; this.repo = repo;
    }

    /** 从 QBO 拉取 batch 条发票并 upsert 到 H2 */
    @Transactional
    public int syncFromQbo(int batch) throws Exception {
        var ds = factory.get();
//        String q = "select Id, TotalAmt, Balance, DueDate, TxnDate, TxnStatus, CustomerRef "
//                + "from Invoice startposition 1 maxresults " + Math.min(500, Math.max(1, batch));
        String sql = "select * from invoice";
        QueryResult queryResult = ds.executeQuery(sql);

        int count = queryResult.getEntities().size();

        log.info("Total number of invoices: " + count);

        int n=0;
        for (var e : queryResult.getEntities()) {
            var inv = (Invoice)e;
            var ent = repo.findByQboId(inv.getId()).orElseGet(InvoiceEntity::new);
            ent.setQboId(inv.getId());
            ent.setCustomerName(inv.getCustomerRef()==null? null : inv.getCustomerRef().getName());
            ent.setTotalAmt(inv.getTotalAmt());
            ent.setBalance(inv.getBalance());
            ent.setTxnDate(inv.getTxnDate() == null ? null : inv.getTxnDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            ent.setDueDate(inv.getDueDate() == null ? null : inv.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

            // 状态机
            BigDecimal total = inv.getTotalAmt()==null? BigDecimal.ZERO: inv.getTotalAmt();
            BigDecimal bal   = inv.getBalance()==null? BigDecimal.ZERO: inv.getBalance();
            String status;
            if ("VOID".equalsIgnoreCase(String.valueOf(inv.getTxnStatus()))) status="VOID";
            else if (total.signum()>0 && bal.signum()==0) status="PAID";
            else if (bal.compareTo(total)==0 && total.signum()>0) status="OPEN";
            else if (bal.signum()>0) status="PARTIAL_PAID";
            else status="UNKNOWN";
            ent.setStatus(status);

            repo.save(ent); n++;
        }
        return n;
    }
}
