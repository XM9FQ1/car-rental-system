package hu.nye.carrental.service;

import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Customer;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;

    public RentalService(RentalRepository rentalRepository,
                         CarRepository carRepository,
                         CustomerRepository customerRepository) {
        this.rentalRepository = rentalRepository;
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
    }

    // Yeni kiralama: kiralama kaydı oluşur + araç RENTED olur (ikisi birlikte, tek transaction)
    @Transactional
    public Rental createRental(Long carId, Long customerId, LocalDate startDate, LocalDate plannedEndDate) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RentalException("The selected car does not exist."));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RentalException("The selected customer does not exist."));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new RentalException("Car " + car.getPlateNumber() + " is not available for rent.");
        }
        if (plannedEndDate.isBefore(startDate)) {
            throw new RentalException("Planned return date cannot be before the start date.");
        }

        Rental rental = new Rental();
        rental.setCar(car);
        rental.setCustomer(customer);
        rental.setStartDate(startDate);
        rental.setPlannedEndDate(plannedEndDate);
        rental.setDailyPrice(car.getDailyPrice());

        car.setStatus(CarStatus.RENTED);

        return rentalRepository.save(rental);
    }

    // Araç dönüşü: dönüş tarihi + toplam ücret kaydedilir, araç tekrar AVAILABLE olur
    @Transactional
    public Rental returnCar(Long rentalId, LocalDate returnDate) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RentalException("Rental not found."));

        if (!rental.isActive()) {
            throw new RentalException("This rental is already closed.");
        }
        if (returnDate == null) {
            throw new RentalException("Return date is required.");
        }
        if (returnDate.isBefore(rental.getStartDate())) {
            throw new RentalException("Return date cannot be before the start date (" + rental.getStartDate() + ").");
        }

        rental.setReturnDate(returnDate);
        rental.setTotalPrice(rental.calculatePrice(returnDate));

        Car car = rental.getCar();
        if (car.getStatus() == CarStatus.RENTED) {
            car.setStatus(CarStatus.AVAILABLE);
        }

        return rental;
    }
}
