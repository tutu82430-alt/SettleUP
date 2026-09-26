package io.settleup.dto.response;

import io.settleup.entity.Expense;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExpenseDto {
    private Long id;
    private Long groupId;
    private UserDto paidBy;
    private String description;
    private BigDecimal amount;
    private Expense.SplitType splitType;
    private String category;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private List<SplitDto> splits;
}
