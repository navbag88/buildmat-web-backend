package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service @RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repo;
    private final SettingsService settingsService;

    public List<Map<String,Object>> getAll(String q) {
        List<CustomerEntity> list = (q == null || q.isBlank()) ? repo.findAll() :
            repo.findByNameContainingIgnoreCaseOrPhoneContaining(q, q);
        return list.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> getById(Long id) {
        return repo.findById(id).map(this::toMap).orElseThrow();
    }

    public Map<String,Object> save(Long id, Map<String,Object> body) {
        CustomerEntity c = id != null ? repo.findById(id).orElseThrow() : new CustomerEntity();
        c.setName((String)body.get("name"));
        c.setPhone((String)body.getOrDefault("phone",""));
        c.setEmail((String)body.getOrDefault("email",""));
        c.setAddress((String)body.getOrDefault("address",""));
        if (c.getCreatedAt() == null) c.setCreatedAt(LocalDateTime.now());
        CustomerEntity saved = repo.save(c);
        log.info("Customer {}: id={} name='{}'", id == null ? "created" : "updated", saved.getId(), saved.getName());
        return toMap(saved);
    }

    public void delete(Long id) {
        log.info("Customer deleted: id={}", id);
        repo.deleteById(id);
    }

    public Map<String,Object> importExcel(MultipartFile file) {
        try {
            List<Map<String,Object>> results = ExcelImportUtil.importCustomers(file.getInputStream());
            int imported = 0;
            List<String> errors = new ArrayList<>();
            for (Map<String,Object> row : results) {
                if (row.containsKey("error")) { errors.add((String)row.get("error")); continue; }
                CustomerEntity c = new CustomerEntity();
                c.setName((String)row.get("name")); c.setPhone((String)row.getOrDefault("phone",""));
                c.setEmail((String)row.getOrDefault("email","")); c.setAddress((String)row.getOrDefault("address",""));
                c.setCreatedAt(LocalDateTime.now()); repo.save(c); imported++;
            }
            log.info("Customer Excel import complete: imported={} errors={}", imported, errors.size());
            if (!errors.isEmpty()) log.warn("Customer import errors: {}", errors);
            return Map.of("imported", imported, "errors", errors);
        } catch (Exception e) {
            log.error("Customer Excel import failed: {}", e.getMessage(), e);
            throw new RuntimeException("Import failed: " + e.getMessage());
        }
    }

    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = ExcelExportUtil.exportCustomers(repo.findAll());
            return download(data, "customers.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try {
            byte[] data = PdfExportUtil.exportCustomers(repo.findAll(), settingsService.get().getBusinessName());
            return download(data, "customers.pdf", "application/pdf");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> importTemplate() {
        try {
            byte[] data = ExcelImportUtil.generateCustomerTemplate();
            return download(data, "customers-import-template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private Map<String,Object> toMap(CustomerEntity c) {
        return Map.of("id",c.getId(),"name",c.getName(),"phone",nvl(c.getPhone()),
            "email",nvl(c.getEmail()),"address",nvl(c.getAddress()));
    }

    private String nvl(String s) { return s == null ? "" : s; }
    private ResponseEntity<byte[]> download(byte[] data, String name, String ct) {
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+name)
            .contentType(MediaType.parseMediaType(ct)).body(data);
    }
}
