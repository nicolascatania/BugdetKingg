package com.veritech.BudgetKing.service;

import com.veritech.BudgetKing.dto.*;
import com.veritech.BudgetKing.enumerator.TransactionType;
import com.veritech.BudgetKing.exception.SavingsGoalRuntimeException;
import com.veritech.BudgetKing.filter.DashBoardFilter;
import com.veritech.BudgetKing.filter.TransactionFilter;
import com.veritech.BudgetKing.interfaces.ICrudService;
import com.veritech.BudgetKing.mapper.TransactionMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.AppUser;
import com.veritech.BudgetKing.model.Category;
import com.veritech.BudgetKing.model.SavingsGoal;
import com.veritech.BudgetKing.model.Transaction;
import com.veritech.BudgetKing.repository.AccountRepository;
import com.veritech.BudgetKing.repository.TransactionRepository;
import com.veritech.BudgetKing.security.util.SecurityUtils;
import com.veritech.BudgetKing.utils.DateUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.veritech.BudgetKing.enumerator.TransactionType.*;

@Service
@RequiredArgsConstructor
public class TransactionService implements ICrudService<TransactionDTO, UUID, TransactionFilter> {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final SecurityUtils securityUtils;
    private final AccountService accountService;
    private final CategoryService categoryService;


    @Override
    public TransactionDTO getById(UUID id) {
        AppUser user = securityUtils.getCurrentUser();
        Transaction t = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return mapper.toDto(t);
    }

    @Override
    @Transactional
    public TransactionDTO create(TransactionDTO dto) {

        AppUser user = securityUtils.getCurrentUser();
        Account sourceAccount = accountService.getEntityById(dto.account());
        Account destinationAccount = resolveDestinationAccount(dto);

        // Category is optional for TRANSFER transactions
        Category category = null;
        if (dto.category() != null) {
            category = categoryService.getEntityById(dto.category());
        }

        assertNotSavingsType(dto);
        validateTransaction(dto, sourceAccount, destinationAccount);

        applyBalanceChanges(TransactionType.fromString(dto.type()), dto.amount(), sourceAccount, destinationAccount, null);

        TransactionRelatedEntities related = new TransactionRelatedEntities(
                user,
                sourceAccount,
                destinationAccount,
                category

        );

        Transaction transaction = mapper.toEntity(dto, related);
        return mapper.toDto(transactionRepository.save(transaction));
    }


    @Override
    @Transactional
    public TransactionDTO update(UUID id, TransactionDTO dto) {
        Transaction existing = getTransaction(id);

        assertImmutableFieldsUnchanged(existing, dto);
        validateTransaction(dto, existing.getAccount(), existing.getDestinationAccount());

        revertBalanceChanges(existing.getType(), existing.getAmount(), existing.getAccount(), existing.getDestinationAccount(), existing.getSavingsGoal());
        applyBalanceChanges(existing.getType(), dto.amount(), existing.getAccount(), existing.getDestinationAccount(), existing.getSavingsGoal());

        Category category = dto.category() != null ? categoryService.getEntityById(dto.category()) : null;

        existing.setDate(LocalDateTime.parse(dto.date()));
        existing.setAmount(dto.amount());
        existing.setDescription(dto.description());
        existing.setCounterparty(dto.counterparty());
        existing.setCategory(category);

        return mapper.toDto(existing);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        Transaction existing = getTransaction(id);

        revertBalanceChanges(existing.getType(), existing.getAmount(), existing.getAccount(), existing.getDestinationAccount(), existing.getSavingsGoal());

        transactionRepository.delete(existing);
    }

    @Override
    public List<OptionDTO> getOptions() {
        AppUser user = securityUtils.getCurrentUser();
        return transactionRepository.findByUser(user)
                .stream()
                .map(t -> new OptionDTO(t.getId().toString(), t.getDescription()))
                .toList();
    }

    @Override
    public Page<TransactionDTO> search(TransactionFilter filter) {

        AppUser user = securityUtils.getCurrentUser();

        Pageable pageRequest = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "date")
        );

        Page<Transaction> transactions;
        if (filter == null)
            transactions = transactionRepository.findAllByUser(user, pageRequest);
        else
            transactions = transactionRepository.findAll(filter.toSpecification(user), pageRequest);

        return transactions.map(mapper::toDto);
    }

    /**
     * Gets the entity by UUID and current user in the session
     * If not found, throws Runtime Exception
     *
     * @param id UUID from the transaction the user is looking for
     * @return Transaction entity
     */
    public Transaction getTransaction(UUID id) {
        AppUser user = securityUtils.getCurrentUser();
        return transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
    }

    /**
     * Gets the list of the last movements of the month of the user
     *
     * @return
     */
    public List<LastMovesDTO> movementsOfThisMonth() {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMonths(1);

        return transactionRepository
                .findByUserAndDateBetween(user, start, end)
                .stream()
                .map(mapper::toLastMovesDTO)
                .sorted((a, b) -> b.date().compareTo(a.date()))
                .toList();
    }

    /**
     * Gets the user`s monthly balance
     *
     * @return
     */
    public MonthlyTransactionReportDTO getMonthlyBalance() {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMonths(1);

        return transactionRepository.getMonthlyReport(user, start, end);
    }

    /**
     * Only {@code SavingsGoalService} may create savings movements: it is the one
     * that keeps the goal's own balance in step with the account. Editing an
     * existing one through the generic endpoint is still allowed.
     */
    void assertNotSavingsType(TransactionDTO dto) {
        if (TransactionType.fromString(dto.type()).isSavings()) {
            throw new IllegalArgumentException("Savings movements must be made through the savings goal endpoints");
        }
    }

    void validateTransaction(
            TransactionDTO dto,
            @NotNull Account source,
            Account destination
    ) {
        TransactionType type = TransactionType.fromString(dto.type());
        if (type.equals(TRANSFER)) {
            if (destination == null) {
                throw new IllegalArgumentException("Destination account is required for TRANSFER");
            }
            if (source.getId().equals(destination.getId())) {
                throw new IllegalArgumentException("Source and destination accounts must be different");
            }
        }

        if (dto.amount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    /**
     * Applies the balance effect of a movement to every party involved. The goal
     * parameter is only consulted for the savings types; the others ignore it.
     *
     * <p>This is the single place that knows how each {@link TransactionType} moves
     * money, so {@code SavingsGoalService} delegates here instead of keeping its own
     * copy of the rules.</p>
     *
     * @throws SavingsGoalRuntimeException when the goal is closed or the movement would
     *                                     leave it with a negative balance
     */
    void applyBalanceChanges(
            TransactionType type,
            BigDecimal amount,
            Account source,
            Account destination,
            SavingsGoal goal
    ) {
        switch (type) {
            case EXPENSE -> source.setBalance(source.getBalance().subtract(amount));

            case INCOME -> source.setBalance(source.getBalance().add(amount));

            case TRANSFER -> {
                source.setBalance(source.getBalance().subtract(amount));
                destination.setBalance(destination.getBalance().add(amount));
            }

            // The goal is moved first: it is the side that can refuse, and refusing before
            // touching the account keeps the failure side-effect free even outside a transaction.
            case SAVINGS_DEPOSIT -> {
                moveGoalBalance(goal, amount);
                source.setBalance(source.getBalance().subtract(amount));
            }

            case SAVINGS_WITHDRAWAL -> {
                moveGoalBalance(goal, amount.negate());
                source.setBalance(source.getBalance().add(amount));
            }
        }
    }

    /**
     * Undoes the balance effect a stored transaction previously applied, so it can be
     * safely reapplied with new values (update) or left undone permanently (delete).
     * Exact mirror of {@link #applyBalanceChanges}.
     */
    void revertBalanceChanges(
            TransactionType type,
            BigDecimal amount,
            Account source,
            Account destination,
            SavingsGoal goal
    ) {
        switch (type) {
            case EXPENSE -> source.setBalance(source.getBalance().add(amount));

            case INCOME -> source.setBalance(source.getBalance().subtract(amount));

            case TRANSFER -> {
                source.setBalance(source.getBalance().add(amount));
                destination.setBalance(destination.getBalance().subtract(amount));
            }

            case SAVINGS_DEPOSIT -> {
                moveGoalBalance(goal, amount.negate());
                source.setBalance(source.getBalance().add(amount));
            }

            case SAVINGS_WITHDRAWAL -> {
                moveGoalBalance(goal, amount);
                source.setBalance(source.getBalance().subtract(amount));
            }
        }
    }

    /**
     * Shifts a goal's saved amount by {@code delta} and refreshes its cached
     * {@code achieved} flag. A goal can absorb any deposit but never go negative,
     * and a closed goal is frozen: both cases surface as a 409.
     */
    private void moveGoalBalance(SavingsGoal goal, BigDecimal delta) {
        if (goal == null) {
            throw new IllegalArgumentException("Savings movements require a savings goal");
        }
        if (!goal.isActive()) {
            throw new SavingsGoalRuntimeException("Savings goal is closed");
        }

        BigDecimal updated = goal.getCurrentAmount().add(delta);
        if (updated.signum() < 0) {
            throw new SavingsGoalRuntimeException("Savings goal does not hold enough money for this movement");
        }

        goal.setCurrentAmount(updated);
        goal.setAchieved(updated.compareTo(goal.getTargetAmount()) >= 0);
    }

    /**
     * account, destinationAccount, savingsGoal and type are immutable on update - users who
     * logged the wrong one are expected to delete the transaction and create a new one instead.
     */
    void assertImmutableFieldsUnchanged(Transaction existing, TransactionDTO dto) {
        UUID existingDestinationId = existing.getDestinationAccount() != null
                ? existing.getDestinationAccount().getId()
                : null;
        UUID existingGoalId = existing.getSavingsGoal() != null
                ? existing.getSavingsGoal().getId()
                : null;

        if (!existing.getAccount().getId().equals(dto.account())
                || !existing.getType().name().equals(dto.type())
                || !Objects.equals(existingDestinationId, dto.destinationAccount())
                || !Objects.equals(existingGoalId, dto.savingsGoal())) {
            throw new IllegalArgumentException("account, destinationAccount, savingsGoal and type cannot be changed on update");
        }
    }

    private Account resolveDestinationAccount(TransactionDTO dto) {
        if (dto.destinationAccount() == null) {
            return null;
        }
        return accountService.getEntityById(dto.destinationAccount());
    }


    /**
     * Gets the proper data to show in the dashboard
     * List of expenses by category in a range of dates
     * With icons
     *
     * @param filter
     * @return
     */
    public DashBoardDTO getDataForDashBoard(DashBoardFilter filter) {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime start = DateUtils.parseStart(filter.getDateFrom());
        LocalDateTime end = DateUtils.parseEnd(filter.getDateTo());

        IncomeExpenseDTO totals = transactionRepository.getIncomeAndExpense(user, start, end);
        BigDecimal totalExpense = totals.expense();

        List<CategoryExpenseDTO> listWithPercentage = transactionRepository.getExpensesByCategoryWithIcon(user, start, end)
                .stream()
                .map(r -> {
                    BigDecimal amount = (BigDecimal) r[2];
                    double pct = (totalExpense.compareTo(BigDecimal.ZERO) > 0)
                            ? amount.divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue()
                            : 0.0;

                    return new CategoryExpenseDTO(
                            (String) r[0], // name
                            (String) r[1], // icon
                            amount,
                            pct
                    );
                })
                .collect(Collectors.toList());

        BigDecimal balance = accountService.getAccounts().stream()
                .map(Account::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netBalance = totals.income().subtract(totals.expense());

        return new DashBoardDTO(balance, totals.expense(), totals.income(), netBalance, listWithPercentage);
    }

    /**
     * Every movement of the user inside {@code [dateFrom, dateTo]} (both inclusive,
     * whole days), most recent first. Same shape as {@link #movementsOfThisMonth()}
     * so the same list component can render either.
     *
     * @param dateFrom first day of the range, ISO {@code yyyy-MM-dd}
     * @param dateTo   last day of the range, ISO {@code yyyy-MM-dd}
     */
    public List<LastMovesDTO> movementsBetween(String dateFrom, String dateTo) {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime start = DateUtils.parseStart(dateFrom);
        LocalDateTime end = DateUtils.parseEnd(dateTo);

        return transactionRepository
                .findByUserAndDateBetween(user, start, end)
                .stream()
                .map(mapper::toLastMovesDTO)
                .sorted((a, b) -> b.date().compareTo(a.date()))
                .toList();
    }

    /**
     * Income and expenses of the current calendar month next to the previous one.
     * The current month is partial by nature; the comparison is still useful as a
     * running indicator and the UI labels it as such.
     */
    public MonthComparisonDTO getMonthComparison() {
        AppUser user = securityUtils.getCurrentUser();
        LocalDateTime currentStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime currentEnd = currentStart.plusMonths(1);
        LocalDateTime previousStart = currentStart.minusMonths(1);

        MonthlyTransactionReportDTO current = transactionRepository.getMonthlyReport(user, currentStart, currentEnd);
        MonthlyTransactionReportDTO previous = transactionRepository.getMonthlyReport(user, previousStart, currentStart);

        return new MonthComparisonDTO(current.income(), current.outcome(), previous.income(), previous.outcome());
    }


    /**
     * Returns income and expense totals per month for the current year.
     * Can be optionally filtered by account.
     */
    public List<MonthlyIncomeExpenseDTO> getIncomeExpensePerMonth(UUID accountId) {

        AppUser user = securityUtils.getCurrentUser();
        int currentYear = LocalDateTime.now().getYear();

        return transactionRepository
                .getIncomeExpenseByMonth(user, currentYear, accountId);
    }

    public void testCICD() {
        // Method to test CICD pipeline
    }

}