package hu.nye.carrental.config;

import hu.nye.carrental.model.Brand;
import hu.nye.carrental.repository.BrandRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// Formdaki açılır listeden gelen id'yi (ör. "3") Brand nesnesine çevirir.
@Component
public class StringToBrandConverter implements Converter<String, Brand> {

    private final BrandRepository brandRepository;

    public StringToBrandConverter(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public Brand convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return brandRepository.findById(Long.valueOf(source.trim())).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
