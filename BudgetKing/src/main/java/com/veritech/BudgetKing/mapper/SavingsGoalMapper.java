package com.veritech.BudgetKing.mapper;

import com.veritech.BudgetKing.dto.SavingsGoalDTO;
import com.veritech.BudgetKing.dto.SavingsGoalRelatedEntities;
import com.veritech.BudgetKing.enumerator.SavingsGoalStatus;
import com.veritech.BudgetKing.interfaces.ICrudMapper;
import com.veritech.BudgetKing.model.Account;
import com.veritech.BudgetKing.model.SavingsGoal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Translates between {@link SavingsGoal} and {@link SavingsGoalDTO}.
 *
 * <p>Deliberately free of repositories and of business maths: related entities
 * arrive already resolved, and the derived progress figures are filled in by
 * {@link com.veritech.BudgetKing.service.SavingsGoalService} after mapping.</p>
 */
@Component
public class SavingsGoalMapper implements ICrudMapper<SavingsGoal, SavingsGoalDTO, SavingsGoalRelatedEntities> {

    /**
     * Maps the persisted state only. Derived components ({@code state}, progress
     * figures) are emitted empty and are expected to be replaced by the service
     * before the DTO leaves the app.
     */
    @Override
    public SavingsGoalDTO toDto(SavingsGoal entity) {
        Account linkedAccount = entity.getLinkedAccount();

        return new SavingsGoalDTO(
                entity.getId(),
                entity.getName(),
                entity.getIcon(),
                entity.getTargetAmount(),
                entity.getTargetDate(),
                linkedAccount != null ? linkedAccount.getId() : null,
                linkedAccount != null ? linkedAccount.getName() : null,
                entity.getStatus(),
                null,
                entity.isAchieved(),
                entity.getCurrentAmount(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L
        );
    }

    /**
     * Builds a brand-new, empty, active goal. Money-related and derived components
     * of the DTO are ignored: a goal is only funded through contributions.
     */
    @Override
    public SavingsGoal toEntity(SavingsGoalDTO dto, SavingsGoalRelatedEntities relatedEntities) {
        return SavingsGoal.builder()
                .id(dto.id())
                .name(dto.name())
                .icon(dto.icon())
                .targetAmount(dto.targetAmount())
                .targetDate(dto.targetDate())
                .linkedAccount(relatedEntities.linkedAccount())
                .user(relatedEntities.user())
                .currentAmount(BigDecimal.ZERO)
                .status(SavingsGoalStatus.ACTIVE)
                .achieved(false)
                .build();
    }
}
