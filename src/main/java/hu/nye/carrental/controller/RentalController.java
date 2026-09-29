package hu.nye.carrental.controller;

import hu.nye.carrental.dto.RentalForm;
import hu.nye.carrental.model.CarStatus;
import hu.nye.carrental.model.Rental;
import hu.nye.carrental.repository.CarRepository;
import hu.nye.carrental.repository.CustomerRepository;
import hu.nye.carrental.repository.RentalRepository;
import hu.nye.carrental.service.RentalException;
import hu.nye.carrental.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@Controller
@RequestMapping("/rentals")
public class RentalController {

    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final RentalService rentalService;

    public RentalController(RentalRepository rentalRepository,
                            CarRepository carRepository,
                            CustomerRepository customerRepository,
                            RentalService rentalService) {
        this.rentalRepository = rentalRepository;
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
        this.rentalService = rentalService;
    }

    // Listeleme: GET /rentals?show=all|active|closed
    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "all") String show, Model model) {
        switch (show) {
            case "active" -> model.addAttribute("rentals", rentalRepository.findActiveWithDetails());
            case "closed" -> model.addAttribute("rentals", rentalRepository.findClosedWithDetails());
            default -> {
                show = "all";
                model.addAttribute("rentals", rentalRepository.findAllWithDetails());
            }
        }
        model.addAttribute("show", show);
        return "rentals/list";
    }

    // Yeni kiralama formu: GET /rentals/new  (araç listesinden gelirse ?carId=5)
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long carId, Model model) {
        RentalForm form = new RentalForm();
        form.setCarId(carId);
        form.setStartDate(LocalDate.now());
        form.setPlannedEndDate(LocalDate.now().plusDays(1));
        model.addAttribute("rentalForm", form);
        addFormData(model);
        return "rentals/form";
    }

    // Kiralamayı oluştur: POST /rentals/save
    @PostMapping("/save")
    public String create(@Valid @ModelAttribute("rentalForm") RentalForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (form.getStartDate() != null && form.getPlannedEndDate() != null
                && form.getPlannedEndDate().isBefore(form.getStartDate())) {
            result.rejectValue("plannedEndDate", "range", "Planned return date cannot be before the start date.");
        }

        if (result.hasErrors()) {
            addFormData(model);
            return "rentals/form";
        }

        try {
            Rental rental = rentalService.createRental(
                    form.getCarId(), form.getCustomerId(), form.getStartDate(), form.getPlannedEndDate());
            redirectAttributes.addFlashAttribute("success",
                    "Rental created: " + rental.getCar().getPlateNumber()
                            + " is now rented to " + rental.getCustomer().getFullName() + ".");
            return "redirect:/rentals";
        } catch (RentalException e) {
            result.reject("rental", e.getMessage());
            addFormData(model);
            return "rentals/form";
        }
    }

    // Araç dönüş sayfası: GET /rentals/{id}/return
    @GetMapping("/{id}/return")
    public String returnForm(@PathVariable Long id, Model model) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found"));
        model.addAttribute("rental", rental);
        model.addAttribute("today", LocalDate.now());
        return "rentals/return";
    }

    // Aracı teslim al ve kiralamayı kapat: POST /rentals/{id}/return
    @PostMapping("/{id}/return")
    public String returnCar(@PathVariable Long id,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                            RedirectAttributes redirectAttributes) {
        try {
            Rental rental = rentalService.returnCar(id, returnDate);
            redirectAttributes.addFlashAttribute("success",
                    "Car " + rental.getCar().getPlateNumber() + " returned. Total price: "
                            + rental.getTotalPrice().toPlainString());
            return "redirect:/rentals";
        } catch (RentalException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rentals/" + id + "/return";
        }
    }

    // Formdaki açılır listeler: sadece müsait araçlar + tüm müşteriler
    private void addFormData(Model model) {
        model.addAttribute("cars", carRepository.search(null, null, CarStatus.AVAILABLE));
        model.addAttribute("customers", customerRepository.findAllByOrderByLastNameAscFirstNameAsc());
    }
}
