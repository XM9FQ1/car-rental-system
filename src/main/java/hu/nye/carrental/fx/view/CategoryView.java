package hu.nye.carrental.fx.view;

import hu.nye.carrental.model.Category;
import hu.nye.carrental.repository.CategoryRepository;

import java.util.List;

/** Categories screen (JavaFX). */
public class CategoryView extends NameListView<Category> {

    private final CategoryRepository repository;

    public CategoryView(CategoryRepository repository) {
        super("Categories", "Category");
        this.repository = repository;
    }

    @Override
    protected List<Category> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Override
    protected Category newEntity() {
        return new Category();
    }

    @Override
    protected Long idOf(Category item) {
        return item.getId();
    }

    @Override
    protected String nameOf(Category item) {
        return item.getName();
    }

    @Override
    protected void setName(Category item, String name) {
        item.setName(name);
    }

    @Override
    protected boolean nameExists(String name, Long excludeId) {
        return excludeId == null
                ? repository.existsByNameIgnoreCase(name)
                : repository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    @Override
    protected void save(Category item) {
        repository.save(item);
    }

    @Override
    protected void deleteById(Long id) {
        repository.deleteById(id);
    }
}
