package com.veritech.BudgetKing.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // ROLE_ADMIN, ROLE_USER

    @ManyToMany(mappedBy = "roles")
    private Set<AppUser> users = new HashSet<>();
}
