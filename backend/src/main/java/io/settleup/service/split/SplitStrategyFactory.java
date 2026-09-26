package io.settleup.service.split;

import io.settleup.entity.Expense;
import io.settleup.exception.InvalidSplitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory that selects the correct SplitStrategy based on the expense SplitType.
 */
@Component
@RequiredArgsConstructor
public class SplitStrategyFactory {

    private final EqualSplitStrategy equalSplitStrategy;
    private final PercentageSplitStrategy percentageSplitStrategy;
    private final ExactAmountSplitStrategy exactAmountSplitStrategy;

    /**
     * Returns the appropriate split strategy for the given split type.
     *
     * @param splitType the type of split
     * @return the corresponding SplitStrategy implementation
     */
    public SplitStrategy getStrategy(Expense.SplitType splitType) {
        return switch (splitType) {
            case EQUAL -> equalSplitStrategy;
            case PERCENTAGE -> percentageSplitStrategy;
            case EXACT -> exactAmountSplitStrategy;
            default -> throw new InvalidSplitException(
                    "Unknown split type: " + splitType);
        };
    }
}
