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
 * Divides expense by specified percentages.
 * Percentages must sum to exactly 100.
 */
@Component
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public List<ExpenseSplit> computeSplits(
            Expense expense,
            Map<Long, User> participants,
            ExpenseRequest request
    ) {
        Map<Long, BigDecimal> percentages = request.getPercentageSplits();
        if (percentages == null || percentages.isEmpty()) {
            throw new InvalidSplitException("Percentage splits must be provided.");
        }

        // Validate that percentages cover exactly the participants
        for (Long userId : participants.keySet()) {
            if (!percentages.containsKey(userId)) {
                throw new InvalidSplitException(
                        "Percentage not specified for participant with id: " + userId);
            }
        }

        // Validate sum = 100
        BigDecimal sum = percentages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (sum.compareTo(HUNDRED) != 0) {
            throw new InvalidSplitException(
                    "Percentages must sum to 100, but got: " + sum);
        }

        BigDecimal total = expense.getAmount();
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal allocatedSoFar = BigDecimal.ZERO;

        List<Long> userIds = new ArrayList<>(participants.keySet());
        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            User participant = participants.get(userId);
            BigDecimal pct = percentages.get(userId);

            BigDecimal splitAmount;
            if (i == userIds.size() - 1) {
                // Last participant gets the remainder to ensure exact total
                splitAmount = total.subtract(allocatedSoFar);
            } else {
                splitAmount = total.multiply(pct)
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                allocatedSoFar = allocatedSoFar.add(splitAmount);
            }

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(participant)
                    .amount(splitAmount)
                    .percentage(pct)
                    .build());
        }

        return splits;
    }
}
