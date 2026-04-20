package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "suppliers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String phone;
    private String email;
    @Column(columnDefinition = "TEXT") private String address;
    private String gstin;
    private String contactPerson;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
