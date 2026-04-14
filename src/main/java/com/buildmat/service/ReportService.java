package com.buildmat.service;

import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service @RequiredArgsConstructor
public class ReportService {
    private final com.buildmat.dao.ReportQueryService qSvc;
    private final SettingsService settingsService;

    public Object salesSummary(String from, String to) {
        log.debug("Report: sales-summary from={} to={}", from, to);
        return qSvc.salesSummary(from, to);
    }

    public Object outstanding(String asOf) {
        log.debug("Report: outstanding asOf={}", asOf);
        return qSvc.outstanding(asOf);
    }

    public Object customerSales(String from, String to) {
        log.debug("Report: customer-sales from={} to={}", from, to);
        return qSvc.customerSales(from, to);
    }

    public Object productSales(String from, String to) {
        log.debug("Report: product-sales from={} to={}", from, to);
        return qSvc.productSales(from, to);
    }

    public Object gst(String from, String to) {
        log.debug("Report: gst from={} to={}", from, to);
        return qSvc.gst(from, to);
    }

    public Object paymentCollection(String from, String to) {
        log.debug("Report: payment-collection from={} to={}", from, to);
        return qSvc.paymentCollection(from, to);
    }

    public ResponseEntity<byte[]> exportExcel(String type, String from, String to, String asOf) {
        try {
            log.info("Report export Excel: type={} from={} to={} asOf={}", type, from, to, asOf);
            byte[] data = ExcelExportUtil.exportReport(type, from, to, asOf, qSvc);
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+type+".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(data);
        } catch (Exception e) {
            log.error("Report Excel export failed: type={} from={} to={}: {}", type, from, to, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<byte[]> exportPdf(String type, String from, String to, String asOf) {
        try {
            log.info("Report export PDF: type={} from={} to={} asOf={}", type, from, to, asOf);
            byte[] data = PdfExportUtil.exportReport(type, from, to, asOf, qSvc, settingsService.get().getBusinessName());
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+type+".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(data);
        } catch (Exception e) {
            log.error("Report PDF export failed: type={} from={} to={}: {}", type, from, to, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
