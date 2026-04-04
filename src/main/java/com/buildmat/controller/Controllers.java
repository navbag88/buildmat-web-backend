package com.buildmat.controller;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.security.SecurityConfig;
import com.buildmat.service.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

// ═══ Health Controller ════════════════════════════════════════════════════════
@RestController
class HealthController {
    @GetMapping("/api/health")
    public ResponseEntity<?> health() { return ResponseEntity.ok(Map.of("status", "UP")); }
}

// ═══ Auth Controller ══════════════════════════════════════════════════════════
@RestController @RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("username"), body.get("password"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() { return authService.me(); }
}

// ═══ Dashboard Controller ═════════════════════════════════════════════════════
@RestController @RequestMapping("/api/dashboard")
@RequiredArgsConstructor
class DashboardController {
    private final DashboardService svc;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() { return ResponseEntity.ok(svc.getStats()); }
}

// ═══ Customer Controller ══════════════════════════════════════════════════════
@RestController @RequestMapping("/api/customers")
@RequiredArgsConstructor
class CustomerController {
    private final CustomerService svc;

    @GetMapping    public ResponseEntity<?> getAll(@RequestParam(required=false) String q) { return ResponseEntity.ok(svc.getAll(q)); }
    @GetMapping("/{id}") public ResponseEntity<?> getById(@PathVariable Long id) { return ResponseEntity.ok(svc.getById(id)); }
    @PostMapping   public ResponseEntity<?> create(@RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(null, body)); }
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(id, body)); }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id) { svc.delete(id); return ResponseEntity.ok(Map.of("ok",true)); }

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) { return ResponseEntity.ok(svc.importExcel(file)); }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() { return svc.importTemplate(); }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() { return svc.exportExcel(); }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() { return svc.exportPdf(); }
}

// ═══ Product Controller ═══════════════════════════════════════════════════════
@RestController @RequestMapping("/api/products")
@RequiredArgsConstructor
class ProductController {
    private final ProductService svc;

    @GetMapping    public ResponseEntity<?> getAll(@RequestParam(required=false) String q) { return ResponseEntity.ok(svc.getAll(q)); }
    @GetMapping("/{id}") public ResponseEntity<?> getById(@PathVariable Long id) { return ResponseEntity.ok(svc.getById(id)); }
    @PostMapping   public ResponseEntity<?> create(@RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(null, body)); }
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(id, body)); }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id) { svc.delete(id); return ResponseEntity.ok(Map.of("ok",true)); }

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) { return ResponseEntity.ok(svc.importExcel(file)); }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() { return svc.importTemplate(); }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() { return svc.exportExcel(); }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() { return svc.exportPdf(); }
}

// ═══ Invoice Controller ════════════════════════════════════════════════════════
@RestController @RequestMapping("/api/invoices")
@RequiredArgsConstructor
class InvoiceController {
    private final InvoiceService svc;

    @GetMapping    public ResponseEntity<?> getAll(@RequestParam(required=false) String q) { return ResponseEntity.ok(svc.getAll(q)); }
    @GetMapping("/{id}") public ResponseEntity<?> getById(@PathVariable Long id) { return ResponseEntity.ok(svc.getById(id)); }
    @PostMapping   public ResponseEntity<?> create(@RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(null, body)); }
    @PutMapping("/{id}") public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(id, body)); }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id) { svc.delete(id); return ResponseEntity.ok(Map.of("ok",true)); }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) { return svc.generatePdf(id); }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() { return svc.exportExcel(); }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() { return svc.exportPdf(); }
}

// ═══ Payment Controller ════════════════════════════════════════════════════════
@RestController @RequestMapping("/api/payments")
@RequiredArgsConstructor
class PaymentController {
    private final PaymentService svc;

    @GetMapping    public ResponseEntity<?> getAll() { return ResponseEntity.ok(svc.getAll()); }
    @GetMapping("/invoice/{invoiceId}") public ResponseEntity<?> byInvoice(@PathVariable Long invoiceId) { return ResponseEntity.ok(svc.byInvoice(invoiceId)); }
    @PostMapping   public ResponseEntity<?> create(@RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.save(body)); }
    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id) { svc.delete(id); return ResponseEntity.ok(Map.of("ok",true)); }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() { return svc.exportExcel(); }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() { return svc.exportPdf(); }
}

// ═══ Reports Controller ════════════════════════════════════════════════════════
@RestController @RequestMapping("/api/reports")
@RequiredArgsConstructor
class ReportController {
    private final ReportService svc;

    @GetMapping("/sales-summary")       public ResponseEntity<?> salesSummary(@RequestParam String from, @RequestParam String to) { return ResponseEntity.ok(svc.salesSummary(from,to)); }
    @GetMapping("/outstanding")         public ResponseEntity<?> outstanding(@RequestParam String asOf) { return ResponseEntity.ok(svc.outstanding(asOf)); }
    @GetMapping("/customer-sales")      public ResponseEntity<?> customerSales(@RequestParam String from, @RequestParam String to) { return ResponseEntity.ok(svc.customerSales(from,to)); }
    @GetMapping("/product-sales")       public ResponseEntity<?> productSales(@RequestParam String from, @RequestParam String to) { return ResponseEntity.ok(svc.productSales(from,to)); }
    @GetMapping("/gst")                 public ResponseEntity<?> gst(@RequestParam String from, @RequestParam String to) { return ResponseEntity.ok(svc.gst(from,to)); }
    @GetMapping("/payment-collection")  public ResponseEntity<?> paymentCollection(@RequestParam String from, @RequestParam String to) { return ResponseEntity.ok(svc.paymentCollection(from,to)); }

    @GetMapping("/{type}/export/excel") public ResponseEntity<byte[]> excel(@PathVariable String type, @RequestParam(required=false) String from, @RequestParam(required=false) String to, @RequestParam(required=false) String asOf) { return svc.exportExcel(type,from,to,asOf); }
    @GetMapping("/{type}/export/pdf")   public ResponseEntity<byte[]> pdf(@PathVariable String type, @RequestParam(required=false) String from, @RequestParam(required=false) String to, @RequestParam(required=false) String asOf) { return svc.exportPdf(type,from,to,asOf); }
}

// ═══ User Controller (Admin only) ═════════════════════════════════════════════
@RestController @RequestMapping("/api/users")
@RequiredArgsConstructor
class UserController {
    private final UserService svc;

    @GetMapping    public ResponseEntity<?> getAll() { return ResponseEntity.ok(svc.getAll()); }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String,Object> body,
                                    org.springframework.security.core.Authentication auth) {
        try { return ResponseEntity.ok(svc.create(body, callerRole(auth))); }
        catch (RuntimeException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String,Object> body,
                                    org.springframework.security.core.Authentication auth) {
        try { return ResponseEntity.ok(svc.update(id, body, callerRole(auth))); }
        catch (RuntimeException e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String,String> body) {
        svc.changePassword(id, body.get("password")); return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id) { svc.delete(id); return ResponseEntity.ok(Map.of("ok",true)); }

    private String callerRole(org.springframework.security.core.Authentication auth) {
        if (auth == null) return "USER";
        return auth.getAuthorities().stream().findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("USER");
    }
}

// ═══ Settings Controller ══════════════════════════════════════════════════════
@RestController @RequestMapping("/api/settings")
@RequiredArgsConstructor
class SettingsController {
    private final com.buildmat.service.SettingsService svc;

    @GetMapping
    public ResponseEntity<?> get() {
        com.buildmat.model.SettingsEntity s = svc.get();
        Map<String,Object> r = new java.util.LinkedHashMap<>();
        r.put("businessName", s.getBusinessName());
        r.put("tagLine",      s.getTagLine());
        r.put("gstNumber",    s.getGstNumber());
        r.put("phone",        s.getPhone());
        r.put("email",        s.getEmail());
        r.put("address",      s.getAddress());
        r.put("hasLogo",      s.getLogoData() != null && s.getLogoData().length > 0);
        return ResponseEntity.ok(r);
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody Map<String,Object> body) { return ResponseEntity.ok(svc.update(body)); }

    @PostMapping("/logo")
    public ResponseEntity<?> uploadLogo(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","No file provided"));
        svc.saveLogo(file.getBytes(), file.getContentType());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<?> removeLogo() { svc.removeLogo(); return ResponseEntity.ok(Map.of("ok", true)); }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        com.buildmat.model.SettingsEntity s = svc.get();
        if (s.getLogoData() == null || s.getLogoData().length == 0)
            return ResponseEntity.notFound().build();
        String ct = s.getLogoContentType() != null ? s.getLogoContentType() : "image/png";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct)).body(s.getLogoData());
    }
}
