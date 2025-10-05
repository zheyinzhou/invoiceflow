package com.zzy.web;

import com.zzy.domain.dto.CustomerRiskDTO;
import com.zzy.domain.dto.RankMode;
import com.zzy.service.KpiService;
import com.zzy.service.RiskTopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {
    private final RiskTopService svc;
    private final KpiService kpi;

    // /api/risk/customers?mode=amount|max_days|ratio&top=10
    @GetMapping("/customers")
    public List<CustomerRiskDTO> top(
            @RequestParam(defaultValue = "amount") String mode,
            @RequestParam(defaultValue = "10") int top
    ){
        var m = switch (mode.toLowerCase()) {
            case "amount"   -> RankMode.AMOUNT;
            case "max_days" -> RankMode.MAX_DAYS;
            case "ratio"    -> RankMode.RATIO;
            default -> throw new IllegalArgumentException("mode must be amount|max_days|ratio");
        };
        return svc.topCustomers(m, Math.max(1, Math.min(top, 100)));
    }

    @GetMapping("/kpi/overdue-by-due")
    public List<KpiService.OverdueBucket> overdueByDue(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "week") String gran
    ) {
        return kpi.overdueByDueDate(LocalDate.parse(from),
                LocalDate.parse(to),
                gran);
    }
}

