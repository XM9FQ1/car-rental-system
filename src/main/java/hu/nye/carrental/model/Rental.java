package hu.nye.carrental.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "rentals")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "planned_end_date", nullable = false)
    private LocalDate plannedEndDate;

    // null = kiralama hâlâ açık (araç henüz dönmedi)
    @Column(name = "return_date")
    private LocalDate returnDate;

    // Kiralama anındaki günlük fiyat (araç fiyatı sonradan değişse de bu sabit kalır)
    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    // Araç dönünce hesaplanır
    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    public Rental() {
    }

    // ---- İş mantığı yardımcıları ----

    public boolean isActive() {
        return returnDate == null;
    }

    public boolean isOverdue() {
        return isActive() && plannedEndDate != null && plannedEndDate.isBefore(LocalDate.now());
    }

    public long getPlannedDays() {
        return daysBetween(startDate, plannedEndDate);
    }

    public BigDecimal getEstimatedPrice() {
        return calculatePrice(plannedEndDate);
    }

    // Toplam ücret = gün sayısı x günlük fiyat (en az 1 gün)
    public BigDecimal calculatePrice(LocalDate endDate) {
        return dailyPrice.multiply(BigDecimal.valueOf(daysBetween(startDate, endDate)));
    }

    private static long daysBetween(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        return Math.max(1, days);
    }

    // ---- Getter / Setter ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
