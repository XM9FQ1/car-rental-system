package hu.nye.carrental.controller;

import hu.nye.carrental.model.Brand;
import hu.nye.carrental.model.Car;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Category;
import hu.nye.carrental.repository.BrandRepository;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CategoryRepository;
import hu.nye.carrental.repository.RentalRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cars")
public class CarController {

    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final RentalRepository rentalRepository;

    public CarController(CarRepository carRepository,
                         BrandRepository brandRepository,
                         CategoryRepository categoryRepository,
                         RentalRepository rentalRepository) {
        this.carRepository = carRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.rentalRepository = rentalRepository;
    }

    @ModelAttribute("brands")
    public List<Brand> brands() {
        return brandRepository.findAllByOrderByNameAsc();
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @ModelAttribute("statuses")
    public CarStatus[] statuses() {
        return CarStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long brandId,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) CarStatus status,
                       Model model) {
        model.addAttribute("cars", carRepository.search(brandId, categoryId, status));
        model.addAttribute("brandId", brandId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        return "cars/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("car", new Car());
        return "cars/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));
        model.addAttribute("car", car);
        return "cars/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("car") Car car,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {

        if (car.getPlateNumber() != null) {
            car.setPlateNumber(car.getPlateNumber().trim().toUpperCase());
        }

        if (car.getPlateNumber() != null && !car.getPlateNumber().isEmpty()) {
            boolean duplicate = (car.getId() == null)
                    ? carRepository.existsByPlateNumberIgnoreCase(car.getPlateNumber())
                    : carRepository.existsByPlateNumberIgnoreCaseAndIdNot(car.getPlateNumber(), car.getId());
            if (duplicate) {
                result.rejectValue("plateNumber", "duplicate", "A car with this plate number already exists.");
            }
        }

        // Durum kuralları: RENTED durumu sadece kiralama ile verilir / kaldırılır
        boolean hasActiveRental = car.getId() != null
                && rentalRepository.existsByCar_IdAndReturnDateIsNull(car.getId());
        if (hasActiveRental) {
            if (car.getStatus() != CarStatus.RENTED) {
                result.rejectValue("status", "rented",
                        "This car has an active rental. Close the rental to make it available again.");
            }
        } else if (car.getStatus() == CarStatus.RENTED) {
            result.rejectValue("status", "auto",
                    "The Rented status is set automatically when a rental is created.");
        }

        if (result.hasErrors()) {
            return "cars/form";
        }

        carRepository.save(car);
        redirectAttributes.addFlashAttribute("success", "Car saved successfully.");
        return "redirect:/cars";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            carRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Car deleted.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error",
                    "This car cannot be deleted because it has rentals.");
        }
        return "redirect:/cars";
    }
}
