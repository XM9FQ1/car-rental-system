package hu.nye.carrental.controller;

import hu.nye.carrental.model.Customer;
import hu.nye.carrental.repository.CustomerRepository;
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

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Listeleme + arama: GET /customers?q=anna
    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        if (q != null && !q.isBlank()) {
            String term = q.trim();
            model.addAttribute("customers", customerRepository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
                            term, term, term));
        } else {
            model.addAttribute("customers", customerRepository.findAllByOrderByLastNameAscFirstNameAsc());
        }
        model.addAttribute("q", q);
        return "customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        model.addAttribute("customer", customer);
        return "customers/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("customer") Customer customer,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {

        if (customer.getFirstName() != null) customer.setFirstName(customer.getFirstName().trim());
        if (customer.getLastName() != null) customer.setLastName(customer.getLastName().trim());
        if (customer.getEmail() != null) customer.setEmail(customer.getEmail().trim().toLowerCase());
        if (customer.getPhone() != null) customer.setPhone(customer.getPhone().trim());
        if (customer.getLicenseNumber() != null) customer.setLicenseNumber(customer.getLicenseNumber().trim().toUpperCase());

        boolean isNew = (customer.getId() == null);

        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            boolean emailTaken = isNew
                    ? customerRepository.existsByEmailIgnoreCase(customer.getEmail())
                    : customerRepository.existsByEmailIgnoreCaseAndIdNot(customer.getEmail(), customer.getId());
            if (emailTaken) {
                result.rejectValue("email", "duplicate", "A customer with this email already exists.");
            }
        }

        if (customer.getLicenseNumber() != null && !customer.getLicenseNumber().isEmpty()) {
            boolean licenseTaken = isNew
                    ? customerRepository.existsByLicenseNumberIgnoreCase(customer.getLicenseNumber())
                    : customerRepository.existsByLicenseNumberIgnoreCaseAndIdNot(customer.getLicenseNumber(), customer.getId());
            if (licenseTaken) {
                result.rejectValue("licenseNumber", "duplicate", "A customer with this license number already exists.");
            }
        }

        if (result.hasErrors()) {
            return "customers/form";
        }

        customerRepository.save(customer);
        redirectAttributes.addFlashAttribute("success", "Customer saved successfully.");
        return "redirect:/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Customer deleted.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error",
                    "This customer cannot be deleted because they have rentals.");
        }
        return "redirect:/customers";
    }
}
