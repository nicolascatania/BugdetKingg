package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
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
@DisplayName("Transaction Service Specification")
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

    @Mock
    private CategoryService categoryService;

    private AppUser mockUser;
    private Category mockCategory;
    private TransactionDTO mockDto;
    private Transaction mockTransaction;
    private UUID transactionId;
    private Account mockAccount;
    private final BigDecimal initialAmount = new BigDecimal("15.00");

    @BeforeEach
    void setUpDefaults() {
        transactionId = UUID.randomUUID();
        mockUser = new AppUser();
        mockAccount = new Account();
        UUID categoryId = UUID.randomUUID();

        mockCategory = Category.builder()
                .id(categoryId)
                .name("Entertainment")
                .user(mockUser)
                .build();

        mockTransaction = Transaction.builder()
                .id(transactionId)
                .date(LocalDateTime.now())
                .amount(initialAmount)
                .type(TransactionType.EXPENSE)
                .description("Some Description")
                .category(mockCategory)
                .account(mockAccount)
                .user(mockUser)
                .build();

        mockDto = new TransactionDTO(
                transactionId,
                LocalDateTime.now().toString(),
                initialAmount,
                TransactionType.EXPENSE.name(),
                "Counterparty",
                "Some Description",
                categoryId,
                mockCategory.getName(),
                mockAccount.getId(),
                null,
                mockAccount.getName()
        );
    }

    @Test
    @DisplayName("Should retrieve transaction successfully by ID")
    void shouldGetByIdSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(mockTransaction));
        when(transactionMapper.toDto(mockTransaction)).thenReturn(mockDto);

        TransactionDTO result = transactionService.getById(transactionId);

        assertNotNull(result, () -> "Result should not be null");
        assertEquals("Some Description", result.description(), () -> "Description mismatch");
        verify(transactionRepository).findByIdAndUser(transactionId, mockUser);
    }

    @Test
    @DisplayName("Should throw exception when transfer is missing destination account")
    void shouldThrowExceptionWhenTransferMissingDestination() {
        TransactionDTO dto = new TransactionDTO(transactionId, null, initialAmount, "TRANSFER", null, null, null, null, mockAccount.getId(), null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, mockAccount, null),
                () -> "Should require destination for transfers");

        assertEquals("Destination account is required for TRANSFER", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when transfer source and destination are the same")
    void shouldThrowExceptionWhenTransferAccountsAreSame() {
        Account sameAccount = Account.builder().id(UUID.randomUUID()).build();
        TransactionDTO dto = new TransactionDTO(transactionId, null, initialAmount, "TRANSFER", null, null, null, null, sameAccount.getId(), sameAccount.getId(), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, sameAccount, sameAccount),
                () -> "Source and destination accounts must be distinct");

        assertEquals("Source and destination accounts must be different", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for non-positive transaction amounts")
    void shouldThrowExceptionForNegativeAmount() {
        TransactionDTO dto = new TransactionDTO(transactionId, null, new BigDecimal("-15.00"), "EXPENSE", null, null, null, null, mockAccount.getId(), null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, mockAccount, null),
                () -> "Amount must be positive");

        assertEquals("Amount must be greater than zero", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for invalid transaction types")
    void shouldThrowExceptionForInvalidType() {
        TransactionDTO dto = new TransactionDTO(transactionId, null, initialAmount, "INVALID_TYPE", null, null, null, null, mockAccount.getId(), null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.applyBalanceChanges(dto, mockAccount, null),
                () -> "Should reject invalid types");

        assertEquals("Transaction type not valid: INVALID_TYPE", ex.getMessage());
    }

    @Test
    @DisplayName("Should decrease balance on expense")
    void shouldDecreaseBalanceOnExpense() {
        Account source = Account.builder().balance(new BigDecimal("100.00")).build();
        TransactionDTO dto = createDto("EXPENSE", "30.00");

        transactionService.applyBalanceChanges(dto, source, null);

        assertEquals(new BigDecimal("70.00"), source.getBalance(), () -> "Balance should decrease");
    }

    @Test
    @DisplayName("Should increase balance on income")
    void shouldIncreaseBalanceOnIncome() {
        Account source = Account.builder().balance(new BigDecimal("100.00")).build();
        TransactionDTO dto = createDto("INCOME", "50.00");

        transactionService.applyBalanceChanges(dto, source, null);

        assertEquals(new BigDecimal("150.00"), source.getBalance(), () -> "Balance should increase");
    }

    @Test
    @DisplayName("Should update both balances on transfer")
    void shouldUpdateBothBalancesOnTransfer() {
        Account source = Account.builder().balance(new BigDecimal("100.00")).build();
        Account destination = Account.builder().balance(new BigDecimal("50.00")).build();
        TransactionDTO dto = createDto("TRANSFER", "40.00");

        transactionService.applyBalanceChanges(dto, source, destination);

        assertEquals(new BigDecimal("60.00"), source.getBalance(), () -> "Source balance mismatch");
        assertEquals(new BigDecimal("90.00"), destination.getBalance(), () -> "Destination balance mismatch");
    }

    @Test
    @DisplayName("Should create transaction and update balance successfully")
    void shouldCreateTransactionSuccessfully() {
        Account source = Account.builder().id(mockAccount.getId()).balance(new BigDecimal("100.00")).build();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(any())).thenReturn(source);
        when(transactionMapper.toEntity(any(), any())).thenReturn(mockTransaction);
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toDto(any())).thenReturn(mockDto);

        TransactionDTO result = transactionService.create(mockDto);

        assertNotNull(result, () -> "Result should not be null");
        assertEquals(new BigDecimal("85.00"), source.getBalance(), () -> "Balance update mismatch");
        verify(transactionRepository).save(any());
    }

    private TransactionDTO createDto(String type, String amount) {
        return new TransactionDTO(UUID.randomUUID(), LocalDateTime.now().toString(), new BigDecimal(amount), type, "Counterparty", "Desc", UUID.randomUUID(), "Cat", UUID.randomUUID(), null, null);
    }
}