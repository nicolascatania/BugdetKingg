package com.veritech.BudgetKing.model;

import com.veritech.BudgetKing.security.enumerator.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Null for a {@code GOOGLE} user — there is no password of ours to check.
     * Kept on the entity (rather than dropped) so the {@code LOCAL} path in
     * {@link com.veritech.BudgetKing.security.controller.AuthController} keeps
     * compiling while it is feature-flagged off.
     */
    private String passwordHash;

    /** How this user signs in. Currently always {@code GOOGLE} — see {@link AuthProvider}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.GOOGLE;

    /** Google's stable subject id ("sub" claim). Null for a {@code LOCAL} user. */
    @Column(name = "provider_id", unique = true)
    private String providerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lastName;

    /** Google profile photo URL. Null for a {@code LOCAL} user. */
    private String picture;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
