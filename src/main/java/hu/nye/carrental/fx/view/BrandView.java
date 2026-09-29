package hu.nye.carrental.fx.view;

import hu.nye.carrental.model.Brand;
import hu.nye.carrental.repository.BrandRepository;

import java.util.List;

/** Brands screen (JavaFX). */
public class BrandView extends NameListView<Brand> {

    private final BrandRepository repository;

    public BrandView(BrandRepository repository) {
        super("Brands", "Brand");
        this.repository = repository;
    }

    @Override
    protected List<Brand> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Override
    protected Brand newEntity() {
        return new Brand();
    }

    @Override
    protected Long idOf(Brand item) {
        return item.getId();
    }

    @Override
    protected String nameOf(Brand item) {
        return item.getName();
    }

    @Override
    protected void setName(Brand item, String name) {
        item.setName(name);
    }

    @Override
    protected boolean nameExists(String name, Long excludeId) {
        return excludeId == null
                ? repository.existsByNameIgnoreCase(name)
                : repository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    @Override
    protected void save(Brand item) {
        repository.save(item);
    }

    @Override
    protected void deleteById(Long id) {
        repository.deleteById(id);
    }
}
