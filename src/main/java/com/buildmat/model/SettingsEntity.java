package com.buildmat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "settings")
@Data
public class SettingsEntity {

    @Id
    private Long id = 1L;

    @Column(name = "business_name", length = 200)
    private String businessName = "My Business";

    @Column(name = "tag_line", length = 300)
    private String tagLine = "";

    @Column(name = "gst_number", length = 50)
    private String gstNumber = "";

    @Column(name = "phone", length = 50)
    private String phone = "";

    @Column(name = "email", length = 100)
    private String email = "";

    @Column(name = "address", length = 500)
    private String address = "";
}
