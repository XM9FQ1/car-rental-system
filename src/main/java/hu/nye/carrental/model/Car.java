package hu.nye.carrental.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Brand is required.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    // Model name, e.g. "Corolla" (column is nullable so older cars can still be loaded)
    @NotBlank(message = "Model is required.")
    @Size(max = 50, message = "Model can be at most 50 characters.")
    @Column(length = 50)
    private String model;

    // Model year, e.g. 2023 ("year" is a reserved word in some databases, so the column is model_year)
    @NotNull(message = "Year is required.")
    @Min(value = 1990, message = "Year must be 1990 or later.")
    @Max(value = 2100, message = "Year is not valid.")
    @Column(name = "model_year")
    private Integer year;

    @NotNull(message = "Category is required.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // "^$|" -> boş değeri bu kural atlar; boşluk hatasını sadece @NotBlank verir.
    @NotBlank(message = "Plate number is required.")
    @Pattern(regexp = "^$|^[A-Za-z0-9 -]{2,15}$",
             message = "Plate number must be 2-15 characters (letters, digits, space or dash).")
    @Column(name = "plate_number", nullable = false, unique = true, length = 15)
    private String plateNumber;

    @NotNull(message = "Daily price is required.")
    @DecimalMin(value = "0.01", message = "Daily price must be greater than 0.")
    @Digits(integer = 8, fraction = 2, message = "Daily price can have at most 2 decimal places.")
    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @NotNull(message = "Status is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarStatus status = CarStatus.AVAILABLE;

    // Optional photo of the car from the internet (downloaded and cached by the desktop app)
    @Size(max = 500, message = "Image URL can be at most 500 characters.")
    @Pattern(regexp = "^$|^https?://.+", message = "Image URL must start with http:// or https://.")
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public Car() {
    }

    /** "Toyota Corolla 2023" */
    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        if (brand != null) {
            name.append(brand.getName());
        }
        if (model != null && !model.isBlank()) {
            name.append(' ').append(model);
        }
        if (year != null) {
            name.append(' ').append(year);
        }
        return name.toString().trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public CarStatus getStatus() {
        return status;
    }

    public void setStatus(CarStatus status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
