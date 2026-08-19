package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.RecurringTransactionDTO;
import com.veritech.BudgetKing.dto.TransactionDTO;
import com.veritech.BudgetKing.enumerator.RecurrenceFrequency;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.RecurringTransactionRuntimeException;
import com.veritech.BudgetKing.filter.RecurringTransactionFilter;
import com.veritech.BudgetKing.mapper.RecurringTransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.RecurringTransaction;
import com.veritech.BudgetKing.repository.RecurringTransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recurring Transaction Service Specification")
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringTransactionMapper mapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private AppUser mockUser;
    private Account mockAccount;
    private Category mockCategory;
    private UUID templateId;
    private RecurringTransaction mockTemplate;
    private RecurringTransactionDTO mockDto;
    private final BigDecimal amount = new BigDecimal("15.00");

    @BeforeEach
    void setUpDefaults() {
        templateId = UUID.randomUUID();

        mockUser = AppUser.builder()
                .id(UUID.randomUUID())
                .email("owner@budgetking.com")
                .passwordHash("hash")
                .name("Owner")
                .lastName("User")
                .roles(new HashSet<>())
                .build();

        mockAccount = Account.builder()
                .id(UUID.randomUUID())
                .name("Main Bank")
                .balance(new BigDecimal("100.00"))
                .icon("fa fa-bank")
                .user(mockUser)
                .build();

        mockCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Subscriptions")
                .user(mockUser)
                .build();

        mockTemplate = RecurringTransaction.builder()
                .id(templateId)
                .description("Netflix")
                .amount(amount)
                .type(TransactionType.EXPENSE)
                .counterparty("Netflix")
                .category(mockCategory)
                .account(mockAccount)
                .frequency(RecurrenceFrequency.MONTHLY)
                .nextRunDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .user(mockUser)
                .build();

        mockDto = new RecurringTransactionDTO(
                templateId,
                "Netflix",
                amount,
                TransactionType.EXPENSE.name(),
                "Netflix",
                mockCategory.getId(),
                mockCategory.getName(),
                mockAccount.getId(),
                mockAccount.getName(),
                null,
                RecurrenceFrequency.MONTHLY.name(),
                LocalDate.of(2026, 1, 1),
                null,
                true
        );
    }

    /* ----------------------------------- CRUD ---------------------------------------- */

    @Test
    @DisplayName("Should retrieve template successfully by ID")
    void shouldGetByIdSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));
        when(mapper.toDto(mockTemplate)).thenReturn(mockDto);

        RecurringTransactionDTO result = recurringTransactionService.getById(templateId);

        assertNotNull(result, () -> "Result should not be null");
        assertEquals("Netflix", result.description(), () -> "Description mismatch");
        verify(recurringTransactionRepository).findByIdAndUser(templateId, mockUser);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when template does not exist")
    void shouldThrowExceptionWhenNotFound() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> recurringTransactionService.getById(templateId),
                () -> "Should throw EntityNotFoundException for non-existent template");
    }

    @Test
    @DisplayName("Should create template successfully")
    void shouldCreateTemplateSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(mockAccount.getId())).thenReturn(mockAccount);
        when(categoryService.getEntityById(mockCategory.getId())).thenReturn(mockCategory);
        when(mapper.toEntity(any(), any())).thenReturn(mockTemplate);
        when(recurringTransactionRepository.save(any())).thenReturn(mockTemplate);
        when(mapper.toDto(mockTemplate)).thenReturn(mockDto);

        RecurringTransactionDTO result = recurringTransactionService.create(mockDto);

        assertNotNull(result, () -> "Result should not be null");
        verify(recurringTransactionRepository).save(any());
    }

    @Test
    @DisplayName("Should throw exception when creating a TRANSFER template with no destination account")
    void shouldThrowExceptionWhenTransferTemplateMissingDestination() {
        RecurringTransactionDTO transferDto = new RecurringTransactionDTO(
                null, "Rent split", amount, TransactionType.TRANSFER.name(), null,
                null, null, mockAccount.getId(), mockAccount.getName(), null,
                RecurrenceFrequency.MONTHLY.name(), LocalDate.of(2026, 1, 1), null, true
        );

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(mockAccount.getId())).thenReturn(mockAccount);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recurringTransactionService.create(transferDto),
                () -> "Should require destination for TRANSFER templates");

        assertEquals("Destination account is required for TRANSFER", ex.getMessage());
        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when template amount is not positive")
    void shouldThrowExceptionForNonPositiveAmount() {
        RecurringTransactionDTO invalidDto = new RecurringTransactionDTO(
                null, "Netflix", new BigDecimal("-1.00"), TransactionType.EXPENSE.name(), "Netflix",
                mockCategory.getId(), mockCategory.getName(), mockAccount.getId(), mockAccount.getName(), null,
                RecurrenceFrequency.MONTHLY.name(), LocalDate.of(2026, 1, 1), null, true
        );

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(accountService.getEntityById(mockAccount.getId())).thenReturn(mockAccount);
        when(categoryService.getEntityById(mockCategory.getId())).thenReturn(mockCategory);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recurringTransactionService.create(invalidDto),
                () -> "Amount must be positive");

        assertEquals("Amount must be greater than zero", ex.getMessage());
    }

    @Test
    @DisplayName("Should update template successfully")
    void shouldUpdateTemplateSuccessfully() {
        RecurringTransactionDTO updateDto = new RecurringTransactionDTO(
                templateId, "Netflix Premium", amount, TransactionType.EXPENSE.name(), "Netflix",
                mockCategory.getId(), mockCategory.getName(), mockAccount.getId(), mockAccount.getName(), null,
                RecurrenceFrequency.MONTHLY.name(), LocalDate.of(2026, 1, 1), null, true
        );

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));
        when(accountService.getEntityById(mockAccount.getId())).thenReturn(mockAccount);
        when(categoryService.getEntityById(mockCategory.getId())).thenReturn(mockCategory);
        when(recurringTransactionRepository.save(any())).thenReturn(mockTemplate);
        when(mapper.toDto(any())).thenReturn(updateDto);

        RecurringTransactionDTO result = recurringTransactionService.update(templateId, updateDto);

        assertNotNull(result, () -> "Updated result should not be null");
        assertEquals("Netflix Premium", result.description(), () -> "Description should be updated");
        verify(recurringTransactionRepository).save(mockTemplate);
    }

    @Test
    @DisplayName("Should delete template successfully")
    void shouldDeleteTemplateSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));

        assertDoesNotThrow(() -> recurringTransactionService.deleteById(templateId),
                () -> "Deletion should succeed for an owned template");
        verify(recurringTransactionRepository).delete(mockTemplate);
    }

    @Test
    @DisplayName("Should return paginated search results")
    void shouldSearchSuccessfully() {
        RecurringTransactionFilter filter = new RecurringTransactionFilter();
        filter.setPage(0);
        filter.setSize(10);

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "nextRunDate"));
        Page<RecurringTransaction> page = new PageImpl<>(List.of(mockTemplate), expectedPageable, 1);

        when(recurringTransactionRepository.<RecurringTransaction>findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<RecurringTransaction>>any(),
                any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toDto(mockTemplate)).thenReturn(mockDto);

        Page<RecurringTransactionDTO> result = recurringTransactionService.search(filter);

        assertNotNull(result, () -> "Page result should not be null");
        assertEquals(1, result.getTotalElements(), () -> "Total elements mismatch");
    }

    @Test
    @DisplayName("Should return list of template options")
    void shouldGetOptionsSuccessfully() {
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByUser(mockUser)).thenReturn(List.of(mockTemplate));

        List<com.veritech.BudgetKing.dto.OptionDTO> result = recurringTransactionService.getOptions();

        assertNotNull(result, () -> "Options list should not be null");
        assertEquals(1, result.size(), () -> "Options size mismatch");
        assertEquals("Netflix", result.get(0).value(), () -> "Option value mismatch");
        verifyNoInteractions(mapper);
    }

    /* ------------------------------------ engine -------------------------------------- */

    @Test
    @DisplayName("runNow should throw when template is paused")
    void runNowShouldThrowWhenPaused() {
        mockTemplate.setActive(false);
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));

        RecurringTransactionRuntimeException ex = assertThrows(RecurringTransactionRuntimeException.class,
                () -> recurringTransactionService.runNow(templateId),
                () -> "Should reject firing a paused template");

        assertEquals("This recurring transaction is paused, activate it before running it.", ex.getMessage());
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("runNow should throw when template already reached its end date")
    void runNowShouldThrowWhenExpired() {
        mockTemplate.setEndDate(LocalDate.now().minusDays(1));
        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));

        RecurringTransactionRuntimeException ex = assertThrows(RecurringTransactionRuntimeException.class,
                () -> recurringTransactionService.runNow(templateId),
                () -> "Should reject firing an expired template");

        assertEquals("This recurring transaction already reached its end date.", ex.getMessage());
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("runNow should create a transaction and advance the cursor past today")
    void runNowShouldFireAndAdvanceCursor() {
        LocalDate today = LocalDate.now();
        mockTemplate.setNextRunDate(today.minusDays(5));
        mockTemplate.setEndDate(null);

        TransactionDTO transactionDto = mock(TransactionDTO.class);

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));
        when(mapper.toTransactionDto(eq(mockTemplate), eq(today))).thenReturn(transactionDto);
        when(recurringTransactionRepository.save(any())).thenReturn(mockTemplate);
        when(mapper.toDto(mockTemplate)).thenReturn(mockDto);

        recurringTransactionService.runNow(templateId);

        verify(transactionService).create(transactionDto);
        assertTrue(mockTemplate.getNextRunDate().isAfter(today), () -> "Cursor must move strictly past today");
        verify(recurringTransactionRepository).save(mockTemplate);
    }

    @Test
    @DisplayName("runNow should deactivate a template whose cursor moves past its end date")
    void runNowShouldDeactivateExhaustedTemplate() {
        LocalDate today = LocalDate.now();
        mockTemplate.setNextRunDate(today);
        mockTemplate.setEndDate(today); // series ends today: firing today exhausts it

        when(securityUtils.getCurrentUser()).thenReturn(mockUser);
        when(recurringTransactionRepository.findByIdAndUser(templateId, mockUser)).thenReturn(Optional.of(mockTemplate));
        when(mapper.toTransactionDto(any(), any())).thenReturn(mock(TransactionDTO.class));
        when(recurringTransactionRepository.save(any())).thenReturn(mockTemplate);
        when(mapper.toDto(mockTemplate)).thenReturn(mockDto);

        recurringTransactionService.runNow(templateId);

        assertFalse(mockTemplate.isActive(), () -> "Template should be deactivated once exhausted");
    }

    @Test
    @DisplayName("fireDueOccurrences should create one transaction and advance the cursor for a due template")
    void fireDueOccurrencesShouldFireSingleOccurrence() {
        LocalDate today = LocalDate.of(2026, 2, 1);
        mockTemplate.setNextRunDate(today);
        TransactionDTO transactionDto = mock(TransactionDTO.class);
        when(mapper.toTransactionDto(mockTemplate, today)).thenReturn(transactionDto);

        int created = recurringTransactionService.fireDueOccurrences(mockTemplate, today);

        assertEquals(1, created, () -> "Should have generated exactly one occurrence");
        assertEquals(RecurrenceFrequency.MONTHLY.advance(today), mockTemplate.getNextRunDate(),
                () -> "Cursor should advance by exactly one period");
        verify(transactionService).create(transactionDto);
    }

    @Test
    @DisplayName("fireDueOccurrences should catch up several missed periods in one run")
    void fireDueOccurrencesShouldCatchUpMultiplePeriods() {
        LocalDate today = LocalDate.of(2026, 3, 15);
        mockTemplate.setNextRunDate(LocalDate.of(2026, 1, 1)); // 3 monthly occurrences behind
        when(mapper.toTransactionDto(eq(mockTemplate), any())).thenReturn(mock(TransactionDTO.class));

        int created = recurringTransactionService.fireDueOccurrences(mockTemplate, today);

        assertEquals(3, created, () -> "Jan, Feb and Mar occurrences are all due");
        assertEquals(LocalDate.of(2026, 4, 1), mockTemplate.getNextRunDate(),
                () -> "Cursor should land exactly on the next still-pending occurrence");
        verify(transactionService, times(3)).create(any());
    }

    @Test
    @DisplayName("fireDueOccurrences should do nothing for a template that is not due yet")
    void fireDueOccurrencesShouldSkipNotYetDueTemplate() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        mockTemplate.setNextRunDate(today.plusDays(10));

        int created = recurringTransactionService.fireDueOccurrences(mockTemplate, today);

        assertEquals(0, created, () -> "Nothing should fire before nextRunDate");
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("fireDueOccurrences should skip a paused template")
    void fireDueOccurrencesShouldSkipPausedTemplate() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        mockTemplate.setNextRunDate(today);
        mockTemplate.setActive(false);

        int created = recurringTransactionService.fireDueOccurrences(mockTemplate, today);

        assertEquals(0, created, () -> "A paused template must not fire");
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("runDue should fire every due template and report how many transactions were generated")
    void runDueShouldFireDueTemplates() {
        LocalDate today = LocalDate.now();
        mockTemplate.setNextRunDate(today);
        when(recurringTransactionRepository.findDue(any())).thenReturn(List.of(mockTemplate));
        when(mapper.toTransactionDto(eq(mockTemplate), any())).thenReturn(mock(TransactionDTO.class));
        when(recurringTransactionRepository.save(any())).thenReturn(mockTemplate);

        int created = recurringTransactionService.runDue();

        assertEquals(1, created, () -> "Should have generated exactly one transaction");
        verify(transactionService).create(any());
        verify(recurringTransactionRepository).save(mockTemplate);
    }

    @Test
    @DisplayName("runDue should skip a failing template without blocking the rest of the batch")
    void runDueShouldSkipFailingTemplateAndContinue() {
        LocalDate today = LocalDate.now();

        RecurringTransaction failingTemplate = RecurringTransaction.builder()
                .id(UUID.randomUUID())
                .description("Broken")
                .amount(amount)
                .type(TransactionType.EXPENSE)
                .category(mockCategory)
                .account(mockAccount)
                .frequency(RecurrenceFrequency.MONTHLY)
                .nextRunDate(today)
                .active(true)
                .user(mockUser)
                .build();

        mockTemplate.setNextRunDate(today);

        when(recurringTransactionRepository.findDue(any())).thenReturn(List.of(failingTemplate, mockTemplate));
        when(mapper.toTransactionDto(eq(failingTemplate), any())).thenThrow(new RuntimeException("boom"));
        when(mapper.toTransactionDto(eq(mockTemplate), any())).thenReturn(mock(TransactionDTO.class));
        when(recurringTransactionRepository.save(mockTemplate)).thenReturn(mockTemplate);

        int created = recurringTransactionService.runDue();

        assertEquals(1, created, () -> "Only the healthy template should have produced a transaction");
        verify(transactionService, times(1)).create(any());
    }

    @Test
    @DisplayName("getUpcoming should project future occurrences without mutating the template")
    void getUpcomingShouldProjectOccurrences() {
        AppUser user = mockUser;
        mockTemplate.setNextRunDate(LocalDate.now());
        LocalDate originalCursor = mockTemplate.getNextRunDate();

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(recurringTransactionRepository.findActiveInWindow(eq(user), any(), any())).thenReturn(List.of(mockTemplate));
        when(mapper.toOccurrenceDto(eq(mockTemplate), any())).thenAnswer(invocation -> {
            LocalDate occurrence = invocation.getArgument(1);
            return new RecurringTransactionDTO(
                    mockDto.id(), mockDto.description(), mockDto.amount(), mockDto.type(), mockDto.counterparty(),
                    mockDto.category(), mockDto.categoryName(), mockDto.account(), mockDto.accountName(),
                    mockDto.destinationAccount(), mockDto.frequency(), occurrence, mockDto.endDate(), mockDto.active()
            );
        });

        List<RecurringTransactionDTO> result = recurringTransactionService.getUpcoming();

        assertFalse(result.isEmpty(), () -> "Should project at least one occurrence within the horizon");
        assertEquals(originalCursor, mockTemplate.getNextRunDate(), () -> "Projection must not mutate the template");
        verify(recurringTransactionRepository, never()).save(any());
        verifyNoInteractions(transactionService);
    }
}
