package hu.nye.carrental.repository;

import hu.nye.carrental.model.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    @Query("""
           select r from Rental r
           join fetch r.car c
           join fetch c.brand
           join fetch c.category
           join fetch r.customer
           order by r.startDate desc, r.id desc
           """)
    List<Rental> findAllWithDetails();

    @Query("""
           select r from Rental r
           join fetch r.car c
           join fetch c.brand
           join fetch c.category
           join fetch r.customer
           where r.returnDate is null
           order by r.plannedEndDate asc
           """)
    List<Rental> findActiveWithDetails();

    @Query("""
           select r from Rental r
           join fetch r.car c
           join fetch c.brand
           join fetch c.category
           join fetch r.customer
           where r.returnDate is not null
           order by r.returnDate desc, r.id desc
           """)
    List<Rental> findClosedWithDetails();

    // Bu aracın açık (dönmemiş) bir kiralaması var mı?
    boolean existsByCar_IdAndReturnDateIsNull(Long carId);
}
