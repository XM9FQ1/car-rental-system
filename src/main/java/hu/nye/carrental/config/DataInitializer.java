package hu.nye.carrental.config;

import hu.nye.carrental.model.Brand;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Category;
import hu.nye.carrental.model.Customer;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import hu.nye.carrental.service.RentalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uygulama başlarken çalışır. Veritabanı BOŞSA örnek veri yükler.
 * Veritabanında zaten veri varsa hiçbir şey yapmaz (mevcut veriye dokunmaz).
 * Kapatmak için application.properties içinde: app.seed-data=false
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final RentalService rentalService;
    private final boolean seedEnabled;

    public DataInitializer(BrandRepository brandRepository,
                           CategoryRepository categoryRepository,
                           CarRepository carRepository,
                           CustomerRepository customerRepository,
                           RentalRepository rentalRepository,
                           RentalService rentalService,
                           @Value("${app.seed-data:true}") boolean seedEnabled) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
        this.rentalRepository = rentalRepository;
        this.rentalService = rentalService;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Sample data: disabled (app.seed-data=false).");
            return;
        }
        if (brandRepository.count() > 0 || categoryRepository.count() > 0
                || carRepository.count() > 0 || customerRepository.count() > 0) {
            log.info("Sample data: skipped, the database already contains data.");
            return;
        }

        // ---- Brands ----
        Brand toyota = brand("Toyota");
        Brand bmw = brand("BMW");
        Brand volkswagen = brand("Volkswagen");
        Brand renault = brand("Renault");
        Brand ford = brand("Ford");
        Brand skoda = brand("Skoda");
        Brand suzuki = brand("Suzuki");

        // ---- Categories ----
        Category economy = category("Economy");
        Category hatchback = category("Hatchback");
        Category sedan = category("Sedan");
        Category suv = category("SUV");
        Category van = category("Van");

        // ---- Cars ----
        Car suzukiEconomy = car("AA-AB-101", suzuki, economy, "25.00", CarStatus.AVAILABLE);
        car("AA-AB-102", skoda, economy, "28.00", CarStatus.AVAILABLE);
        car("AA-AC-201", volkswagen, hatchback, "32.00", CarStatus.AVAILABLE);
        Car renaultHatchback = car("AA-AC-202", renault, hatchback, "30.00", CarStatus.AVAILABLE);
        Car toyotaSedan = car("AA-AD-301", toyota, sedan, "45.00", CarStatus.AVAILABLE);
        Car bmwSedan = car("AA-AD-302", bmw, sedan, "75.00", CarStatus.AVAILABLE);
        Car toyotaSuv = car("AA-AE-401", toyota, suv, "55.00", CarStatus.AVAILABLE);
        Car volkswagenSuv = car("AA-AE-402", volkswagen, suv, "60.00", CarStatus.AVAILABLE);
        car("AA-AF-501", ford, van, "65.00", CarStatus.AVAILABLE);
        car("AA-AF-502", renault, van, "58.00", CarStatus.MAINTENANCE);

        // ---- Customers ----
        Customer anna = customer("Anna", "Kovács", "anna.kovacs@example.com", "+36 30 111 2233", "HU1234567");
        Customer peter = customer("Péter", "Nagy", "peter.nagy@example.com", "+36 20 222 3344", "HU2345678");
        Customer eszter = customer("Eszter", "Szabó", "eszter.szabo@example.com", "+36 70 333 4455", "HU3456789");
        Customer mate = customer("Máté", "Tóth", "mate.toth@example.com", "+36 30 444 5566", "HU4567890");
        Customer lili = customer("Lili", "Horváth", "lili.horvath@example.com", "+36 20 555 6677", "HU5678901");
        Customer daniel = customer("Dániel", "Varga", "daniel.varga@example.com", "+36 70 666 7788", "HU6789012");

        // ---- Rentals (RentalService ile: araç durumu ve fiyat gerçek kullanımdaki gibi hesaplanır) ----
        LocalDate today = LocalDate.now();

        // Kapanmış kiralamalar
        Rental r1 = rentalService.createRental(toyotaSedan.getId(), anna.getId(), today.minusDays(20), today.minusDays(16));
        rentalService.returnCar(r1.getId(), today.minusDays(16));

        Rental r2 = rentalService.createRental(suzukiEconomy.getId(), peter.getId(), today.minusDays(12), today.minusDays(9));
        rentalService.returnCar(r2.getId(), today.minusDays(8));

        Rental r3 = rentalService.createRental(volkswagenSuv.getId(), eszter.getId(), today.minusDays(6), today.minusDays(4));
        rentalService.returnCar(r3.getId(), today.minusDays(4));

        // Aktif kiralamalar
        rentalService.createRental(bmwSedan.getId(), mate.getId(), today.minusDays(2), today.plusDays(3));
        rentalService.createRental(toyotaSuv.getId(), lili.getId(), today.minusDays(1), today.plusDays(4));

        // Gecikmiş (Overdue) kiralama: planlanan dönüş tarihi geçmiş, araç hâlâ dönmemiş
        rentalService.createRental(renaultHatchback.getId(), daniel.getId(), today.minusDays(7), today.minusDays(2));

        log.info("Sample data loaded: {} brands, {} categories, {} cars, {} customers, {} rentals.",
                brandRepository.count(), categoryRepository.count(), carRepository.count(),
                customerRepository.count(), rentalRepository.count());
    }

    private Brand brand(String name) {
        return brandRepository.save(new Brand(name));
    }

    private Category category(String name) {
        return categoryRepository.save(new Category(name));
    }

    private Car car(String plate, Brand brand, Category category, String dailyPrice, CarStatus status) {
        Car car = new Car();
        car.setPlateNumber(plate);
        car.setBrand(brand);
        car.setCategory(category);
        car.setDailyPrice(new BigDecimal(dailyPrice));
        car.setStatus(status);
        return carRepository.save(car);
    }

    private Customer customer(String firstName, String lastName, String email, String phone, String license) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setLicenseNumber(license);
        return customerRepository.save(customer);
    }
}
