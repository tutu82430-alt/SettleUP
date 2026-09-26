package io.settleup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits",
        indexes = {
            @Index(name = "idx_expense_splits_expense_id", columnList = "expense_id"),
            @Index(name = "idx_expense_splits_user_id", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The actual monetary amount this user owes for this expense.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * For PERCENTAGE splits: the percentage this user owes (0-100).
     * Null for EQUAL and EXACT splits.
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;
}
