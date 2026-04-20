package com.buildmat.repository;
import com.buildmat.model.PurchasePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePaymentEntity, Long> {
    List<PurchasePaymentEntity> findByPurchaseId(Long purchaseId);
    List<PurchasePaymentEntity> findByOrderByPaymentDateDescIdDesc();
}
