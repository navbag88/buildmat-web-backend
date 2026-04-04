package com.buildmat.service;

import com.buildmat.model.SettingsEntity;
import com.buildmat.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository repo;

    public SettingsEntity get() {
        return repo.findById(1L).orElseGet(() -> {
            SettingsEntity s = new SettingsEntity();
            s.setId(1L);
            s.setBusinessName("My Business");
            s.setTagLine("");
            s.setGstNumber("");
            s.setPhone("");
            s.setEmail("");
            s.setAddress("");
            return repo.save(s);
        });
    }

    public SettingsEntity update(Map<String, Object> body) {
        SettingsEntity s = get();
        if (body.containsKey("businessName")) s.setBusinessName(str(body.get("businessName")));
        if (body.containsKey("tagLine"))      s.setTagLine(str(body.get("tagLine")));
        if (body.containsKey("gstNumber"))    s.setGstNumber(str(body.get("gstNumber")));
        if (body.containsKey("phone"))        s.setPhone(str(body.get("phone")));
        if (body.containsKey("email"))        s.setEmail(str(body.get("email")));
        if (body.containsKey("address"))      s.setAddress(str(body.get("address")));
        return repo.save(s);
    }

    public SettingsEntity saveLogo(byte[] data, String contentType) {
        SettingsEntity s = get();
        s.setLogoData(data);
        s.setLogoContentType(contentType != null ? contentType : "image/png");
        return repo.save(s);
    }

    public void removeLogo() {
        SettingsEntity s = get();
        s.setLogoData(null);
        s.setLogoContentType(null);
        repo.save(s);
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }
}
