package com.veritech.BudgetKing.filter;

import com.veritech.BudgetKing.filter.generic.GenericSpecifications;
import com.veritech.BudgetKing.filter.generic.PageableFilter;
import com.veritech.BudgetKing.filter.generic.SpecificationFilter;
import com.veritech.BudgetKing.model.AppUser;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;


@Data
public class AppUserFilter extends PageableFilter implements SpecificationFilter<AppUser> {

    private String firstName;
    private String lastName;
    private String phone;
    private String email;


    @Override
    public Specification<AppUser> toSpecification() {
        Specification<AppUser> spec = Specification.where((from, criteriaBuilder) -> criteriaBuilder.conjunction()); // base vacía

        if (firstName != null && !firstName.isBlank()) {
            spec = spec.and(GenericSpecifications.likeIgnoreCase("firstName", firstName));
        }
        if (lastName != null && !lastName.isBlank()) {
            spec = spec.and(GenericSpecifications.likeIgnoreCase("lastName", lastName));
        }
        if (phone != null && !phone.isBlank()) {
            spec = spec.and(GenericSpecifications.likeIgnoreCase("phone", phone));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and(GenericSpecifications.likeIgnoreCase("email", email));
        }

        return spec;
    }

    @Override
    public Specification<AppUser> toSpecification(AppUser user) {
        return null;
    }
}
