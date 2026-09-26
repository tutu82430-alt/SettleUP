package io.settleup.service.split;

import io.settleup.dto.request.ExpenseRequest;
import io.settleup.entity.Expense;
import io.settleup.entity.ExpenseSplit;
import io.settleup.entity.User;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for computing how an expense is divided among participants.
 * Implementations: EqualSplitStrategy, PercentageSplitStrategy, ExactAmountSplitStrategy.
 */
public interface SplitStrategy {

    /**
     * Computes the list of ExpenseSplit records for the given expense.
     *
     * @param expense      the expense entity (amount, paidBy, etc.)
     * @param participants map of userId -> User entity for all participants
     * @param request      the original request containing split-specific data
     * @return list of ExpenseSplit records (not yet persisted)
     * @throws io.settleup.exception.InvalidSplitException if the split data is invalid
     */
    List<ExpenseSplit> computeSplits(
            Expense expense,
            Map<Long, User> participants,
            ExpenseRequest request
    );
}
