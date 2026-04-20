package com.buildmat.repository;
import com.buildmat.model.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {
    List<PurchaseEntity> findByOrderByPurchaseDateDescIdDesc();
    List<PurchaseEntity> findByPurchaseNumberContainingIgnoreCaseOrSupplierNameContainingIgnoreCaseOrStatusContainingIgnoreCase(
        String num, String supplier, String status);
    @Query("SELECT MAX(p.purchaseNumber) FROM PurchaseEntity p WHERE p.purchaseNumber LIKE :prefix%")
    Optional<String> findMaxPurchaseNumber(String prefix);
}
