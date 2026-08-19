package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.exception.SavingsGoalRuntimeException;
import com.veritech.BudgetKing.filter.SavingsGoalFilter;
import com.veritech.BudgetKing.mapper.SavingsGoalMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.repository.SavingsGoalRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
                .linkedAccount(mockAccount)
                .user(mockUser)
                .achieved(false)
                .build();

        // Mirrors what the real mapper does: persisted fields mapped, derived fields zeroed.
        mockDto = new SavingsGoalDTO(
                goalId, goalName, "fa fa-plane", targetAmount, targetDate,
                accountId, "Main Bank", false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );
    }

    @Test
    @DisplayName("Should retrieve savings goal by ID with computed progress")
    void shouldGetByIdSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.of(mockGoal));
        when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

        SavingsGoalDTO result = savingsGoalService.getById(goalId);

        assertNotNull(result, () -> "Result should not be null");
        assertEquals(goalName, result.name(), () -> "Goal name mismatch");
        assertEquals(new BigDecimal("500.00"), result.currentAmount(), () -> "Current amount should come from linked account balance");
        assertEquals(new BigDecimal("25.00"), result.progressPercentage(), () -> "Progress percentage mismatch");
        assertEquals(new BigDecimal("1500.00"), result.remainingAmount(), () -> "Remaining amount mismatch");
        assertEquals(new BigDecimal("250.00"), result.monthlyRequired(), () -> "Monthly required mismatch");
        assertFalse(result.achieved(), () -> "Goal should not be achieved yet");
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
    @DisplayName("Should create savings goal successfully with a linked account")
    void shouldCreateGoalSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
        when(savingsGoalMapper.toEntity(any(), any())).thenReturn(mockGoal);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(mockGoal);
        when(savingsGoalMapper.toDto(mockGoal)).thenReturn(mockDto);

        SavingsGoalDTO result = savingsGoalService.create(mockDto);

        assertNotNull(result, () -> "Result DTO should not be null");
        verify(accountService).getEntityById(accountId);
        verify(savingsGoalRepository).save(any(SavingsGoal.class));
    }

    @Test
    @DisplayName("Should create savings goal successfully without a linked account")
    void shouldCreateGoalWithoutLinkedAccount() {
        SavingsGoalDTO dtoWithoutAccount = new SavingsGoalDTO(
                null, goalName, "fa fa-plane", targetAmount, targetDate,
                null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );
        SavingsGoal goalWithoutAccount = SavingsGoal.builder()
                .id(goalId).name(goalName).icon("fa fa-plane")
                .targetAmount(targetAmount).targetDate(targetDate)
                .user(mockUser).achieved(false).build();

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalMapper.toEntity(any(), any())).thenReturn(goalWithoutAccount);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(goalWithoutAccount);
        when(savingsGoalMapper.toDto(goalWithoutAccount)).thenReturn(dtoWithoutAccount);

        SavingsGoalDTO result = savingsGoalService.create(dtoWithoutAccount);

        assertNotNull(result, () -> "Result DTO should not be null");
        assertEquals(BigDecimal.ZERO, result.currentAmount(), () -> "Current amount should be zero when unlinked");
        verifyNoInteractions(accountService);
    }

    @Test
    @DisplayName("Should throw exception when target amount is not positive")
    void shouldThrowExceptionWhenTargetAmountIsNotPositive() {
        SavingsGoalDTO invalidDto = new SavingsGoalDTO(
                null, goalName, "fa fa-plane", BigDecimal.ZERO, targetDate,
                null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);

        assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.create(invalidDto),
                () -> "Should reject a non-positive target amount");
        verify(savingsGoalRepository, never()).save(any(SavingsGoal.class));
    }

    @Test
    @DisplayName("Should throw exception when target date is in the past")
    void shouldThrowExceptionWhenTargetDateIsInThePast() {
        SavingsGoalDTO invalidDto = new SavingsGoalDTO(
                null, goalName, "fa fa-plane", targetAmount, LocalDate.now().minusDays(1),
                null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);

        assertThrows(SavingsGoalRuntimeException.class, () -> savingsGoalService.create(invalidDto),
                () -> "Should reject a target date already in the past");
        verify(savingsGoalRepository, never()).save(any(SavingsGoal.class));
    }

    @Test
    @DisplayName("Should mark goal as achieved when linked balance already covers the target")
    void shouldMarkGoalAsAchievedOnCreate() {
        Account fundedAccount = Account.builder()
                .id(accountId).name("Main Bank").balance(new BigDecimal("5000.00")).user(mockUser).build();
        SavingsGoal fundedGoal = SavingsGoal.builder()
                .id(goalId).name(goalName).icon("fa fa-plane").targetAmount(targetAmount)
                .targetDate(targetDate).linkedAccount(fundedAccount).user(mockUser).achieved(false).build();

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(accountId)).thenReturn(fundedAccount);
        when(savingsGoalMapper.toEntity(any(), any())).thenReturn(fundedGoal);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(savingsGoalMapper.toDto(any())).thenReturn(mockDto);

        savingsGoalService.create(mockDto);

        assertTrue(fundedGoal.isAchieved(), () -> "Goal should be flagged achieved once balance covers the target");
    }

    @Test
    @DisplayName("Should update savings goal details successfully")
    void shouldUpdateGoalSuccessfully() {
        SavingsGoalDTO updateDto = new SavingsGoalDTO(
                goalId, "New Name", "fa fa-flag", targetAmount, targetDate,
                accountId, "Main Bank", false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L
        );

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.of(mockGoal));
        when(accountService.getEntityById(accountId)).thenReturn(mockAccount);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(mockGoal);
        when(savingsGoalMapper.toDto(any())).thenReturn(updateDto);

        SavingsGoalDTO result = savingsGoalService.update(goalId, updateDto);

        assertNotNull(result, () -> "Updated result should not be null");
        assertEquals("New Name", result.name(), () -> "Goal name should be updated");
        verify(savingsGoalRepository).save(mockGoal);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating a non-existent goal")
    void shouldThrowExceptionWhenUpdatingNonExistentGoal() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> savingsGoalService.update(goalId, mockDto),
                () -> "Should throw EntityNotFoundException for non-existent savings goal");
    }

    @Test
    @DisplayName("Should delete savings goal successfully")
    void shouldDeleteGoalSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByIdAndUser(goalId, mockUser)).thenReturn(Optional.of(mockGoal));

        assertDoesNotThrow(() -> savingsGoalService.deleteById(goalId),
                () -> "Deletion should succeed for an existing goal");
        verify(savingsGoalRepository).delete(mockGoal);
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

        assertNotNull(result, () -> "Page result should not be null");
        assertEquals(1, result.getTotalElements(), () -> "Total elements mismatch");
    }

    @Test
    @DisplayName("Should return list of savings goal options")
    void shouldGetOptionsSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByUser(mockUser)).thenReturn(List.of(mockGoal));

        List<com.veritech.BudgetKing.dto.OptionDTO> result = savingsGoalService.getOptions();

        assertNotNull(result, () -> "Options list should not be null");
        assertEquals(1, result.size(), () -> "Options size mismatch");
        assertEquals(goalName, result.get(0).value(), () -> "Option value mismatch");
        verifyNoInteractions(savingsGoalMapper);
    }

    @Test
    @DisplayName("Should aggregate a correct summary across all goals")
    void shouldGetSummarySuccessfully() {
        SavingsGoal achievedGoal = SavingsGoal.builder()
                .id(UUID.randomUUID()).name("Emergency Fund").icon("fa fa-shield")
                .targetAmount(new BigDecimal("1000.00")).targetDate(targetDate)
                .linkedAccount(Account.builder().balance(new BigDecimal("1000.00")).build())
                .user(mockUser).achieved(true).build();

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(savingsGoalRepository.findByUser(mockUser)).thenReturn(List.of(mockGoal, achievedGoal));

        var summary = savingsGoalService.getSummary();

        assertEquals(new BigDecimal("1500.00"), summary.totalSaved(), () -> "Total saved mismatch");
        assertEquals(new BigDecimal("3000.00"), summary.totalTarget(), () -> "Total target mismatch");
        assertEquals(new BigDecimal("50.00"), summary.progressPercentage(), () -> "Aggregated progress mismatch");
        assertEquals(2, summary.totalGoals(), () -> "Total goals mismatch");
        assertEquals(1, summary.achievedGoals(), () -> "Achieved goals mismatch");
    }
}
