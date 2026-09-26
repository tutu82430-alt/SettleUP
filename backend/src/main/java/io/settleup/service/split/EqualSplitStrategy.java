package io.settleup.service.split;

import io.settleup.dto.request.ExpenseRequest;
import io.settleup.entity.Expense;
import io.settleup.entity.ExpenseSplit;
import io.settleup.entity.User;
import io.settleup.exception.InvalidSplitException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Divides the expense equally among all participants.
 * Handles penny rounding: the "first" participant absorbs any remainder.
 */
@Component
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(
            Expense expense,
            Map<Long, User> participants,
            ExpenseRequest request
    ) {
        if (participants.isEmpty()) {
            throw new InvalidSplitException("At least one participant is required for an equal split.");
        }

        int count = participants.size();
        BigDecimal total = expense.getAmount();
        BigDecimal share = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(share.multiply(BigDecimal.valueOf(count)));

        List<ExpenseSplit> splits = new ArrayList<>();
        boolean remainderAssigned = false;

        for (User participant : participants.values()) {
            BigDecimal splitAmount = share;
            if (!remainderAssigned && remainder.compareTo(BigDecimal.ZERO) != 0) {
                splitAmount = splitAmount.add(remainder);
                remainderAssigned = true;
            }
            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(participant)
                    .amount(splitAmount)
                    .build());
        }

        return splits;
    }
}
