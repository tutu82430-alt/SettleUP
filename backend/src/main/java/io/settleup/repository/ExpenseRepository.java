package io.settleup.repository;

import io.settleup.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT e FROM Expense e
        JOIN FETCH e.paidBy
        JOIN FETCH e.splits s
        JOIN FETCH s.user
        WHERE e.group.id = :groupId
        ORDER BY e.expenseDate DESC, e.createdAt DESC
        """)
    List<Expense> findByGroupIdWithDetails(@Param("groupId") Long groupId);

    @Query("""
        SELECT e FROM Expense e
        JOIN FETCH e.paidBy
        JOIN FETCH e.splits s
        JOIN FETCH s.user
        WHERE e.group.id = :groupId
        AND (:category IS NULL OR e.category = :category)
        AND (:fromDate IS NULL OR e.expenseDate >= :fromDate)
        AND (:toDate IS NULL OR e.expenseDate <= :toDate)
        AND (:memberId IS NULL OR e.paidBy.id = :memberId
             OR EXISTS (SELECT 1 FROM ExpenseSplit sp WHERE sp.expense = e AND sp.user.id = :memberId))
        ORDER BY e.expenseDate DESC, e.createdAt DESC
        """)
    List<Expense> findByGroupIdWithFilters(
            @Param("groupId") Long groupId,
            @Param("category") String category,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("memberId") Long memberId
    );

    /**
     * Loads all expenses and their splits for balance computation.
     * Returns only what's needed for net balance calculation.
     */
    @Query("""
        SELECT e FROM Expense e
        JOIN FETCH e.paidBy
        JOIN FETCH e.splits s
        JOIN FETCH s.user
        WHERE e.group.id = :groupId
        """)
    List<Expense> findByGroupIdForBalanceCalc(@Param("groupId") Long groupId);
}
