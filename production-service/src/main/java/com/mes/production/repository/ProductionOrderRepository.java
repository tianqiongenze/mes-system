package com.mes.production.repository;
import com.mes.production.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, String> {
    List<ProductionOrder> findByStatus(String status);
    List<ProductionOrder> findByWorkCenter(String workCenter);
    List<ProductionOrder> findByAssignedOperator(String operator);
    @Query("SELECT COALESCE(SUM(o.quantity),0) FROM ProductionOrder o WHERE o.status = ?1")
    int totalQuantityByStatus(String status);
    @Query("SELECT COALESCE(SUM(o.defectCount),0) FROM ProductionOrder o WHERE o.status = 'COMPLETED'")
    int totalDefects();
    @Query("SELECT COUNT(o) FROM ProductionOrder o WHERE o.priority = ?1")
    long countByPriority(String priority);
}
