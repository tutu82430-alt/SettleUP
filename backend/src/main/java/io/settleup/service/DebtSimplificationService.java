package io.settleup.service;

import io.settleup.dto.response.SettlementTransactionDto;
import io.settleup.entity.Expense;
import io.settleup.entity.Settlement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * DebtSimplificationService — core algorithm to minimize settlement transactions.
 *
 * <p>Algorithm Overview:
 * <ol>
 *   <li>Compute each member's net balance: sum of what they paid MINUS sum of what they owe.</li>
 *   <li>Positive balance = creditor (others owe them). Negative balance = debtor (they owe others).</li>
 *   <li>Use a greedy matching algorithm:
 *     <ul>
 *       <li>At each step, pick the largest creditor (most owed) and largest debtor (owes most).</li>
 *       <li>Create a transaction between them for min(creditor_balance, |debtor_balance|).</li>
 *       <li>Reduce balances accordingly. Repeat until all balances are zero.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>This greedy approach produces at most (N-1) transactions for N people,
 * which is optimal or near-optimal for most real-world distributions.
 */
@Service
public class DebtSimplificationService {

    private static final BigDecimal EPSILON = new BigDecimal("0.005");

    /**
     * Computes the minimum set of settlement transactions to zero out all balances.
     *
     * @param expenses   list of all expenses in the group
     * @param settlements list of already-completed settlements
     * @return list of (fromUserId, toUserId, amount) transactions needed
     */
    public List<SettlementTransactionDto> computeMinimumTransactions(
            List<Expense> expenses,
            List<Settlement> settlements
    ) {
        // Step 1: Compute net balances
        Map<Long, BigDecimal> netBalances = computeNetBalances(expenses, settlements);

        // Step 2: Separate into creditors and debtors
        // Use priority queues for greedy matching
        PriorityQueue<long[]> creditors = new PriorityQueue<>(
                // max-heap by amount (creditors with highest positive balance first)
                (a, b) -> b[1] > a[1] ? 1 : (b[1] < a[1] ? -1 : 0)
        );
        PriorityQueue<long[]> debtors = new PriorityQueue<>(
                // max-heap by absolute negative amount (debtors who owe most first)
                (a, b) -> a[1] > b[1] ? 1 : (a[1] < b[1] ? -1 : 0)
        );

        // Use cents (multiply by 100) to avoid floating-point in comparisons
        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            long userId = entry.getKey();
            long centsBalance = entry.getValue()
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            if (centsBalance > 0) {
                creditors.offer(new long[]{userId, centsBalance});
            } else if (centsBalance < 0) {
                debtors.offer(new long[]{userId, centsBalance});
            }
            // zero balances are ignored
        }

        // Step 3: Greedy matching
        List<SettlementTransactionDto> transactions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] creditor = creditors.poll();
            long[] debtor = debtors.poll();

            long creditAmount = creditor[1];       // positive
            long debtAmount = Math.abs(debtor[1]); // positive absolute value

            long settled = Math.min(creditAmount, debtAmount);
            BigDecimal settledAmount = BigDecimal.valueOf(settled, 2); // cents -> dollars

            transactions.add(SettlementTransactionDto.builder()
                    .fromUserId(debtor[0])
                    .toUserId(creditor[0])
                    .amount(settledAmount)
                    .build());

            long remainingCredit = creditAmount - settled;
            long remainingDebt = debtAmount - settled;

            if (remainingCredit > 0) {
                creditors.offer(new long[]{creditor[0], remainingCredit});
            }
            if (remainingDebt > 0) {
                debtors.offer(new long[]{debtor[0], -remainingDebt});
            }
        }

        return transactions;
    }

    /**
     * Computes the net balance map: userId -> netBalance.
     * netBalance > 0 means others owe this person.
     * netBalance < 0 means this person owes others.
     *
     * @param expenses    all group expenses
     * @param settlements all completed settlements in the group
     * @return map of userId to net balance
     */
    public Map<Long, BigDecimal> computeNetBalances(
            List<Expense> expenses,
            List<Settlement> settlements
    ) {
        Map<Long, BigDecimal> balances = new HashMap<>();

        // Process expenses
        for (Expense expense : expenses) {
            Long paidById = expense.getPaidBy().getId();

            // Payer's balance increases by the full amount they paid
            balances.merge(paidById, expense.getAmount(), BigDecimal::add);

            // Each participant's balance decreases by their split amount
            for (var split : expense.getSplits()) {
                Long splitUserId = split.getUser().getId();
                balances.merge(splitUserId, split.getAmount().negate(), BigDecimal::add);
            }
        }

        // Process completed settlements (they adjust balances)
        for (Settlement settlement : settlements) {
            if (settlement.getStatus() == Settlement.SettlementStatus.COMPLETED) {
                Long fromId = settlement.getFromUser().getId();
                Long toId = settlement.getToUser().getId();
                BigDecimal amount = settlement.getAmount();

                // fromUser paid toUser → fromUser's debt decreases, toUser's credit decreases
                balances.merge(fromId, amount, BigDecimal::add);
                balances.merge(toId, amount.negate(), BigDecimal::add);
            }
        }

        // Remove entries within epsilon of zero (rounding artifacts)
        balances.entrySet().removeIf(e ->
                e.getValue().abs().compareTo(EPSILON) < 0);

        return balances;
    }
}
