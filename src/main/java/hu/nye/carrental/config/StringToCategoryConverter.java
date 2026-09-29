package hu.nye.carrental.config;

import hu.nye.carrental.model.Category;
import hu.nye.carrental.repository.CategoryRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// Formdaki açılır listeden gelen id'yi (ör. "2") Category nesnesine çevirir.
@Component
public class StringToCategoryConverter implements Converter<String, Category> {

    private final CategoryRepository categoryRepository;

    public StringToCategoryConverter(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return categoryRepository.findById(Long.valueOf(source.trim())).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
