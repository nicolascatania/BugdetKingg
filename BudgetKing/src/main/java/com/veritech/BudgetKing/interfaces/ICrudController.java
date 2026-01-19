package com.veritech.BudgetKing.interfaces;

import com.veritech.BudgetKing.dto.OptionDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



public interface ICrudController<D, ID, FILTER> {

    /**
     * El servicio maneja los DTO directamente
     */
    ICrudService<D, ID, FILTER> getService();

    @GetMapping("/{id}")
    default ResponseEntity<D> getById(@PathVariable ID id) {
        D dto = getService().getById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    default ResponseEntity<D> create(@Valid @RequestBody D dto) {
        D saved = getService().create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    default ResponseEntity<D> update(
            @PathVariable ID id,
            @Valid @RequestBody D dto
    ) {
        D updated = getService().update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    default ResponseEntity<Void> delete(@PathVariable ID id) {
        getService().deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    default ResponseEntity<Page<D>> search(@RequestBody FILTER filter) {
        return ResponseEntity.ok(getService().search(filter));
    }

    @GetMapping("/options")
    default ResponseEntity<List<OptionDTO>> getOptions() {
        return ResponseEntity.ok(getService().getOptions());
    }
}
