package io.settleup.dto.request;

import io.settleup.entity.Expense;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount format invalid")
    private BigDecimal amount;

    @NotNull(message = "Split type is required")
    private Expense.SplitType splitType;

    @Size(max = 50)
    private String category;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    /**
     * IDs of users to split the expense with (including the payer).
     */
    @NotEmpty(message = "At least one participant is required")
    private List<Long> participantIds;

    /**
     * For PERCENTAGE splits: map of userId -> percentage (must sum to 100).
     */
    private Map<Long, BigDecimal> percentageSplits;

    /**
     * For EXACT splits: map of userId -> exact amount.
     */
    private Map<Long, BigDecimal> exactSplits;
}
