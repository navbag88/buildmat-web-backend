package com.buildmat.repository;
import com.buildmat.model.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
    List<SupplierEntity> findByOrderByNameAsc();
    List<SupplierEntity> findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(String name, String phone);
}
