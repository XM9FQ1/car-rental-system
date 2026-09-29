package hu.nye.carrental.controller;

import hu.nye.carrental.model.Brand;
import hu.nye.carrental.repository.BrandRepository;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/brands")
public class BrandController {

    private final BrandRepository brandRepository;

    public BrandController(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("brands", brandRepository.findAllByOrderByNameAsc());
        return "brands/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("brand", new Brand());
        return "brands/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        model.addAttribute("brand", brand);
        return "brands/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("brand") Brand brand,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {

        if (brand.getName() != null) {
            brand.setName(brand.getName().trim());
        }

        boolean duplicate = (brand.getId() == null)
                ? brandRepository.existsByNameIgnoreCase(brand.getName())
                : brandRepository.existsByNameIgnoreCaseAndIdNot(brand.getName(), brand.getId());

        if (duplicate) {
            result.rejectValue("name", "duplicate", "A brand with this name already exists.");
        }

        if (result.hasErrors()) {
            return "brands/form";
        }

        brandRepository.save(brand);
        redirectAttributes.addFlashAttribute("success", "Brand saved successfully.");
        return "redirect:/brands";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            brandRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Brand deleted.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error",
                    "This brand cannot be deleted because it is used by one or more cars.");
        }
        return "redirect:/brands";
    }
}
