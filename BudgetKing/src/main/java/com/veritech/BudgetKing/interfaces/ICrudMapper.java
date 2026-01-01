package com.veritech.BudgetKing.interfaces;

public interface ICrudMapper<E, D, R> {
    D toDto(E entity);
    E toEntity(D dto, R relatedEntities);
}
