package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.SavingsGoalCloseDTO;
import com.veritech.BudgetKing.dto.SavingsGoalContributionDTO;
import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.SavingsGoalRuntimeException;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.mapper.SavingsGoalMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.SavingsGoalRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Balance maths live in {@link TransactionService} and are covered there; these
 * tests check the goal-level rules and that the service delegates with the right
 * arguments. Where a scenario depends on the goal actually changing (close), the
 * mocked {@code applyBalanceChanges} is stubbed to mimic the real effect.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SavingsGoal Service Specification")
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private SavingsGoalMapper savingsGoalMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private AppUser mockUser;
    private Account mockAccount;
    private SavingsGoal mockGoal;
    private SavingsGoalDTO mockDto;
    private UUID goalId;
    private UUID accountId;
    private final String goalName = "Vacation";
    private final BigDecimal targetAmount = new BigDecimal("2000.00");
    private LocalDate targetDate;

    @BeforeEach
    void setUpDefaults() {
        goalId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        targetDate = LocalDate.now().plusMonths(6);

        mockUser = new AppUser();

        mockAccount = Account.builder()
                .id(accountId)
                .name("Main Bank")
                .balance(new BigDecimal("500.00"))
                .user(mockUser)
                .build();

        mockGoal = SavingsGoal.builder()
                .id(goalId)
                .name(goalName)
                .icon("fa fa-plane")
                .targetAmount(targetAmount)
                .targetDate(targetDate)
                .currentAmount(new BigDecimal("500.00"))
                .status(SavingsGoalStatus.ACTIVE)
                .linkedAccount(mockAccount)
                .user(mockUser)
                .achieved(false)
                .build();

        // Mirrors what the real mapper does: persisted fields mapped, derived fields empty.
        mockDto = dto(goalId, goalName, targetAmount, targetDate, accountId);
    }

    /** Builds a create/update payload; derived components are ignored by the service. */
    private SavingsGoalDTO dto(UUID id, String name, BigDecimal target, LocalDate date, UUID linkedAccountId) {
        return new SavingsGoalDTO(
                id, name, "fa fa-plane", target, date,
                linkedAccountId, linkedAccountId != null ? "Main Bank" : null,
                SavingsGoalStatus.ACTIVE, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );
    }

    private void stubOwnedGoal(SavingsGoal goal) {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByIdAndUser(goal.getId(), mockUser)).thenReturn(Optional.of(goal));
    }

    private void stubMappingRoundTrip() {
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(savingsGoalMapper.toDto(any(SavingsGoal.class))).thenReturn(mockDto);
    }

    @Nested
    @DisplayName("Read")
    class Read {

        @Test
        @DisplayName("Should derive progress from the money held by the goal, not from the linked account")
        void shouldGetByIdSuccessfully() {
            stubOwnedGoal(mockGoal);
            when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

            SavingsGoalDTO result = savingsGoalService.getById(goalId);

            assertEquals(new BigDecimal("500.00"), result.currentAmount(), () -> "Current amount should be the goal's own balance");
            assertEquals(new BigDecimal("25.00"), result.progressPercentage(), () -> "Progress percentage mismatch");
            assertEquals(new BigDecimal("1500.00"), result.remainingAmount(), () -> "Remaining amount mismatch");
            assertEquals(new BigDecimal("250.00"), result.monthlyRequired(), () -> "Monthly required mismatch");
            assertEquals(SavingsGoalDTO.STATE_ACTIVE, result.state(), () -> "A funded, in-date goal is ACTIVE");
            assertFalse(result.achieved(), () -> "Goal should not be achieved yet");
        }

        @Test
        @DisplayName("Should report OVERDUE when the target date passed without reaching the target")
        void shouldReportOverdue() {
            mockGoal.setTargetDate(LocalDate.now().minusDays(1));
            stubOwnedGoal(mockGoal);
            when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

            SavingsGoalDTO result = savingsGoalService.getById(goalId);

            assertEquals(SavingsGoalDTO.STATE_OVERDUE, result.state(), () -> "Past date and not covered means OVERDUE");
            assertEquals(new BigDecimal("500.00"), result.currentAmount(), () -> "Money must stay untouched when the date passes");
            assertEquals(0L, result.daysRemaining(), () -> "Days remaining never go negative");
        }

        @Test
        @DisplayName("Should report ACHIEVED over OVERDUE when the target is covered")
        void shouldReportAchieved() {
            mockGoal.setTargetDate(LocalDate.now().minusDays(1));
            mockGoal.setCurrentAmount(targetAmount);
            mockGoal.setAchieved(true);
            stubOwnedGoal(mockGoal);
            when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

            assertEquals(SavingsGoalDTO.STATE_ACHIEVED, savingsGoalService.getById(goalId).state(),
                    () -> "Achieved wins over overdue");
        }

        @Test
        @DisplayName("Should report CLOSED regardless of money or date")
        void shouldReportClosed() {
            mockGoal.setStatus(SavingsGoalStatus.CLOSED);
            stubOwnedGoal(mockGoal);
            when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

            assertEquals(SavingsGoalDTO.STATE_CLOSED, savingsGoalService.getById(goalId).state(),
                    () -> "Closed status maps to the CLOSED state");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when savings goal does not exist")
        void shouldThrowExceptionWhenNotFound() {
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> savingsGoalService.getById(goalId),
                    () -> "Should throw EntityNotFoundException for non-existent savings goal");
        }

        @Test
        @DisplayName("Should return paginated search results")
        void shouldSearchSuccessfully() {
            SavingsGoalFilter filter = new SavingsGoalFilter();
            filter.setPage(0);
            filter.setSize(10);

            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("targetDate").ascending());
            Page<SavingsGoal> goalPage = new PageImpl<>(List.of(mockGoal), expectedPageable, 1);

            when(savingsGoalRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(goalPage);
            when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

            Page<SavingsGoalDTO> result = savingsGoalService.search(filter);

            assertEquals(1, result.getTotalElements(), () -> "Total elements mismatch");
        }

        @Test
        @DisplayName("Should return list of savings goal options")
        void shouldGetOptionsSuccessfully() {
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalRepository.findByUser(mockUser)).thenReturn(List.of(mockGoal));

            List<com.veritech.BudgetKing.dto.OptionDTO> result = savingsGoalService.getOptions();

            assertEquals(1, result.size(), () -> "Options size mismatch");
            assertEquals(goalName, result.get(0).value(), () -> "Option value mismatch");
            verifyNoInteractions(savingsGoalMapper);
        }
    }

    @Nested
    @DisplayName("Create & update")
    class CreateAndUpdate {

        @Test
        @DisplayName("Should create an empty, active goal with a default source account")
        void shouldCreateGoalSuccessfully() {
            SavingsGoal fresh = SavingsGoal.builder()
                    .id(goalId).name(goalName).icon("fa fa-plane").targetAmount(targetAmount)
                    .targetDate(targetDate).linkedAccount(mockAccount).user(mockUser).build();

            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            when(savingsGoalMapper.toEntity(any(), any())).thenReturn(fresh);
            stubMappingRoundTrip();

            SavingsGoalDTO result = savingsGoalService.create(mockDto);

            assertEquals(BigDecimal.ZERO, result.currentAmount(), () -> "A new goal starts empty even with a funded linked account");
            assertFalse(fresh.isAchieved(), () -> "An empty goal is never achieved");
            assertEquals(SavingsGoalStatus.ACTIVE, fresh.getStatus(), () -> "New goals are ACTIVE");
            verify(accountService).getEntityById(accountId);
        }

        @Test
        @DisplayName("Should create savings goal successfully without a linked account")
        void shouldCreateGoalWithoutLinkedAccount() {
            SavingsGoalDTO dtoWithoutAccount = dto(null, goalName, targetAmount, targetDate, null);
            SavingsGoal goalWithoutAccount = SavingsGoal.builder()
                    .id(goalId).name(goalName).icon("fa fa-plane")
                    .targetAmount(targetAmount).targetDate(targetDate)
                    .user(mockUser).build();

            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalMapper.toEntity(any(), any())).thenReturn(goalWithoutAccount);
            when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(goalWithoutAccount);
            when(savingsGoalMapper.toDto(goalWithoutAccount)).thenReturn(dtoWithoutAccount);

            SavingsGoalDTO result = savingsGoalService.create(dtoWithoutAccount);

            assertEquals(BigDecimal.ZERO, result.currentAmount(), () -> "Current amount should be zero when unlinked");
            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("Should throw exception when target amount is not positive")
        void shouldThrowExceptionWhenTargetAmountIsNotPositive() {
            SavingsGoalDTO invalidDto = dto(null, goalName, BigDecimal.ZERO, targetDate, null);
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.create(invalidDto),
                    () -> "Should reject a non-positive target amount");
            verify(savingsGoalRepository, never()).save(any(SavingsGoal.class));
        }

        @Test
        @DisplayName("Should throw exception when target date is in the past")
        void shouldThrowExceptionWhenTargetDateIsInThePast() {
            SavingsGoalDTO invalidDto = dto(null, goalName, targetAmount, LocalDate.now().minusDays(1), null);
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.create(invalidDto),
                    () -> "Should reject a target date already in the past");
            verify(savingsGoalRepository, never()).save(any(SavingsGoal.class));
        }

        @Test
        @DisplayName("Should update savings goal details successfully")
        void shouldUpdateGoalSuccessfully() {
            SavingsGoalDTO updateDto = dto(goalId, "New Name", targetAmount, targetDate, accountId);

            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(mockGoal);
            when(savingsGoalMapper.toDto(any())).thenReturn(updateDto);

            SavingsGoalDTO result = savingsGoalService.update(goalId, updateDto);

            assertEquals("New Name", result.name(), () -> "Goal name should be updated");
            verify(savingsGoalRepository).save(mockGoal);
        }

        @Test
        @DisplayName("Should allow renaming an overdue goal without changing its date")
        void shouldAllowRenamingOverdueGoal() {
            LocalDate pastDate = LocalDate.now().minusDays(3);
            mockGoal.setTargetDate(pastDate);
            SavingsGoalDTO renameDto = dto(goalId, "Renamed", targetAmount, pastDate, accountId);

            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            stubMappingRoundTrip();

            assertDoesNotThrow(() -> savingsGoalService.update(goalId, renameDto),
                    () -> "An unchanged past date must not block editing the rest");
            assertEquals("Renamed", mockGoal.getName(), () -> "Name should be updated");
        }

        @Test
        @DisplayName("Should still reject moving the target date into the past")
        void shouldRejectNewPastDateOnUpdate() {
            SavingsGoalDTO badDate = dto(goalId, goalName, targetAmount, LocalDate.now().minusDays(1), accountId);
            stubOwnedGoal(mockGoal);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.update(goalId, badDate),
                    () -> "A changed date must be in the future");
        }

        @Test
        @DisplayName("Should reject updating a closed goal")
        void shouldRejectUpdatingClosedGoal() {
            mockGoal.setStatus(SavingsGoalStatus.CLOSED);
            stubOwnedGoal(mockGoal);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.update(goalId, mockDto),
                    () -> "Closed goals are read-only");
            verify(savingsGoalRepository, never()).save(any(SavingsGoal.class));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating a non-existent goal")
        void shouldThrowExceptionWhenUpdatingNonExistentGoal() {
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> savingsGoalService.update(goalId, mockDto),
                    () -> "Should throw EntityNotFoundException for non-existent savings goal");
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("Should move money from the account into the goal and record a SAVINGS_DEPOSIT")
        void shouldDeposit() {
            BigDecimal amount = new BigDecimal("50.00");
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, amount, null, null);

            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            stubMappingRoundTrip();

            savingsGoalService.deposit(goalId, request);

            verify(transactionService).applyBalanceChanges(
                    eq(TransactionType.SAVINGS_DEPOSIT), eq(amount), eq(mockAccount), isNull(), eq(mockGoal));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction movement = captor.getValue();
            assertEquals(TransactionType.SAVINGS_DEPOSIT, movement.getType(), () -> "Movement type mismatch");
            assertEquals(amount, movement.getAmount(), () -> "Movement amount mismatch");
            assertSame(mockAccount, movement.getAccount(), () -> "Movement must reference the source account");
            assertSame(mockGoal, movement.getSavingsGoal(), () -> "Movement must reference the goal");
            assertSame(mockUser, movement.getUser(), () -> "Movement belongs to the goal owner");
            assertEquals("Savings · " + goalName, movement.getDescription(), () -> "Default description mismatch");
            assertEquals(goalName, movement.getCounterparty(), () -> "Counterparty is the goal");
            assertNotNull(movement.getDate(), () -> "Date defaults to now");
        }

        @Test
        @DisplayName("Should keep the user's note and date on the movement")
        void shouldKeepNoteAndDate() {
            LocalDateTime when = LocalDateTime.of(2026, 1, 15, 10, 0);
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, new BigDecimal("10.00"), when, "  January share ");

            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            stubMappingRoundTrip();

            savingsGoalService.deposit(goalId, request);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertEquals("January share", captor.getValue().getDescription(), () -> "Note should be trimmed and kept");
            assertEquals(when, captor.getValue().getDate(), () -> "Given date should be kept");
        }

        @Test
        @DisplayName("Should reject a deposit larger than the account balance")
        void shouldRejectInsufficientFunds() {
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, new BigDecimal("500.01"), null, null);
            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.deposit(goalId, request),
                    () -> "Cannot set aside more than the account holds");
            verifyNoInteractions(transactionService, transactionRepository);
        }

        @Test
        @DisplayName("Should reject a non-positive deposit")
        void shouldRejectNonPositiveAmount() {
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, BigDecimal.ZERO, null, null);
            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.deposit(goalId, request),
                    () -> "Amount must be positive");
            verifyNoInteractions(transactionService, transactionRepository);
        }

        @Test
        @DisplayName("Should reject a deposit into a closed goal")
        void shouldRejectDepositIntoClosedGoal() {
            mockGoal.setStatus(SavingsGoalStatus.CLOSED);
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, new BigDecimal("10.00"), null, null);
            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.deposit(goalId, request),
                    () -> "Closed goals do not accept money");
            verifyNoInteractions(transactionService, transactionRepository);
        }

        @Test
        @DisplayName("Should respond not found when the goal belongs to another user")
        void shouldRejectForeignGoal() {
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, new BigDecimal("10.00"), null, null);
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> savingsGoalService.deposit(goalId, request),
                    () -> "Ownership is enforced through the user-scoped lookup");
            verifyNoInteractions(transactionService, transactionRepository);
        }
    }

    @Nested
    @DisplayName("Withdraw")
    class Withdraw {

        @Test
        @DisplayName("Should move money from the goal back into the account and record a SAVINGS_WITHDRAWAL")
        void shouldWithdraw() {
            BigDecimal amount = new BigDecimal("200.00");
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, amount, null, null);

            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            stubMappingRoundTrip();

            savingsGoalService.withdraw(goalId, request);

            verify(transactionService).applyBalanceChanges(
                    eq(TransactionType.SAVINGS_WITHDRAWAL), eq(amount), eq(mockAccount), isNull(), eq(mockGoal));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertEquals(TransactionType.SAVINGS_WITHDRAWAL, captor.getValue().getType(), () -> "Movement type mismatch");
        }

        @Test
        @DisplayName("Should reject withdrawing more than the goal holds")
        void shouldRejectOverWithdraw() {
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, new BigDecimal("500.01"), null, null);
            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.withdraw(goalId, request),
                    () -> "Cannot withdraw more than the goal holds");
            verifyNoInteractions(transactionService, transactionRepository);
        }

        @Test
        @DisplayName("Should reject a withdrawal from a closed goal")
        void shouldRejectWithdrawFromClosedGoal() {
            mockGoal.setStatus(SavingsGoalStatus.CLOSED);
            SavingsGoalContributionDTO request = new SavingsGoalContributionDTO(accountId, new BigDecimal("10.00"), null, null);
            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.withdraw(goalId, request),
                    () -> "Closed goals are frozen");
            verifyNoInteractions(transactionService, transactionRepository);
        }
    }

    @Nested
    @DisplayName("Close")
    class Close {

        @Test
        @DisplayName("Should return everything to the chosen account and mark the goal CLOSED")
        void shouldCloseWithFunds() {
            stubOwnedGoal(mockGoal);
            when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
            stubMappingRoundTrip();
            // Mimic the real balance maths so the assertion below is meaningful.
            doAnswer(invocation -> {
                SavingsGoal goal = invocation.getArgument(4);
                goal.setCurrentAmount(goal.getCurrentAmount().subtract(invocation.getArgument(1)));
                return null;
            }).when(transactionService).applyBalanceChanges(any(), any(), any(), any(), any());

            savingsGoalService.close(goalId, new SavingsGoalCloseDTO(accountId));

            verify(transactionService).applyBalanceChanges(
                    eq(TransactionType.SAVINGS_WITHDRAWAL), eq(new BigDecimal("500.00")), eq(mockAccount), isNull(), eq(mockGoal));
            assertEquals(BigDecimal.ZERO.setScale(2), mockGoal.getCurrentAmount(), () -> "Goal must be empty after closing");
            assertEquals(SavingsGoalStatus.CLOSED, mockGoal.getStatus(), () -> "Goal must be CLOSED");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertEquals("Closed savings goal · " + goalName, captor.getValue().getDescription(), () -> "Closing description mismatch");
        }

        @Test
        @DisplayName("Should close an empty goal without needing an account")
        void shouldCloseEmptyGoalWithoutAccount() {
            mockGoal.setCurrentAmount(BigDecimal.ZERO);
            stubOwnedGoal(mockGoal);
            stubMappingRoundTrip();

            savingsGoalService.close(goalId, null);

            assertEquals(SavingsGoalStatus.CLOSED, mockGoal.getStatus(), () -> "Goal must be CLOSED");
            verifyNoInteractions(accountService, transactionService, transactionRepository);
        }

        @Test
        @DisplayName("Should require a destination account when the goal still holds money")
        void shouldRequireAccountWhenFunded() {
            stubOwnedGoal(mockGoal);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.close(goalId, new SavingsGoalCloseDTO(null)),
                    () -> "Money cannot vanish: an account is required");
            assertEquals(SavingsGoalStatus.ACTIVE, mockGoal.getStatus(), () -> "Goal must stay ACTIVE");
            verifyNoInteractions(transactionService, transactionRepository);
        }

        @Test
        @DisplayName("Should reject closing an already closed goal")
        void shouldRejectReclose() {
            mockGoal.setStatus(SavingsGoalStatus.CLOSED);
            stubOwnedGoal(mockGoal);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.close(goalId, new SavingsGoalCloseDTO(accountId)),
                    () -> "Closing is a one-time action");
            verifyNoInteractions(transactionService, transactionRepository);
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Should delete an empty goal and detach its past movements")
        void shouldDeleteEmptyGoal() {
            mockGoal.setCurrentAmount(BigDecimal.ZERO);
            stubOwnedGoal(mockGoal);

            assertDoesNotThrow(() -> savingsGoalService.deleteById(goalId),
                    () -> "Deletion should succeed for an empty goal");

            InOrder inOrder = inOrder(transactionRepository, savingsGoalRepository);
            inOrder.verify(transactionRepository).unlinkSavingsGoal(mockGoal);
            inOrder.verify(savingsGoalRepository).delete(mockGoal);
        }

        @Test
        @DisplayName("Should refuse to delete a goal that still holds money")
        void shouldRejectDeleteWithFunds() {
            stubOwnedGoal(mockGoal);

            assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.deleteById(goalId),
                    () -> "Money must be withdrawn or the goal closed first");
            verify(transactionRepository, never()).unlinkSavingsGoal(any());
            verify(savingsGoalRepository, never()).delete(any(SavingsGoal.class));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting a non-existent goal")
        void shouldThrowExceptionWhenDeletingNonExistentGoal() {
            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> savingsGoalService.deleteById(goalId),
                    () -> "Should throw EntityNotFoundException for non-existent savings goal");
            verify(savingsGoalRepository, never()).delete(any(SavingsGoal.class));
        }
    }

    @Nested
    @DisplayName("Summary")
    class Summary {

        @Test
        @DisplayName("Should aggregate open goals and leave closed ones out")
        void shouldGetSummarySuccessfully() {
            SavingsGoal achievedGoal = SavingsGoal.builder()
                    .id(UUID.randomUUID()).name("Emergency Fund").icon("fa fa-shield")
                    .targetAmount(new BigDecimal("1000.00")).targetDate(targetDate)
                    .currentAmount(new BigDecimal("1000.00"))
                    .user(mockUser).achieved(true).build();
            SavingsGoal closedGoal = SavingsGoal.builder()
                    .id(UUID.randomUUID()).name("Old laptop").icon("fa fa-laptop")
                    .targetAmount(new BigDecimal("9999.00")).targetDate(targetDate)
                    .currentAmount(BigDecimal.ZERO).status(SavingsGoalStatus.CLOSED)
                    .user(mockUser).achieved(true).build();

            when(securityUtils.getCurrentUser()).thenReturn(mockUser);
            when(savingsGoalRepository.findByUser(mockUser)).thenReturn(List.of(mockGoal, achievedGoal, closedGoal));

            var summary = savingsGoalService.getSummary();

            assertEquals(new BigDecimal("1500.00"), summary.totalSaved(), () -> "Total saved mismatch");
            assertEquals(new BigDecimal("3000.00"), summary.totalTarget(), () -> "Closed goals must not inflate the target");
            assertEquals(new BigDecimal("50.00"), summary.progressPercentage(), () -> "Aggregated progress mismatch");
            assertEquals(2, summary.totalGoals(), () -> "Closed goals are not counted");
            assertEquals(1, summary.achievedGoals(), () -> "Achieved goals mismatch");
        }
    }
}
