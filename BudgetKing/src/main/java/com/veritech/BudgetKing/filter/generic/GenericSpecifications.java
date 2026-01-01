package com.veritech.BudgetKing.filter.generic;

import org.springframework.data.jpa.domain.Specification;


public class GenericSpecifications {

    public static <T> Specification<T> greaterOrEqual(String field, Number value) {
        return (root, query, cb) -> cb.ge(root.get(field), value);
    }

    public static <T> Specification<T> lessOrEqual(String field, Number value) {
        return (root, query, cb) -> cb.le(root.get(field), value);
    }

    /**
     * Allows nulls, inclusive or not depending on last parameter
     * x = 10 & y = null => values > 10
     * x = 10 & y = 20 => 10 < values < 20
     * both null => empty
     */
    public static <T> Specification<T> between(String field, Number min, Number max, boolean inclusive) {
        return (root, query, cb) -> {
            if (min == null && max == null) return cb.conjunction();

            if (min != null && max != null) {
                return inclusive
                        ? cb.and(cb.ge(root.get(field), min), cb.le(root.get(field), max))
                        : cb.and(cb.gt(root.get(field), min), cb.lt(root.get(field), max));
            } else if (min != null) {
                return inclusive ? cb.ge(root.get(field), min) : cb.gt(root.get(field), min);
            } else { // max != null
                return inclusive ? cb.le(root.get(field), max) : cb.lt(root.get(field), max);
            }
        };
    }



    public static <T> Specification<T> likeIgnoreCase(String field, String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }

    public static <T> Specification<T> equalIgnoreCase(String field, String value) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(field)), value.toLowerCase());
    }
}
