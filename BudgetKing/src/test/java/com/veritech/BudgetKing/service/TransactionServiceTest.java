package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.CategoryDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.BaseRepositoryTest;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {


    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TransactionMapper transactionMapper;


    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;


    private AppUser mockUser;
    private Category mockCategory;
    private TransactionDTO mockDto;
    private Transaction mockTransaction;
    private UUID transactionId;
    private UUID categoryId;
    private Account mockAccount;


    @BeforeEach
    void setUp() {

        transactionId = UUID.randomUUID();
        mockUser = new AppUser();
        mockAccount = new Account();

        mockCategory = Category.builder()
                .id(categoryId)
                .name("Entertainment")
                .user(mockUser)
                .build();

        mockTransaction =  Transaction.builder()
                .id(transactionId)
                .date(LocalDateTime.now())
                .amount(new java.math.BigDecimal("15.00"))
                .type(TransactionType.EXPENSE)
                .counterparty("Some Counterparty")
                .description("Some Description")
                .category(mockCategory)
                .account(mockAccount)
                .user(mockUser)
                .build();

        mockDto = new TransactionDTO(
                transactionId,
                LocalDateTime.now().toString(),
                new java.math.BigDecimal("15.00"),
                TransactionType.EXPENSE.name(),
                "Some Counterparty",
                "Some Description",
                mockCategory.getId(),
                mockCategory.getName(),
                mockAccount.getId(),
                null,
                mockAccount.getName()
        );

    }



    @Test
    void getByID_Success(){
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.ofNullable(mockTransaction));
        when(transactionMapper.toDto(mockTransaction)).thenReturn(mockDto);


        TransactionDTO result = transactionService.getById(transactionId);

        assertNotNull(result);
        assertEquals("Some Description", result.description());
        verify(transactionRepository).findByIdAndUser(transactionId, mockUser);

    }


    @Test
    void validateTransaction_InvalidTransferNoDestination_ThrowsException() {
        TransactionDTO dto = new TransactionDTO(
                transactionId,
                LocalDateTime.now().toString(),
                new java.math.BigDecimal("15.00"),
                "TRANSFER",
                "Some Counterparty",
                "Some Description",
                mockCategory.getId(),
                mockCategory.getName(),
                mockAccount.getId(),
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.validateTransaction(dto, mockAccount, null);
        });

        assertEquals("Destination account is required for TRANSFER", exception.getMessage());
    }

    @Test
    void validateTransaction_InvalidTransferAccountFromEqualsAccountTO_ThrowsException() {
        UUID sameId = UUID.randomUUID();
        Account account = new Account();
        account.setId(sameId);

        TransactionDTO dto = new TransactionDTO(
                transactionId,
                LocalDateTime.now().toString(),
                new java.math.BigDecimal("15.00"),
                "TRANSFER",
                "Some Counterparty",
                "Some Description",
                UUID.randomUUID(), // Category ID
                "Category Name",
                sameId, // Source ID
                sameId, // Destination ID
                null
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.validateTransaction(dto, account, account);
        });

        assertEquals("Source and destination accounts must be different", exception.getMessage());
    }

    @Test
    void validateTransaction_NegativeAmmount_ThrowsException() {
        TransactionDTO dto = new TransactionDTO(
                transactionId,
                LocalDateTime.now().toString(),
                new java.math.BigDecimal("-15.00"),
                "EXPENSE",
                "Some Counterparty",
                "Some Description",
                mockCategory.getId(),
                mockCategory.getName(),
                mockAccount.getId(),
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.validateTransaction(dto, mockAccount, null);
        });

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }


    @Test
    void applyBalanceChanges_InvalidType_ThrowsException() {
        TransactionDTO dto = new TransactionDTO(
                transactionId,
                LocalDateTime.now().toString(),
                new java.math.BigDecimal("15.00"),
                "INVALID_TYPE",
                "Some Counterparty",
                "Some Description",
                mockCategory.getId(),
                mockCategory.getName(),
                mockAccount.getId(),
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.applyBalanceChanges(dto, mockAccount, null);
        });

        assertEquals("Transaction type not valid: " + dto.type(), exception.getMessage());
    }

    @Test
    void applyBalanceChanges_Expense_DecreasesSourceBalance() {
        // GIVEN
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        TransactionDTO dto = createDto("EXPENSE", "30.00");

        // WHEN
        transactionService.applyBalanceChanges(dto, source, null);

        // THEN
        // 100.00 - 30.00 = 70.00
        assertEquals(new BigDecimal("70.00"), source.getBalance());
    }

    @Test
    void applyBalanceChanges_Income_IncreasesSourceBalance() {
        // GIVEN
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        TransactionDTO dto = createDto("INCOME", "50.00");

        // WHEN
        transactionService.applyBalanceChanges(dto, source, null);

        // THEN
        // 100.00 + 50.00 = 150.00
        assertEquals(new BigDecimal("150.00"), source.getBalance());
    }

    @Test
    void applyBalanceChanges_Transfer_ChangesBothBalances() {
        // GIVEN
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Account destination = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("50.00")).build();
        TransactionDTO dto = createDto("TRANSFER", "40.00");

        // WHEN
        transactionService.applyBalanceChanges(dto, source, destination);

        // THEN
        // From: 100.00 - 40.00 = 60.00
        // To: 50.00 + 40.00 = 90.00
        assertEquals(new BigDecimal("60.00"), source.getBalance());
        assertEquals(new BigDecimal("90.00"), destination.getBalance());
    }

    private TransactionDTO createDto(String type, String amount) {
        return new TransactionDTO(
                UUID.randomUUID(), LocalDateTime.now().toString(),
                new BigDecimal(amount), type, "Counterparty", "Desc",
                UUID.randomUUID(), "Cat", UUID.randomUUID(), null, null
        );
    }

    @Test
    void create_Success() {
        // GIVEN
        Account source = Account.builder().id(mockAccount.getId()).balance(new BigDecimal("100.00")).build();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(any())).thenReturn(source);
        when(transactionMapper.toEntity(any(), any())).thenReturn(mockTransaction);
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toDto(any())).thenReturn(mockDto);

        // WHEN
        TransactionDTO result = transactionService.create(mockDto);

        // THEN
        assertNotNull(result);
        assertEquals(new BigDecimal("85.00"), source.getBalance());
        verify(transactionRepository, times(1)).save(any());
    }
}