package com.veritech.BudgetKing.interfaces;

public interface ICrudMapper<E, D> {

    D toDto(E entity);

    E toEntity(D dto);

    void updateEntity(E entity, D dto);
}
