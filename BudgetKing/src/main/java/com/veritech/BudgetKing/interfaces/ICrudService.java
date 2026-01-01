package com.veritech.BudgetKing.interfaces;

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

    List<D> search(FILTER filter);
}
