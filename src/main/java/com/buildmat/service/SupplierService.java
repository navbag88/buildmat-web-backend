package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service @RequiredArgsConstructor @Transactional
public class SupplierService {
    private final SupplierRepository repo;
    private final SettingsService settingsService;

    public List<Map<String,Object>> getAll(String q) {
        List<SupplierEntity> list = (q == null || q.isBlank())
            ? repo.findByOrderByNameAsc()
            : repo.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(q, q);
        return list.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> getById(Long id) {
        return repo.findById(id).map(this::toMap).orElseThrow();
    }

    public Map<String,Object> save(Long id, Map<String,Object> body) {
        SupplierEntity s = id != null ? repo.findById(id).orElseThrow() : new SupplierEntity();
        s.setName((String) body.get("name"));
        s.setPhone((String) body.getOrDefault("phone", ""));
        s.setEmail((String) body.getOrDefault("email", ""));
        s.setAddress((String) body.getOrDefault("address", ""));
        s.setGstin((String) body.getOrDefault("gstin", ""));
        s.setContactPerson((String) body.getOrDefault("contactPerson", ""));
        if (s.getCreatedAt() == null) s.setCreatedAt(LocalDateTime.now());
        SupplierEntity saved = repo.save(s);
        log.info("Supplier {}: id={} name='{}'", id == null ? "created" : "updated", saved.getId(), saved.getName());
        return toMap(saved);
    }

    public void delete(Long id) {
        log.info("Supplier deleted: id={}", id);
        repo.deleteById(id);
    }

    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = ExcelExportUtil.exportSuppliers(repo.findByOrderByNameAsc());
            return download(data, "suppliers.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) {
            log.error("Supplier Excel export failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try {
            byte[] data = PdfExportUtil.exportSuppliers(repo.findByOrderByNameAsc(), settingsService.get().getBusinessName());
            return download(data, "suppliers.pdf", "application/pdf");
        } catch (Exception e) {
            log.error("Supplier PDF export failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Map<String,Object> toMap(SupplierEntity s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("phone", nvl(s.getPhone()));
        m.put("email", nvl(s.getEmail()));
        m.put("address", nvl(s.getAddress()));
        m.put("gstin", nvl(s.getGstin()));
        m.put("contactPerson", nvl(s.getContactPerson()));
        return m;
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private ResponseEntity<byte[]> download(byte[] data, String name, String ct) {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + name)
            .contentType(MediaType.parseMediaType(ct)).body(data);
    }
}
