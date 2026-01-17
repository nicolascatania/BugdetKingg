package com.veritech.BudgetKing.repository;

import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

@DataJpaTest
public abstract class BaseRepositoryTest {

    @Autowired
    protected TestEntityManager entityManager;

    protected AppUser savedUser;
    protected Account savedAccount;
    protected Category savedCategory;

    @BeforeEach
    void setUp() {

        AppUser user = AppUser.builder()
                .name("Nicolas")
                .email("nico@budgetking.com")
                .lastName("Veritech")
                .passwordHash("hash123")
                .roles(new HashSet<>())
                .build();
        this.savedUser = entityManager.persistFlushFind(user);

        Account account = Account.builder()
                .name("Main Bank")
                .balance(new BigDecimal("1000.00"))
                .user(savedUser)
                .build();
        this.savedAccount = entityManager.persistFlushFind(account);


        Category category = Category.builder()
                .name("Entertainment")
                .description("Movies, Games, etc.")
                .user(savedUser)
                .build();
        this.savedCategory = entityManager.persistFlushFind(category);


        Transaction t1 = Transaction.builder()
                .amount(new BigDecimal("50.00"))
                .description("Netflix")
                .category(savedCategory)
                .user(savedUser)
                .account(savedAccount)
                .type(TransactionType.INCOME)
                .date(LocalDateTime.now())
                .build();

        Transaction t2 = Transaction.builder()
                .amount(new BigDecimal("20.00"))
                .description("Steam Game")
                .category(savedCategory)
                .user(savedUser)
                .account(savedAccount)
                .type(TransactionType.INCOME)
                .date(LocalDateTime.now())
                .build();

        entityManager.persist(t1);
        entityManager.persist(t2);


        entityManager.flush();
        entityManager.clear();
    }
}