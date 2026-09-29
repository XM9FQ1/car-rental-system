package hu.nye.carrental.repository;

import hu.nye.carrental.model.InsurancePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {

    List<InsurancePlan> findAllByOrderByDailyPriceAscNameAsc();

    Optional<InsurancePlan> findFirstByOrderByDailyPriceAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
