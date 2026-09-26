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
 * Divides expense by exact monetary amounts specified per participant.
 * Exact amounts must sum to the total expense amount (within 1 cent tolerance).
 */
@Component
public class ExactAmountSplitStrategy implements SplitStrategy {

    @Override
    public List<ExpenseSplit> computeSplits(
            Expense expense,
            Map<Long, User> participants,
            ExpenseRequest request
    ) {
        Map<Long, BigDecimal> exactAmounts = request.getExactSplits();
        if (exactAmounts == null || exactAmounts.isEmpty()) {
            throw new InvalidSplitException("Exact split amounts must be provided.");
        }

        // Validate that amounts cover exactly the participants
        for (Long userId : participants.keySet()) {
            if (!exactAmounts.containsKey(userId)) {
                throw new InvalidSplitException(
                        "Exact amount not specified for participant with id: " + userId);
            }
        }

        // Validate sum == total
        BigDecimal sum = exactAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = expense.getAmount().setScale(2, RoundingMode.HALF_UP);

        BigDecimal diff = sum.subtract(total).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            throw new InvalidSplitException(
                    "Exact split amounts sum (" + sum + ") must equal expense total (" + total + ").");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        for (Map.Entry<Long, User> entry : participants.entrySet()) {
            Long userId = entry.getKey();
            User participant = entry.getValue();
            BigDecimal amount = exactAmounts.get(userId).setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidSplitException("Split amounts cannot be negative.");
            }

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(participant)
                    .amount(amount)
                    .build());
        }

        return splits;
    }
}
