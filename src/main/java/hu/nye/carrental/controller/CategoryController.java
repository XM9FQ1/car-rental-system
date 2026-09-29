package hu.nye.carrental.controller;

import hu.nye.carrental.model.Category;
import hu.nye.carrental.repository.CategoryRepository;
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
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        return "categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        return "categories/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        model.addAttribute("category", category);
        return "categories/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("category") Category category,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {

        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }

        boolean duplicate = (category.getId() == null)
                ? categoryRepository.existsByNameIgnoreCase(category.getName())
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(category.getName(), category.getId());

        if (duplicate) {
            result.rejectValue("name", "duplicate", "A category with this name already exists.");
        }

        if (result.hasErrors()) {
            return "categories/form";
        }

        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Category saved successfully.");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error",
                    "This category cannot be deleted because it is used by one or more cars.");
        }
        return "redirect:/categories";
    }
}
