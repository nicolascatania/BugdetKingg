package com.veritech.BudgetKing.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;



public interface ICrudService<D, ID, FILTER> {

    D getById(ID id);

    @Transactional
    D create(D dto);

    @Transactional
    D update(ID id, D dto);

    @Transactional
    void deleteById(ID id);

    Page<D> search(FILTER filter);
}
