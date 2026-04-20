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
public class PurchaseService {
    private final PurchaseRepository purchaseRepo;
    private final SupplierRepository supplierRepo;
    private final PurchasePaymentRepository paymentRepo;
    private final SettingsService settingsService;

    // ── Purchase CRUD ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String,Object> save(Long id, Map<String,Object> body) {
        PurchaseEntity po = id != null ? purchaseRepo.findById(id).orElseThrow() : new PurchaseEntity();
        Long supplierId = toLong(body.get("supplierId"));
        if (supplierId != null) po.setSupplier(supplierRepo.findById(supplierId).orElse(null));
        po.setPurchaseDate(LocalDate.parse((String) body.get("purchaseDate")));
        if (body.get("dueDate") != null && !body.get("dueDate").toString().isBlank())
            po.setDueDate(LocalDate.parse((String) body.get("dueDate")));
        po.setSupplierInvoiceRef((String) body.getOrDefault("supplierInvoiceRef", ""));
        po.setIncludeGst((Boolean) body.getOrDefault("includeGst", true));
        po.setNotes((String) body.getOrDefault("notes", ""));
        if (po.getPurchaseNumber() == null || po.getPurchaseNumber().isBlank())
            po.setPurchaseNumber(generateNumber());
        if (po.getCreatedAt() == null) po.setCreatedAt(LocalDateTime.now());

        po.getItems().clear();
        List<Map<String,Object>> items = (List<Map<String,Object>>) body.get("items");
        if (items != null) {
            for (Map<String,Object> it : items) {
                PurchaseItemEntity item = new PurchaseItemEntity();
                item.setPurchase(po);
                item.setProductId(toLong(it.get("productId")));
                item.setProductName((String) it.get("productName"));
                item.setUnit((String) it.getOrDefault("unit", ""));
                item.setQuantity(toDouble(it.get("quantity")));
                item.setUnitPrice(toDouble(it.get("unitPrice")));
                item.setTotal(item.getQuantity() * item.getUnitPrice());
                item.setSgstPercent(toDouble(it.getOrDefault("sgstPercent", 0)));
                item.setCgstPercent(toDouble(it.getOrDefault("cgstPercent", 0)));
                po.getItems().add(item);
            }
        }
        recalculate(po);
        PurchaseEntity saved = purchaseRepo.save(po);
        log.info("Purchase {}: number={} supplier='{}' total={} status={}",
            id == null ? "created" : "updated", saved.getPurchaseNumber(),
            saved.getSupplier() != null ? saved.getSupplier().getName() : "N/A",
            saved.getTotalAmount(), saved.getStatus());
        return toMap(saved);
    }

    public List<Map<String,Object>> getAll(String q) {
        List<PurchaseEntity> list = (q == null || q.isBlank())
            ? purchaseRepo.findByOrderByPurchaseDateDescIdDesc()
            : purchaseRepo.findByPurchaseNumberContainingIgnoreCaseOrSupplierNameContainingIgnoreCaseOrStatusContainingIgnoreCase(q, q, q);
        return list.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> getById(Long id) {
        return purchaseRepo.findById(id).map(this::toFullMap).orElseThrow();
    }

    public void delete(Long id) {
        log.info("Purchase deleted: id={}", id);
        purchaseRepo.deleteById(id);
    }

    // ── Purchase Payments ──────────────────────────────────────────────────────

    public List<Map<String,Object>> getPayments(Long purchaseId) {
        return paymentRepo.findByPurchaseId(purchaseId).stream().map(this::paymentToMap).collect(Collectors.toList());
    }

    public Map<String,Object> addPayment(Long purchaseId, Map<String,Object> body) {
        PurchaseEntity po = purchaseRepo.findById(purchaseId).orElseThrow();
        PurchasePaymentEntity p = new PurchasePaymentEntity();
        p.setPurchase(po);
        p.setAmount(toDouble(body.get("amount")));
        p.setPaymentDate(LocalDate.parse((String) body.get("paymentDate")));
        p.setMethod((String) body.getOrDefault("method", "CASH"));
        p.setReference((String) body.getOrDefault("reference", ""));
        p.setNotes((String) body.getOrDefault("notes", ""));
        p.setCreatedAt(LocalDateTime.now());
        PurchasePaymentEntity saved = paymentRepo.save(p);
        updatePurchaseStatus(po);
        log.info("Purchase payment recorded: id={} purchase={} amount={} method={}",
            saved.getId(), po.getPurchaseNumber(), saved.getAmount(), saved.getMethod());
        return paymentToMap(saved);
    }

    public void deletePayment(Long paymentId) {
        paymentRepo.findById(paymentId).ifPresent(p -> {
            PurchaseEntity po = p.getPurchase();
            paymentRepo.deleteById(paymentId);
            if (po != null) updatePurchaseStatus(po);
            log.info("Purchase payment deleted: id={}", paymentId);
        });
    }

    // ── Export ─────────────────────────────────────────────────────────────────

    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] d = ExcelExportUtil.exportPurchases(purchaseRepo.findByOrderByPurchaseDateDescIdDesc());
            return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=purchases.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(d);
        } catch (Exception e) {
            log.error("Purchase Excel export failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try {
            byte[] d = PdfExportUtil.exportPurchases(purchaseRepo.findByOrderByPurchaseDateDescIdDesc(), settingsService.get().getBusinessName());
            return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=purchases.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(d);
        } catch (Exception e) {
            log.error("Purchase PDF export failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void recalculate(PurchaseEntity po) {
        double sub = po.getItems().stream().mapToDouble(PurchaseItemEntity::getTotal).sum();
        po.setSubtotal(sub);
        if (Boolean.TRUE.equals(po.getIncludeGst())) {
            double sgst = po.getItems().stream().mapToDouble(i -> i.getTotal() * i.getSgstPercent() / 100).sum();
            double cgst = po.getItems().stream().mapToDouble(i -> i.getTotal() * i.getCgstPercent() / 100).sum();
            po.setSgstAmount(sgst); po.setCgstAmount(cgst); po.setTaxAmount(sgst + cgst);
        } else { po.setSgstAmount(0.0); po.setCgstAmount(0.0); po.setTaxAmount(0.0); }
        po.setTotalAmount(sub + po.getTaxAmount());
        double paid = paymentRepo.findByPurchaseId(po.getId() != null ? po.getId() : 0L)
            .stream().mapToDouble(PurchasePaymentEntity::getAmount).sum();
        po.setPaidAmount(paid);
        updateStatus(po, paid);
    }

    private void updatePurchaseStatus(PurchaseEntity po) {
        double paid = paymentRepo.findByPurchaseId(po.getId()).stream().mapToDouble(PurchasePaymentEntity::getAmount).sum();
        po.setPaidAmount(paid);
        updateStatus(po, paid);
        purchaseRepo.save(po);
    }

    private void updateStatus(PurchaseEntity po, double paid) {
        if (paid >= po.getTotalAmount() && po.getTotalAmount() > 0) po.setStatus("PAID");
        else if (paid > 0) po.setStatus("PARTIAL");
        else po.setStatus("UNPAID");
    }

    private String generateNumber() {
        String prefix = "PO-" + LocalDate.now().getYear() + "-";
        Optional<String> max = purchaseRepo.findMaxPurchaseNumber(prefix);
        int next = 1;
        if (max.isPresent()) {
            try { next = Integer.parseInt(max.get().substring(prefix.length())) + 1; } catch (Exception ignored) {}
        }
        return prefix + String.format("%04d", next);
    }

    private Map<String,Object> toMap(PurchaseEntity po) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", po.getId());
        m.put("purchaseNumber", po.getPurchaseNumber());
        m.put("supplierId", po.getSupplier() != null ? po.getSupplier().getId() : null);
        m.put("supplierName", po.getSupplier() != null ? po.getSupplier().getName() : "");
        m.put("supplierInvoiceRef", nvl(po.getSupplierInvoiceRef()));
        m.put("purchaseDate", po.getPurchaseDate());
        m.put("dueDate", po.getDueDate());
        m.put("subtotal", po.getSubtotal());
        m.put("sgstAmount", po.getSgstAmount());
        m.put("cgstAmount", po.getCgstAmount());
        m.put("taxAmount", po.getTaxAmount());
        m.put("totalAmount", po.getTotalAmount());
        m.put("paidAmount", po.getPaidAmount());
        m.put("balanceDue", po.getTotalAmount() - po.getPaidAmount());
        m.put("includeGst", po.getIncludeGst());
        m.put("status", po.getStatus());
        m.put("notes", po.getNotes());
        return m;
    }

    private Map<String,Object> toFullMap(PurchaseEntity po) {
        Map<String,Object> m = toMap(po);
        m.put("items", po.getItems().stream().map(item -> {
            Map<String,Object> i = new LinkedHashMap<>();
            i.put("id", item.getId());
            i.put("productId", nvlObj(item.getProductId()));
            i.put("productName", item.getProductName());
            i.put("unit", nvlObj(item.getUnit()));
            i.put("quantity", item.getQuantity());
            i.put("unitPrice", item.getUnitPrice());
            i.put("total", item.getTotal());
            i.put("sgstPercent", item.getSgstPercent());
            i.put("cgstPercent", item.getCgstPercent());
            return i;
        }).collect(Collectors.toList()));
        return m;
    }

    private Map<String,Object> paymentToMap(PurchasePaymentEntity p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("purchaseId", p.getPurchase() != null ? p.getPurchase().getId() : null);
        m.put("purchaseNumber", p.getPurchase() != null ? p.getPurchase().getPurchaseNumber() : "");
        m.put("supplierName", p.getPurchase() != null && p.getPurchase().getSupplier() != null ? p.getPurchase().getSupplier().getName() : "");
        m.put("amount", p.getAmount());
        m.put("paymentDate", p.getPaymentDate());
        m.put("method", p.getMethod());
        m.put("reference", nvl(p.getReference()));
        m.put("notes", nvl(p.getNotes()));
        return m;
    }

    double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    String nvl(String s) { return s == null ? "" : s; }
    Object nvlObj(Object o) { return o == null ? "" : o; }
}
