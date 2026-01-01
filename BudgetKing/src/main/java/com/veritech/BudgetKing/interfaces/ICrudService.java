package com.veritech.BudgetKing.interfaces;

import java.util.List;

public interface ICrudService<E, ID, FILTER> {

    E getById(ID id);

    E create(E entity);

    E update(ID id, E entity);

    void deleteById(ID id);

    List<E> search(FILTER filter);
}
