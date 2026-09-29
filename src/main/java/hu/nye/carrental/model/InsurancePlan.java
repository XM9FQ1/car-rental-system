package hu.nye.carrental.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * An insurance package that can be chosen for a rental (like at real rental companies).
 * dailyPrice = extra cost per day, deductible = the most the customer pays in case of damage (excess).
 */
@Entity
@Table(name = "insurance_plans")
public class InsurancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required.")
    @Size(max = 50, message = "Name can be at most 50 characters.")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 500, message = "Description can be at most 500 characters.")
    @Column(length = 500)
    private String description;

    @NotNull(message = "Daily price is required.")
    @DecimalMin(value = "0.00", message = "Daily price cannot be negative.")
    @Digits(integer = 8, fraction = 2, message = "Daily price can have at most 2 decimal places.")
    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @NotNull(message = "Deductible is required.")
    @DecimalMin(value = "0.00", message = "Deductible cannot be negative.")
    @Digits(integer = 8, fraction = 2, message = "Deductible can have at most 2 decimal places.")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deductible;

    public InsurancePlan() {
    }

    public InsurancePlan(String name, String description, BigDecimal dailyPrice, BigDecimal deductible) {
        this.name = name;
        this.description = description;
        this.dailyPrice = dailyPrice;
        this.deductible = deductible;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }
}
