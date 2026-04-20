package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "purchase_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "purchase_id") private PurchaseEntity purchase;
    private Long productId;
    @Column(nullable = false) private String productName;
    private String unit;
    @Column(nullable = false) private Double quantity;
    @Column(nullable = false) private Double unitPrice;
    @Column(nullable = false) private Double total;
    @Builder.Default private Double sgstPercent = 0.0;
    @Builder.Default private Double cgstPercent = 0.0;
}
