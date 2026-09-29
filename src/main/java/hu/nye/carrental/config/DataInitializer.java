package hu.nye.carrental.config;

import hu.nye.carrental.model.Brand;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Category;
import hu.nye.carrental.model.Customer;
import hu.nye.carrental.model.InsurancePlan;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.InsurancePlanRepository;
import hu.nye.carrental.repository.RentalRepository;
import hu.nye.carrental.service.RentalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Runs when the application starts.
 * - Insurance plans are added if there are none yet (also for existing databases).
 * - All other sample data is added only if the database is EMPTY (existing data is never touched).
 * Turn it off in application.properties: app.seed-data=false
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final RentalService rentalService;
    private final boolean seedEnabled;

    public DataInitializer(BrandRepository brandRepository,
                           CategoryRepository categoryRepository,
                           CarRepository carRepository,
                           CustomerRepository customerRepository,
                           RentalRepository rentalRepository,
                           InsurancePlanRepository insurancePlanRepository,
                           RentalService rentalService,
                           @Value("${app.seed-data:true}") boolean seedEnabled) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
        this.rentalRepository = rentalRepository;
        this.insurancePlanRepository = insurancePlanRepository;
        this.rentalService = rentalService;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Sample data: disabled (app.seed-data=false).");
            return;
        }

        seedInsurancePlans();

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

        // ---- Cars (brand, model, year) ----
        Car suzukiSwift = car("AA-AB-101", suzuki, "Swift", 2021, economy, "25.00", CarStatus.AVAILABLE);
        car("AA-AB-102", skoda, "Fabia", 2022, economy, "28.00", CarStatus.AVAILABLE);
        car("AA-AC-201", volkswagen, "Golf", 2023, hatchback, "32.00", CarStatus.AVAILABLE);
        Car renaultClio = car("AA-AC-202", renault, "Clio", 2022, hatchback, "30.00", CarStatus.AVAILABLE);
        Car toyotaCorolla = car("AA-AD-301", toyota, "Corolla", 2023, sedan, "45.00", CarStatus.AVAILABLE);
        Car bmw3 = car("AA-AD-302", bmw, "3 Series", 2022, sedan, "75.00", CarStatus.AVAILABLE);
        Car toyotaRav4 = car("AA-AE-401", toyota, "RAV4", 2024, suv, "55.00", CarStatus.AVAILABLE);
        Car vwTiguan = car("AA-AE-402", volkswagen, "Tiguan", 2023, suv, "60.00", CarStatus.AVAILABLE);
        car("AA-AF-501", ford, "Transit", 2021, van, "65.00", CarStatus.AVAILABLE);
        car("AA-AF-502", renault, "Trafic", 2020, van, "58.00", CarStatus.MAINTENANCE);

        // ---- Customers ----
        Customer anna = customer("Anna", "Kovács", "anna.kovacs@example.com", "+36 30 111 2233", "HU1234567");
        Customer peter = customer("Péter", "Nagy", "peter.nagy@example.com", "+36 20 222 3344", "HU2345678");
        Customer eszter = customer("Eszter", "Szabó", "eszter.szabo@example.com", "+36 70 333 4455", "HU3456789");
        Customer mate = customer("Máté", "Tóth", "mate.toth@example.com", "+36 30 444 5566", "HU4567890");
        Customer lili = customer("Lili", "Horváth", "lili.horvath@example.com", "+36 20 555 6677", "HU5678901");
        Customer daniel = customer("Dániel", "Varga", "daniel.varga@example.com", "+36 70 666 7788", "HU6789012");

        // ---- Rentals (through RentalService, so statuses and prices are calculated like in real use) ----
        List<InsurancePlan> plans = insurancePlanRepository.findAllByOrderByDailyPriceAscNameAsc();
        Long basic = planId(plans, 0);
        Long medium = planId(plans, 1);
        Long premium = planId(plans, 2);
        LocalDate today = LocalDate.now();

        // Closed rentals
        Rental r1 = rentalService.createRental(toyotaCorolla.getId(), anna.getId(), today.minusDays(20), today.minusDays(16), premium);
        rentalService.returnCar(r1.getId(), today.minusDays(16));

        Rental r2 = rentalService.createRental(suzukiSwift.getId(), peter.getId(), today.minusDays(12), today.minusDays(9), basic);
        rentalService.returnCar(r2.getId(), today.minusDays(8));

        Rental r3 = rentalService.createRental(vwTiguan.getId(), eszter.getId(), today.minusDays(6), today.minusDays(4), medium);
        rentalService.returnCar(r3.getId(), today.minusDays(4));

        // Active rentals
        rentalService.createRental(bmw3.getId(), mate.getId(), today.minusDays(2), today.plusDays(3), premium);
        rentalService.createRental(toyotaRav4.getId(), lili.getId(), today.minusDays(1), today.plusDays(4), medium);

        // Overdue rental: planned return date has passed, car not returned yet
        rentalService.createRental(renaultClio.getId(), daniel.getId(), today.minusDays(7), today.minusDays(2), basic);

        log.info("Sample data loaded: {} brands, {} categories, {} cars, {} customers, {} rentals, {} insurance plans.",
                brandRepository.count(), categoryRepository.count(), carRepository.count(),
                customerRepository.count(), rentalRepository.count(), insurancePlanRepository.count());
    }

    /** Insurance packages like at real rental companies (daily price + deductible / excess). */
    private void seedInsurancePlans() {
        if (insurancePlanRepository.count() > 0) {
            return;
        }
        insurancePlanRepository.save(new InsurancePlan("Basic",
                "Included in every rental. Third-party liability, collision damage waiver (CDW) "
                        + "and theft protection (TP). You pay up to the deductible for any damage.",
                new BigDecimal("0.00"), new BigDecimal("1500.00")));
        insurancePlanRepository.save(new InsurancePlan("Medium",
                "Everything in Basic with a lower deductible, plus windscreen and glass cover.",
                new BigDecimal("11.00"), new BigDecimal("500.00")));
        insurancePlanRepository.save(new InsurancePlan("Premium",
                "Zero deductible. Covers glass, tyres, underbody and roof, with 24/7 roadside "
                        + "assistance and personal accident insurance (PAI).",
                new BigDecimal("24.00"), new BigDecimal("0.00")));
        log.info("Sample data: 3 insurance plans added.");
    }

    private static Long planId(List<InsurancePlan> plans, int index) {
        return plans.size() > index ? plans.get(index).getId() : null;
    }

    private Brand brand(String name) {
        return brandRepository.save(new Brand(name));
    }

    private Category category(String name) {
        return categoryRepository.save(new Category(name));
    }

    private Car car(String plate, Brand brand, String model, int year, Category category,
                    String dailyPrice, CarStatus status) {
        Car car = new Car();
        car.setPlateNumber(plate);
        car.setBrand(brand);
        car.setModel(model);
        car.setYear(year);
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
