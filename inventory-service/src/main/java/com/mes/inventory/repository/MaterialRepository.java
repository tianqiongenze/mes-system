package com.mes.inventory.repository;
import com.mes.inventory.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface MaterialRepository extends JpaRepository<Material, String> {
    Optional<Material> findBySku(String sku);
    @Query("SELECT m FROM Material m WHERE m.quantity <= m.reorderPoint")
    List<Material> findBelowReorderPoint();
    @Query("SELECT m FROM Material m WHERE m.quantity <= m.safetyStock")
    List<Material> findBelowSafetyStock();
    @Query("SELECT COALESCE(SUM(CAST(m.quantity AS double) * m.unitCost),0) FROM Material m")
    double totalInventoryValue();
}
