package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service @RequiredArgsConstructor @Transactional
public class PaymentService {
    private final PaymentRepository paymentRepo;
    private final InvoiceRepository invoiceRepo;
    private final SettingsService settingsService;

    public List<Map<String,Object>> getAll() {
        return paymentRepo.findByOrderByPaymentDateDescIdDesc().stream().map(this::toMap).collect(Collectors.toList());
    }

    public List<Map<String,Object>> byInvoice(Long invoiceId) {
        return paymentRepo.findByInvoiceId(invoiceId).stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> save(Map<String,Object> body) {
        Long invoiceId = toLong(body.get("invoiceId"));
        InvoiceEntity inv = (invoiceId != null) ? invoiceRepo.findById(invoiceId).orElse(null) : null;
        PaymentEntity p = new PaymentEntity();
        p.setInvoice(inv); p.setAmount(toDouble(body.get("amount")));
        p.setPaymentDate(LocalDate.parse((String)body.get("paymentDate")));
        p.setMethod((String)body.getOrDefault("method","CASH"));
        p.setReference((String)body.getOrDefault("reference",""));
        p.setNotes((String)body.getOrDefault("notes",""));
        p.setCreatedAt(LocalDateTime.now());
        PaymentEntity saved = paymentRepo.save(p);
        if (inv != null) {
            updateInvoiceStatus(inv);
            log.info("Payment recorded: id={} invoice={} amount={} method={} invoiceStatus={}",
                saved.getId(), inv.getInvoiceNumber(), saved.getAmount(), saved.getMethod(), inv.getStatus());
        } else {
            log.info("Payment recorded: id={} amount={} method={} (no invoice)",
                saved.getId(), saved.getAmount(), saved.getMethod());
        }
        return toMap(saved);
    }

    public void delete(Long id) {
        paymentRepo.findById(id).ifPresent(p -> {
            InvoiceEntity inv = p.getInvoice();
            if (inv != null) {
                log.info("Payment deleted: id={} invoice={} amount={}", id, inv.getInvoiceNumber(), p.getAmount());
                paymentRepo.deleteById(id);
                updateInvoiceStatus(inv);
                log.info("Invoice status after payment deletion: invoice={} newStatus={}", inv.getInvoiceNumber(), inv.getStatus());
            } else {
                log.info("Payment deleted: id={} amount={} (no invoice)", id, p.getAmount());
                paymentRepo.deleteById(id);
            }
        });
    }

    private void updateInvoiceStatus(InvoiceEntity inv) {
        double paid = paymentRepo.findByInvoiceId(inv.getId()).stream().mapToDouble(PaymentEntity::getAmount).sum();
        inv.setPaidAmount(paid);
        if (paid >= inv.getTotalAmount()) inv.setStatus("PAID");
        else if (paid > 0) inv.setStatus("PARTIAL");
        else inv.setStatus("UNPAID");
        invoiceRepo.save(inv);
    }

    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] d = ExcelExportUtil.exportPayments(paymentRepo.findByOrderByPaymentDateDescIdDesc());
            log.debug("Payments exported to Excel");
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename=payments.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(d);
        } catch (Exception e) {
            log.error("Payment Excel export failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try {
            byte[] d = PdfExportUtil.exportPayments(paymentRepo.findByOrderByPaymentDateDescIdDesc(), settingsService.get().getBusinessName());
            log.debug("Payments exported to PDF");
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename=payments.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(d);
        } catch (Exception e) {
            log.error("Payment PDF export failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Map<String,Object> toMap(PaymentEntity p) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("invoiceId", p.getInvoice() != null ? p.getInvoice().getId() : null);
        m.put("invoiceNumber", p.getInvoice() != null ? p.getInvoice().getInvoiceNumber() : "");
        m.put("customerName", p.getInvoice() != null && p.getInvoice().getCustomer() != null ? p.getInvoice().getCustomer().getName() : "");
        m.put("amount", p.getAmount());
        m.put("paymentDate", p.getPaymentDate());
        m.put("method", p.getMethod());
        m.put("reference", nvl(p.getReference()));
        m.put("notes", nvl(p.getNotes()));
        return m;
    }

    double toDouble(Object o) { if(o instanceof Number) return ((Number)o).doubleValue(); try{return Double.parseDouble(o.toString());}catch(Exception e){return 0;} }
    Long toLong(Object o) { if(o instanceof Number) return ((Number)o).longValue(); try{return Long.parseLong(o.toString());}catch(Exception e){return null;} }
    String nvl(String s) { return s==null?"":s; }
}
