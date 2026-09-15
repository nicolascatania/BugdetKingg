package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.DashBoardDTO;
import com.veritech.BudgetKing.dto.IncomeExpenseDTO;
import com.veritech.BudgetKing.dto.LastMovesDTO;
import com.veritech.BudgetKing.dto.MonthComparisonDTO;
import com.veritech.BudgetKing.dto.MonthlyTransactionReportDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.filter.DashBoardFilter;
import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.SavingsGoalRuntimeException;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.SavingsGoal;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
                mockAccount.getName(),
                null,
                null
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
        TransactionDTO dto = new TransactionDTO(transactionId, null, initialAmount, "TRANSFER", null, null, null, null, mockAccount.getId(), null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, mockAccount, null),
                () -> "Should require destination for transfers");

        assertEquals("Destination account is required for TRANSFER", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when transfer source and destination are the same")
    void shouldThrowExceptionWhenTransferAccountsAreSame() {
        Account sameAccount = Account.builder().id(UUID.randomUUID()).build();
        TransactionDTO dto = new TransactionDTO(transactionId, null, initialAmount, "TRANSFER", null, null, null, null, sameAccount.getId(), sameAccount.getId(), null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, sameAccount, sameAccount),
                () -> "Source and destination accounts must be distinct");

        assertEquals("Source and destination accounts must be different", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for non-positive transaction amounts")
    void shouldThrowExceptionForNegativeAmount() {
        TransactionDTO dto = new TransactionDTO(transactionId, null, new BigDecimal("-15.00"), "EXPENSE", null, null, null, null, mockAccount.getId(), null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, mockAccount, null),
                () -> "Amount must be positive");

        assertEquals("Amount must be greater than zero", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for invalid transaction types")
    void shouldThrowExceptionForInvalidType() {
        TransactionDTO dto = new TransactionDTO(transactionId, null, initialAmount, "INVALID_TYPE", null, null, null, null, mockAccount.getId(), null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransaction(dto, mockAccount, null),
                () -> "Should reject invalid types");

        assertEquals("Transaction type not valid: INVALID_TYPE", ex.getMessage());
    }

    @Test
    @DisplayName("Should decrease balance on expense")
    void shouldDecreaseBalanceOnExpense() {
        Account source = Account.builder().balance(new BigDecimal("100.00")).build();
        transactionService.applyBalanceChanges(TransactionType.EXPENSE, new BigDecimal("30.00"), source, null, null);

        assertEquals(new BigDecimal("70.00"), source.getBalance(), () -> "Balance should decrease");
    }

    @Test
    @DisplayName("Should increase balance on income")
    void shouldIncreaseBalanceOnIncome() {
        Account source = Account.builder().balance(new BigDecimal("100.00")).build();
        transactionService.applyBalanceChanges(TransactionType.INCOME, new BigDecimal("50.00"), source, null, null);

        assertEquals(new BigDecimal("150.00"), source.getBalance(), () -> "Balance should increase");
    }

    @Test
    @DisplayName("Should update both balances on transfer")
    void shouldUpdateBothBalancesOnTransfer() {
        Account source = Account.builder().balance(new BigDecimal("100.00")).build();
        Account destination = Account.builder().balance(new BigDecimal("50.00")).build();
        transactionService.applyBalanceChanges(TransactionType.TRANSFER, new BigDecimal("40.00"), source, destination, null);

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
                "Counterparty", "Updated desc", null, null, account.getId(), null, null, null, null
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
                "Counterparty", "Bigger transfer", null, null, source.getId(), destination.getId(), null, null, null
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
                "Counterparty", "Desc", null, null, UUID.randomUUID(), null, null, null, null
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
                "Counterparty", "Desc", null, null, account.getId(), UUID.randomUUID(), null, null, null
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
                "Counterparty", "Desc", null, null, source.getId(), UUID.randomUUID(), null, null, null
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
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
    // ── savings movements ────────────────────────────────────────────────

    private SavingsGoal activeGoal(String currentAmount, String target) {
        return SavingsGoal.builder()
                .id(UUID.randomUUID())
                .name("Vacation")
                .icon("fa fa-plane")
                .targetAmount(new BigDecimal(target))
                .targetDate(LocalDate.now().plusMonths(3))
                .currentAmount(new BigDecimal(currentAmount))
                .status(SavingsGoalStatus.ACTIVE)
                .user(mockUser)
                .build();
    }

    @Test
    @DisplayName("Should move money from the account into the goal on SAVINGS_DEPOSIT")
    void shouldApplySavingsDeposit() {
        Account source = Account.builder().balance(new BigDecimal("300.00")).build();
        SavingsGoal goal = activeGoal("0.00", "100.00");

        transactionService.applyBalanceChanges(TransactionType.SAVINGS_DEPOSIT, new BigDecimal("50.00"), source, null, goal);

        assertEquals(new BigDecimal("250.00"), source.getBalance(), () -> "Account should drop by the deposit");
        assertEquals(new BigDecimal("50.00"), goal.getCurrentAmount(), () -> "Goal should grow by the deposit");
        assertFalse(goal.isAchieved(), () -> "Half way is not achieved");
    }

    @Test
    @DisplayName("Should flag the goal achieved when a deposit reaches or exceeds the target")
    void shouldFlagAchievedOnOverSaving() {
        Account source = Account.builder().balance(new BigDecimal("300.00")).build();
        SavingsGoal goal = activeGoal("90.00", "100.00");

        transactionService.applyBalanceChanges(TransactionType.SAVINGS_DEPOSIT, new BigDecimal("20.00"), source, null, goal);

        assertEquals(new BigDecimal("110.00"), goal.getCurrentAmount(), () -> "Over-saving is allowed");
        assertTrue(goal.isAchieved(), () -> "Target covered means achieved");
    }

    @Test
    @DisplayName("Should move money from the goal into the account on SAVINGS_WITHDRAWAL")
    void shouldApplySavingsWithdrawal() {
        Account destination = Account.builder().balance(new BigDecimal("10.00")).build();
        SavingsGoal goal = activeGoal("50.00", "100.00");

        transactionService.applyBalanceChanges(TransactionType.SAVINGS_WITHDRAWAL, new BigDecimal("20.00"), destination, null, goal);

        assertEquals(new BigDecimal("30.00"), destination.getBalance(), () -> "Account should grow by the withdrawal");
        assertEquals(new BigDecimal("30.00"), goal.getCurrentAmount(), () -> "Goal should drop by the withdrawal");
    }

    @Test
    @DisplayName("Should revert a SAVINGS_DEPOSIT by giving the money back to the account")
    void shouldRevertSavingsDeposit() {
        Account source = Account.builder().balance(new BigDecimal("250.00")).build();
        SavingsGoal goal = activeGoal("50.00", "100.00");

        transactionService.revertBalanceChanges(TransactionType.SAVINGS_DEPOSIT, new BigDecimal("50.00"), source, null, goal);

        assertEquals(new BigDecimal("300.00"), source.getBalance(), () -> "Account should regain the deposit");
        assertEquals(new BigDecimal("0.00"), goal.getCurrentAmount(), () -> "Goal should be empty again");
    }

    @Test
    @DisplayName("Should revert a SAVINGS_WITHDRAWAL by putting the money back into the goal")
    void shouldRevertSavingsWithdrawal() {
        Account destination = Account.builder().balance(new BigDecimal("30.00")).build();
        SavingsGoal goal = activeGoal("30.00", "100.00");

        transactionService.revertBalanceChanges(TransactionType.SAVINGS_WITHDRAWAL, new BigDecimal("20.00"), destination, null, goal);

        assertEquals(new BigDecimal("10.00"), destination.getBalance(), () -> "Account should give the withdrawal back");
        assertEquals(new BigDecimal("50.00"), goal.getCurrentAmount(), () -> "Goal should regain the withdrawal");
    }

    @Test
    @DisplayName("Should refuse a movement that would leave the goal negative")
    void shouldRejectNegativeGoalBalance() {
        Account account = Account.builder().balance(new BigDecimal("0.00")).build();
        SavingsGoal goal = activeGoal("20.00", "100.00");

        assertThrows(SavingsGoalRuntimeException.class,
                () -> transactionService.applyBalanceChanges(TransactionType.SAVINGS_WITHDRAWAL, new BigDecimal("50.00"), account, null, goal),
                () -> "A goal can never hold a negative amount");
        assertEquals(new BigDecimal("20.00"), goal.getCurrentAmount(), () -> "Goal must stay untouched");
    }

    @Test
    @DisplayName("Should refuse any movement on a closed goal")
    void shouldRejectMovementOnClosedGoal() {
        Account account = Account.builder().balance(new BigDecimal("100.00")).build();
        SavingsGoal goal = activeGoal("0.00", "100.00");
        goal.setStatus(SavingsGoalStatus.CLOSED);

        assertThrows(SavingsGoalRuntimeException.class,
                () -> transactionService.applyBalanceChanges(TransactionType.SAVINGS_DEPOSIT, new BigDecimal("10.00"), account, null, goal),
                () -> "Closed goals are frozen");
    }

    @Test
    @DisplayName("Should reject creating a savings movement through the generic endpoint")
    void shouldRejectGenericCreateOfSavingsType() {
        Account source = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        TransactionDTO dto = new TransactionDTO(
                null, LocalDateTime.now().toString(), new BigDecimal("10.00"), "SAVINGS_DEPOSIT",
                "Vacation", "Savings", null, null, source.getId(), null, null, UUID.randomUUID(), null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(source.getId())).thenReturn(source);

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.create(dto),
                () -> "Savings movements only come from the savings goal endpoints");
        assertEquals(new BigDecimal("100.00"), source.getBalance(), () -> "Balance must stay untouched");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete a SAVINGS_DEPOSIT and give the money back to the account")
    void shouldDeleteSavingsDeposit() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("250.00")).build();
        SavingsGoal goal = activeGoal("50.00", "100.00");
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.SAVINGS_DEPOSIT)
                .account(account)
                .savingsGoal(goal)
                .user(mockUser)
                .build();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        transactionService.deleteById(transactionId);

        assertEquals(new BigDecimal("300.00"), account.getBalance(), () -> "Account should regain the deposit");
        assertEquals(new BigDecimal("0.00"), goal.getCurrentAmount(), () -> "Goal should be empty");
        verify(transactionRepository).delete(existing);
    }

    @Test
    @DisplayName("Should refuse deleting a deposit the goal has already partly withdrawn")
    void shouldRejectDeletingDepositAfterWithdrawal() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("280.00")).build();
        SavingsGoal goal = activeGoal("20.00", "100.00"); // 50 deposited, 30 withdrawn since
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.SAVINGS_DEPOSIT)
                .account(account)
                .savingsGoal(goal)
                .user(mockUser)
                .build();
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        assertThrows(SavingsGoalRuntimeException.class,
                () -> transactionService.deleteById(transactionId),
                () -> "Removing the deposit would leave the goal negative");
        assertEquals(new BigDecimal("20.00"), goal.getCurrentAmount(), () -> "Goal must stay untouched");
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    @DisplayName("Should reject update that relinks a movement to another goal")
    void shouldRejectSavingsGoalChangeOnUpdate() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("100.00")).build();
        SavingsGoal goal = activeGoal("50.00", "100.00");
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.SAVINGS_DEPOSIT)
                .account(account)
                .savingsGoal(goal)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("50.00"), "SAVINGS_DEPOSIT",
                "Vacation", "Desc", null, null, account.getId(), null, null, UUID.randomUUID(), null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.update(transactionId, updateDto),
                () -> "The goal of a movement is immutable");
        assertEquals(new BigDecimal("50.00"), goal.getCurrentAmount(), () -> "Goal must stay untouched");
    }

    @Test
    @DisplayName("Should adjust account and goal when editing the amount of a SAVINGS_DEPOSIT")
    void shouldUpdateSavingsDepositAmount() {
        Account account = Account.builder().id(UUID.randomUUID()).balance(new BigDecimal("250.00")).build();
        SavingsGoal goal = activeGoal("50.00", "100.00");
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.SAVINGS_DEPOSIT)
                .account(account)
                .savingsGoal(goal)
                .user(mockUser)
                .build();
        TransactionDTO updateDto = new TransactionDTO(
                transactionId, LocalDateTime.now().toString(), new BigDecimal("80.00"), "SAVINGS_DEPOSIT",
                "Vacation", "Bigger share", null, null, account.getId(), null, null, goal.getId(), null
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByIdAndUser(transactionId, mockUser)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(updateDto);

        transactionService.update(transactionId, updateDto);

        // account: 250 + 50 (revert) - 80 (apply) = 220 ; goal: 50 - 50 + 80 = 80
        assertEquals(new BigDecimal("220.00"), account.getBalance(), () -> "Account should reflect the delta");
        assertEquals(new BigDecimal("80.00"), goal.getCurrentAmount(), () -> "Goal should reflect the delta");
    }

    @Test
    @DisplayName("Should report the range's net balance as income minus expense on the dashboard")
    void shouldReportNetBalanceOnDashboard() {
        DashBoardFilter filter = new DashBoardFilter();
        filter.setDateFrom("2026-03-01");
        filter.setDateTo("2026-03-31");
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.getIncomeAndExpense(eq(mockUser), any(), any()))
                .thenReturn(new IncomeExpenseDTO(new BigDecimal("1000.00"), new BigDecimal("1250.00")));
        when(transactionRepository.getExpensesByCategoryWithIcon(eq(mockUser), any(), any())).thenReturn(List.of());
        when(accountService.getAccounts()).thenReturn(List.of());

        DashBoardDTO result = transactionService.getDataForDashBoard(filter);

        assertEquals(0, new BigDecimal("-250.00").compareTo(result.netBalance()), () -> "Net must go negative when expenses exceed income");
        assertEquals(0, new BigDecimal("1250.00").compareTo(result.expense()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.income()));
    }

    @Test
    @DisplayName("Should list the movements of a date range most recent first")
    void shouldListMovementsBetween() {
        Transaction older = Transaction.builder().id(UUID.randomUUID()).date(LocalDateTime.of(2026, 3, 2, 9, 0)).build();
        Transaction newer = Transaction.builder().id(UUID.randomUUID()).date(LocalDateTime.of(2026, 3, 20, 9, 0)).build();
        LastMovesDTO olderDto = new LastMovesDTO(older.getId(), "02/03/2026 09:00", BigDecimal.ONE, "EXPENSE", "a", "b", "c", "d");
        LastMovesDTO newerDto = new LastMovesDTO(newer.getId(), "20/03/2026 09:00", BigDecimal.ONE, "EXPENSE", "a", "b", "c", "d");
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.findByUserAndDateBetween(
                mockUser, LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 4, 1, 0, 0)))
                .thenReturn(List.of(older, newer));
        when(transactionMapper.toLastMovesDTO(older)).thenReturn(olderDto);
        when(transactionMapper.toLastMovesDTO(newer)).thenReturn(newerDto);

        List<LastMovesDTO> result = transactionService.movementsBetween("2026-03-01", "2026-03-31");

        assertEquals(List.of(newerDto, olderDto), result, () -> "The end of the range is inclusive and newest comes first");
    }

    @Test
    @DisplayName("Should compare the current month against the whole previous month")
    void shouldCompareCurrentAndPreviousMonth() {
        LocalDateTime currentStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(transactionRepository.getMonthlyReport(mockUser, currentStart, currentStart.plusMonths(1)))
                .thenReturn(new MonthlyTransactionReportDTO(new BigDecimal("500.00"), new BigDecimal("300.00")));
        when(transactionRepository.getMonthlyReport(mockUser, currentStart.minusMonths(1), currentStart))
                .thenReturn(new MonthlyTransactionReportDTO(new BigDecimal("400.00"), new BigDecimal("350.00")));

        MonthComparisonDTO result = transactionService.getMonthComparison();

        assertEquals(0, new BigDecimal("500.00").compareTo(result.currentIncome()));
        assertEquals(0, new BigDecimal("300.00").compareTo(result.currentExpense()));
        assertEquals(0, new BigDecimal("400.00").compareTo(result.previousIncome()));
        assertEquals(0, new BigDecimal("350.00").compareTo(result.previousExpense()));
        assertEquals(0, new BigDecimal("200.00").compareTo(result.currentNet()), () -> "Current net mismatch");
        assertEquals(0, new BigDecimal("50.00").compareTo(result.previousNet()), () -> "Previous net mismatch");
    }
}
