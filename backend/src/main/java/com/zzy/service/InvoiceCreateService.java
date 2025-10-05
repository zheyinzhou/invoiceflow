package com.zzy.service;

import com.intuit.ipp.data.*;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.zzy.domain.dto.CreateInvoiceReq;
import com.zzy.domain.entity.InvoiceEntity;
import com.zzy.domain.repository.InvoiceRepository;
import com.zzy.qbo.QboDataServiceFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.zzy.qbo.InvoiceHelper.*;

//not use here since don't sync invoice to sandbox
@Service
public class InvoiceCreateService {

    private final QboDataServiceFactory factory;
    private final InvoiceRepository repo;

    public InvoiceCreateService(QboDataServiceFactory factory, InvoiceRepository repo) {
        this.factory = factory;
        this.repo = repo;
    }

    @Transactional
    public InvoiceEntity create(CreateInvoiceReq req) throws Exception {
        if (req.amount == null || req.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        DataService ds = factory.get();
        ReferenceType custRef = resolveCustomerRef(ds, req.customerName);
        ReferenceType itemRef = resolveItemRef(ds, req.itemName);

        Invoice inv = buildInvoice(custRef, itemRef, req.amount, req.daysUntilDue, req.note);
        Invoice created = ds.add(inv);

        InvoiceEntity e = new InvoiceEntity();
        e.setQboId(created.getId());
        e.setCustomerName(created.getCustomerRef() == null ? null : created.getCustomerRef().getName());
        e.setTotalAmt(created.getTotalAmt() != null ? created.getTotalAmt() : req.amount);
        e.setBalance(created.getBalance()  != null ? created.getBalance()  : req.amount);
        e.setTxnDate(toLocalDate(created.getTxnDate()));
        e.setDueDate(toLocalDate(created.getDueDate()));
        e.setStatus("OPEN");
        return repo.save(e);
    }

    private ReferenceType resolveCustomerRef(DataService ds, String name) throws FMSException {
        String q = (name != null && !name.isBlank())
                ? "select Id, DisplayName from Customer where Active=true and DisplayName='"+esc(name)+"'"
                : "select Id, DisplayName from Customer where Active=true startposition 1 maxresults 1";
        var qr = ds.executeQuery(q);
        if (!qr.getEntities().isEmpty()) {
            Customer c = (Customer) qr.getEntities().getFirst();
            ReferenceType r = new ReferenceType();
            r.setValue(c.getId());
            r.setName(c.getDisplayName());
            return r;
        }
        throw new FMSException("No active customer found");
    }

    private ReferenceType resolveItemRef(DataService ds, String name) throws FMSException {
        String q = (name != null && !name.isBlank())
                ? "select Id, Name from Item where Active=true and Name='"+esc(name)+"' and Type='Service'"
                : "select Id, Name from Item where Active=true and Type='Service' startposition 1 maxresults 1";
        var qr = ds.executeQuery(q);
        if (!qr.getEntities().isEmpty()) {
            Item it = (Item) qr.getEntities().getFirst();
            ReferenceType r = new ReferenceType();
            r.setValue(it.getId());
            r.setName(it.getName());
            return r;
        }
        throw new FMSException("No active service item found");
    }
}
