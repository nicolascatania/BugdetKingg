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
import jakarta.persistence.EntityNotFoundException;
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

    // ── update() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should revert old amount and apply new amount on EXPENSE update")
    void shouldUpdateExpenseAndAdjustBalance() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.EXPENSE)
                .account(account)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("50.00"), "EXPENSE",
                "Counterparty", "Updated desc", null, null, account.getId(), null, null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(updateDto);

        transactionService.update(transactionId, updateDto);

        // 100 + 30 (revert) - 50 (apply) = 80
        assertEquals(new BigDecimal("80.00"), account.getBalance(), () -> "Balance should reflect only the amount delta");
        assertEquals(new BigDecimal("50.00"), existing.getAmount(), () -> "Stored amount should be updated");
        assertEquals("Updated desc", existing.getDescription(), () -> "Description should be updated");
    }

    @Test
    @DisplayName("Should revert old amount and apply new amount on TRANSFER update")
    void shouldUpdateTransferAndAdjustBothBalances() {
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Account destination = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("50.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.TRANSFER)
                .account(source)
                .destinationAccount(destination)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("300.00"), "TRANSFER",
                "Counterparty", "Bigger transfer", null, null, source.getId(), destination.getId(), null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(updateDto);

        transactionService.update(transactionId, updateDto);

        // source: 100 + 200 (revert) - 300 (apply) = 0
        assertEquals(new BigDecimal("0.00"), source.getBalance(), () -> "Source balance should reflect the new amount");
        // destination: 50 - 200 (revert) + 300 (apply) = 150
        assertEquals(new BigDecimal("150.00"), destination.getBalance(), () -> "Destination balance should reflect the new amount");
    }

    @Test
    @DisplayName("Should reject update that changes the account")
    void shouldRejectAccountChangeOnUpdate() {
        Account originalAccount = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.EXPENSE)
                .account(originalAccount)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("30.00"), "EXPENSE",
                "Counterparty", "Desc", null, null, UUID.randomUUID(), null, null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.update(transactionId, updateDto),
                () -> "Should reject account changes");
        assertEquals(new BigDecimal("100.00"), originalAccount.getBalance(), () -> "Balance must stay untouched");
    }

    @Test
    @DisplayName("Should reject update that changes the type")
    void shouldRejectTypeChangeOnUpdate() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.INCOME)
                .account(account)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("30.00"), "TRANSFER",
                "Counterparty", "Desc", null, null, account.getId(), UUID.randomUUID(), null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.update(transactionId, updateDto),
                () -> "Should reject type changes");
    }

    @Test
    @DisplayName("Should reject update that changes the destination account")
    void shouldRejectDestinationAccountChangeOnUpdate() {
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Account originalDestination = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("50.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.TRANSFER)
                .account(source)
                .destinationAccount(originalDestination)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("30.00"), "TRANSFER",
                "Counterparty", "Desc", null, null, source.getId(), UUID.randomUUID(), null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.update(transactionId, updateDto),
                () -> "Should reject destination account changes");
    }

    @Test
    @DisplayName("Should throw not found when updating another user's transaction")
    void shouldThrowNotFoundWhenUpdatingAnotherUsersTransaction() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> transactionService.update(transactionId, mockDto),
                () -> "Should not find another user's transaction");
    }

    // ── deleteById() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete EXPENSE transaction and revert account balance")
    void shouldDeleteExpenseAndRevertBalance() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("20.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("80.00"))
                .type(TransactionType.EXPENSE)
                .account(account)
                .user(mockUser)
                .build();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        transactionService.deleteById(transactionId);

        assertEquals(new BigDecimal("100.00"), account.getBalance(), () -> "Balance should be restored");
        verify(transactionRepository).delete(existing);
    }

    @Test
    @DisplayName("Should delete TRANSFER transaction and revert both balances")
    void shouldDeleteTransferAndRevertBothBalances() {
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("50.00")).build();
        Account destination = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.TRANSFER)
                .account(source)
                .destinationAccount(destination)
                .user(mockUser)
                .build();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        transactionService.deleteById(transactionId);

        assertEquals(new BigDecimal("100.00"), source.getBalance(), () -> "Source balance should be restored");
        assertEquals(new BigDecimal("50.00"), destination.getBalance(), () -> "Destination balance should be restored");
        verify(transactionRepository).delete(existing);
    }

    @Test
    @DisplayName("Should throw not found when deleting another user's transaction")
    void shouldThrowNotFoundWhenDeletingAnotherUsersTransaction() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> transactionService.deleteById(transactionId),
                () -> "Should not find another user's transaction");
        verify(transactionRepository, never()).delete(any());
    }
}