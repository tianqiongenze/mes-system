package com.mes.equipment.repository;
import com.mes.equipment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, String> {
    Optional<Equipment> findByCode(String code);
    List<Equipment> findByStatus(String status);
    List<Equipment> findByLocation(String location);
    @Query("SELECT COALESCE(AVG(e.utilizationRate),0) FROM Equipment e")
    double averageUtilization();
    long countByStatus(String status);
}
