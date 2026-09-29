package hu.nye.carrental.repository;

import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    @Query("""
           select c from Car c
           join fetch c.brand b
           join fetch c.category cat
           where (:brandId is null or b.id = :brandId)
             and (:categoryId is null or cat.id = :categoryId)
             and (:status is null or c.status = :status)
           order by b.name, c.plateNumber
           """)
    List<Car> search(@Param("brandId") Long brandId,
                     @Param("categoryId") Long categoryId,
                     @Param("status") CarStatus status);

    boolean existsByPlateNumberIgnoreCase(String plateNumber);

    boolean existsByPlateNumberIgnoreCaseAndIdNot(String plateNumber, Long id);
}
