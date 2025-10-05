package com.zzy.service;

import com.zzy.domain.dto.CustomerRiskDTO;
import com.zzy.domain.dto.RankMode;
import com.zzy.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskTopService {
    private final InvoiceRepository repo;

    public List<CustomerRiskDTO> topCustomers(RankMode mode, int top) {
        var today = java.time.LocalDate.now();
        var rows = repo.findOverdueCandidates(); // 你已有：balance>0 & dueDate not null

        var overdueRows = rows.stream()
                .filter(i -> i.getDueDate()!=null && i.getDueDate().isBefore(today))
                .filter(i -> i.getBalance()!=null && i.getBalance().signum()>0)
                .toList();

        // grouping by customers
        record Acc(int invoices, java.math.BigDecimal total, java.math.BigDecimal overdue, int maxDpd) {}
        var map = new java.util.HashMap<String, Acc>();
        for (var i : overdueRows) {
            String key = java.util.Optional.ofNullable(i.getCustomerName()).orElse("-").trim();
            var acc = map.getOrDefault(key, new Acc(0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0));

            var totalAmt = nz(i.getTotalAmt());
            var bal      = nz(i.getBalance());
            int dpd = (int)Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(i.getDueDate(), today));

            acc = new Acc(acc.invoices + 1,
                    acc.total.add(totalAmt),
                    acc.overdue.add(bal),
                    Math.max(acc.maxDpd, dpd));
            map.put(key, acc);
        }

        var list = new java.util.ArrayList<CustomerRiskDTO>();
        for (var e : map.entrySet()) {
            var a = e.getValue();
            double ratio = a.total.signum()==0 ? 0.0 :
                    a.overdue.divide(a.total, 6, java.math.RoundingMode.HALF_UP).doubleValue();
            list.add(new CustomerRiskDTO(e.getKey(), a.invoices, a.total, a.overdue, a.maxDpd, ratio));
        }

        list.sort(switch (mode) {
            case AMOUNT   -> java.util.Comparator.<CustomerRiskDTO, java.math.BigDecimal>comparing(CustomerRiskDTO::overdue).reversed();
            case MAX_DAYS -> java.util.Comparator.<CustomerRiskDTO, Integer>comparing(CustomerRiskDTO::maxDpd).reversed()
                    .thenComparing((CustomerRiskDTO r)-> r.overdue(), java.util.Comparator.reverseOrder());
            case RATIO    -> java.util.Comparator.<CustomerRiskDTO, Double>comparing(CustomerRiskDTO::ratio).reversed()
                    .thenComparing((CustomerRiskDTO r)-> r.overdue(), java.util.Comparator.reverseOrder());
        });

        // pick Top N
        return list.subList(0, Math.min(top, list.size()));
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v){
        return v==null ? java.math.BigDecimal.ZERO : v;
    }
}

