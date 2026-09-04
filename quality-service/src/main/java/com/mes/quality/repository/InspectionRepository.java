package com.mes.quality.repository;
import com.mes.quality.entity.InspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface InspectionRepository extends JpaRepository<InspectionRecord, String> {
    List<InspectionRecord> findByProductionOrderId(String orderId);
    List<InspectionRecord> findByResult(String result);
    @Query("SELECT COALESCE(SUM(r.passed),0) FROM InspectionRecord r")
    int totalPassed();
    @Query("SELECT COALESCE(SUM(r.failed),0) FROM InspectionRecord r")
    int totalFailed();
}
