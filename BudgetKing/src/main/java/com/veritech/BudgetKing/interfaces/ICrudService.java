package com.veritech.BudgetKing.interfaces;

import com.veritech.BudgetKing.dto.OptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ICrudService<D, ID, FILTER> {

    D getById(ID id);

    @Transactional
    D create(D dto);

    @Transactional
    D update(ID id, D dto);

    @Transactional
    void deleteById(ID id);

    Page<D> search(FILTER filter);

    List<OptionDTO> getOptions();
}
