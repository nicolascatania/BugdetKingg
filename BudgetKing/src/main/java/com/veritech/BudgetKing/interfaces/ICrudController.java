package com.veritech.BudgetKing.interfaces;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface ICrudController<E, D, ID, FILTER> {

    ICrudService<E, ID, FILTER> getService();
    ICrudMapper<E, D> getMapper();

    @GetMapping("/{id}")
    default ResponseEntity<D> getById(@PathVariable ID id) {
        E entity = getService().getById(id);
        return ResponseEntity.ok(getMapper().toDto(entity));
    }

    @PostMapping
    default ResponseEntity<D> create(@Valid @RequestBody D dto) {
        E entity = getMapper().toEntity(dto);
        E saved = getService().create(entity);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(getMapper().toDto(saved));
    }

    @PutMapping("/{id}")
    default ResponseEntity<D> update(
            @PathVariable ID id,
            @Valid @RequestBody D dto
    ) {
        E existing = getService().getById(id);
        getMapper().updateEntity(existing, dto);
        E updated = getService().update(id, existing);
        return ResponseEntity.ok(getMapper().toDto(updated));
    }

    @DeleteMapping("/{id}")
    default ResponseEntity<Void> delete(@PathVariable ID id) {
        getService().deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    default ResponseEntity<List<D>> search(@RequestBody FILTER filter) {
        return ResponseEntity.ok(
                getService()
                        .search(filter)
                        .stream()
                        .map(getMapper()::toDto)
                        .toList()
        );
    }
}
