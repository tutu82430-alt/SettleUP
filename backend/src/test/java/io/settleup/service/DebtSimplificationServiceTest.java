package io.settleup.service;

import io.settleup.dto.response.SettlementTransactionDto;
import io.settleup.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the DebtSimplificationService algorithm.
 * Tests edge cases as specified in the requirements.
 */
class DebtSimplificationServiceTest {

    private DebtSimplificationService service;

    @BeforeEach
    void setUp() {
        service = new DebtSimplificationService();
    }

    // ─────────────────── Helpers ───────────────────

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setDisplayName("User " + id);
        u.setEmail("user" + id + "@test.com");
        u.setPasswordHash("hash");
        return u;
    }

    private Expense expense(User paidBy, BigDecimal amount, List<ExpenseSplit> splits) {
        Expense e = new Expense();
        e.setId(System.nanoTime());
        e.setGroup(new Group());
        e.setPaidBy(paidBy);
        e.setAmount(amount);
        e.setSplitType(Expense.SplitType.EQUAL);
        e.setDescription("Test");
        e.setExpenseDate(LocalDate.now());
        e.setSplits(splits);
        return e;
    }

    private ExpenseSplit split(Expense exp, User user, BigDecimal amount) {
        ExpenseSplit s = new ExpenseSplit();
        s.setExpense(exp);
        s.setUser(user);
        s.setAmount(amount);
        return s;
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }

    // ─────────────────── Tests ───────────────────

    @Nested
    @DisplayName("computeNetBalances")
    class NetBalanceTests {

        @Test
        @DisplayName("already settled group returns empty balances")
        void alreadySettled() {
            User alice = user(1L);
            User bob = user(2L);

            // Alice pays 100 for Alice+Bob equally
            Expense e = expense(alice, bd("100.00"), Collections.emptyList());
            ExpenseSplit s1 = split(e, alice, bd("50.00"));
            ExpenseSplit s2 = split(e, bob, bd("50.00"));
            e.setSplits(List.of(s1, s2));

            // Bob settles 50 to Alice
            Settlement settlement = new Settlement();
            settlement.setFromUser(bob);
            settlement.setToUser(alice);
            settlement.setAmount(bd("50.00"));
            settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
            settlement.setGroup(new Group());

            Map<Long, BigDecimal> balances = service.computeNetBalances(
                    List.of(e), List.of(settlement));

            // After settlement, all balances should be zero (map should be empty)
            assertThat(balances).isEmpty();
        }

        @Test
        @DisplayName("single debtor and creditor")
        void singleDebtorAndCreditor() {
            User alice = user(1L);
            User bob = user(2L);

            Expense e = expense(alice, bd("60.00"), Collections.emptyList());
            e.setSplits(List.of(
                    split(e, alice, bd("30.00")),
                    split(e, bob, bd("30.00"))
            ));

            Map<Long, BigDecimal> balances = service.computeNetBalances(
                    List.of(e), Collections.emptyList());

            // Alice paid 60, owes 30 → net +30
            // Bob paid 0, owes 30 → net -30
            assertThat(balances.get(1L)).isEqualByComparingTo("30.00");
            assertThat(balances.get(2L)).isEqualByComparingTo("-30.00");
        }

        @Test
        @DisplayName("multiple debtors and creditors")
        void multipleDebtorsAndCreditors() {
            User alice = user(1L);
            User bob = user(2L);
            User charlie = user(3L);

            // Alice pays 90, split equally among 3
            Expense e1 = expense(alice, bd("90.00"), Collections.emptyList());
            e1.setSplits(List.of(
                    split(e1, alice, bd("30.00")),
                    split(e1, bob, bd("30.00")),
                    split(e1, charlie, bd("30.00"))
            ));

            // Bob pays 60, split equally between bob and charlie
            Expense e2 = expense(bob, bd("60.00"), Collections.emptyList());
            e2.setSplits(List.of(
                    split(e2, bob, bd("30.00")),
                    split(e2, charlie, bd("30.00"))
            ));

            Map<Long, BigDecimal> balances = service.computeNetBalances(
                    List.of(e1, e2), Collections.emptyList());

            // Alice: paid 90, owes 30 → +60
            // Bob: paid 60, owes 30+30=60 → 0 (filtered out)
            // Charlie: paid 0, owes 30+30=60 → -60
            assertThat(balances.get(1L)).isEqualByComparingTo("60.00");
            assertThat(balances.get(2L)).isNull(); // zero balance filtered
            assertThat(balances.get(3L)).isEqualByComparingTo("-60.00");
        }

        @Test
        @DisplayName("(d) floating-point amounts that don't divide evenly (splitting ₹100 three ways)")
        void floatingPointRounding() {
            User alice = user(1L);
            User bob = user(2L);
            User charlie = user(3L);

            // ₹100 split 3 ways: 33.34, 33.33, 33.33
            Expense e = expense(alice, bd("100.00"), Collections.emptyList());
            e.setSplits(List.of(
                    split(e, alice, bd("33.34")),
                    split(e, bob, bd("33.33")),
                    split(e, charlie, bd("33.33"))
            ));

            Map<Long, BigDecimal> balances = service.computeNetBalances(
                    List.of(e), Collections.emptyList());

            assertThat(balances.get(1L)).isEqualByComparingTo("66.66");
            assertThat(balances.get(2L)).isEqualByComparingTo("-33.33");
            assertThat(balances.get(3L)).isEqualByComparingTo("-33.33");

            // Sum of all balances must be zero
            BigDecimal totalBalance = balances.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalBalance.abs()).isLessThanOrEqualTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("(e) member who paid for an expense but is also owed money elsewhere in the same settlement")
        void memberPaidAndIsOwedElsewhereNetBalances() {
            User alice = user(1L);
            User bob = user(2L);
            User charlie = user(3L);
            User dave = user(4L);

            // Expense 1: Alice pays $120 for Alice and Bob ($60 each)
            Expense e1 = expense(alice, bd("120.00"), Collections.emptyList());
            e1.setSplits(List.of(
                    split(e1, alice, bd("60.00")),
                    split(e1, bob, bd("60.00"))
            ));

            // Expense 2: Bob pays $100 for Alice, Bob, Charlie, Dave ($25 each)
            Expense e2 = expense(bob, bd("100.00"), Collections.emptyList());
            e2.setSplits(List.of(
                    split(e2, alice, bd("25.00")),
                    split(e2, bob, bd("25.00")),
                    split(e2, charlie, bd("25.00")),
                    split(e2, dave, bd("25.00"))
            ));

            // Expense 3: Charlie pays $40 for Alice and Charlie ($20 each)
            Expense e3 = expense(charlie, bd("40.00"), Collections.emptyList());
            e3.setSplits(List.of(
                    split(e3, alice, bd("20.00")),
                    split(e3, charlie, bd("20.00"))
            ));

            Map<Long, BigDecimal> balances = service.computeNetBalances(
                    List.of(e1, e2, e3), Collections.emptyList());

            // Alice: paid 120, owes 60+25+20 = 105 -> net +15
            // Bob: paid 100, owes 60+25 = 85 -> net +15
            // Charlie: paid 40, owes 25+20 = 45 -> net -5
            // Dave: paid 0, owes 25 -> net -25
            assertThat(balances.get(1L)).isEqualByComparingTo("15.00");
            assertThat(balances.get(2L)).isEqualByComparingTo("15.00");
            assertThat(balances.get(3L)).isEqualByComparingTo("-5.00");
            assertThat(balances.get(4L)).isEqualByComparingTo("-25.00");
        }
    }

    @Nested
    @DisplayName("computeMinimumTransactions")
    class MinTransactionsTests {

        @Test
        @DisplayName("(a) already settled group produces no transactions")
        void noTransactionsWhenSettled() {
            List<SettlementTransactionDto> txs = service.computeMinimumTransactions(
                    Collections.emptyList(), Collections.emptyList());
            assertThat(txs).isEmpty();
        }

        @Test
        @DisplayName("(b) one debtor and one creditor — single transaction")
        void oneTransaction() {
            User alice = user(1L);
            User bob = user(2L);

            Expense e = expense(alice, bd("50.00"), Collections.emptyList());
            e.setSplits(List.of(
                    split(e, alice, bd("25.00")),
                    split(e, bob, bd("25.00"))
            ));

            List<SettlementTransactionDto> txs = service.computeMinimumTransactions(
                    List.of(e), Collections.emptyList());

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).getFromUserId()).isEqualTo(2L); // Bob owes
            assertThat(txs.get(0).getToUserId()).isEqualTo(1L);   // Alice is owed
            assertThat(txs.get(0).getAmount()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("(c) multiple debtors and multiple creditors — greedy minimizes transactions")
        void threePersonMinimized() {
            User alice = user(1L);
            User bob = user(2L);
            User charlie = user(3L);

            // Alice pays 90 for everyone equally
            Expense e = expense(alice, bd("90.00"), Collections.emptyList());
            e.setSplits(List.of(
                    split(e, alice, bd("30.00")),
                    split(e, bob, bd("30.00")),
                    split(e, charlie, bd("30.00"))
            ));

            List<SettlementTransactionDto> txs = service.computeMinimumTransactions(
                    List.of(e), Collections.emptyList());

            // 2 transactions: Bob -> Alice 30, Charlie -> Alice 30
            assertThat(txs).hasSize(2);
            BigDecimal totalSettled = txs.stream()
                    .map(SettlementTransactionDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalSettled).isEqualByComparingTo("60.00");
        }

        @Test
        @DisplayName("(d) splitting ₹100 three ways — transactions resolve correctly")
        void splitting100ThreeWaysTransactions() {
            User alice = user(1L);
            User bob = user(2L);
            User charlie = user(3L);

            Expense e = expense(alice, bd("100.00"), Collections.emptyList());
            e.setSplits(List.of(
                    split(e, alice, bd("33.34")),
                    split(e, bob, bd("33.33")),
                    split(e, charlie, bd("33.33"))
            ));

            List<SettlementTransactionDto> txs = service.computeMinimumTransactions(
                    List.of(e), Collections.emptyList());

            // Bob owes 33.33 to Alice, Charlie owes 33.33 to Alice
            assertThat(txs).hasSize(2);
            BigDecimal totalSettled = txs.stream()
                    .map(SettlementTransactionDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalSettled).isEqualByComparingTo("66.66");
        }

        @Test
        @DisplayName("(e) member who paid for an expense but is also owed elsewhere — transactions zero out all balances")
        void memberWhoPaidAndIsOwedElsewhereTransactions() {
            User alice = user(1L);
            User bob = user(2L);
            User charlie = user(3L);
            User dave = user(4L);

            Expense e1 = expense(alice, bd("120.00"), Collections.emptyList());
            e1.setSplits(List.of(
                    split(e1, alice, bd("60.00")),
                    split(e1, bob, bd("60.00"))
            ));

            Expense e2 = expense(bob, bd("100.00"), Collections.emptyList());
            e2.setSplits(List.of(
                    split(e2, alice, bd("25.00")),
                    split(e2, bob, bd("25.00")),
                    split(e2, charlie, bd("25.00")),
                    split(e2, dave, bd("25.00"))
            ));

            Expense e3 = expense(charlie, bd("40.00"), Collections.emptyList());
            e3.setSplits(List.of(
                    split(e3, alice, bd("20.00")),
                    split(e3, charlie, bd("20.00"))
            ));

            List<SettlementTransactionDto> txs = service.computeMinimumTransactions(
                    List.of(e1, e2, e3), Collections.emptyList());

            // Check that total transaction count <= N-1 (3 transactions max for 4 people)
            assertThat(txs.size()).isLessThanOrEqualTo(3);

            // Verify all balances zero out when applying transactions
            Map<Long, BigDecimal> balances = service.computeNetBalances(
                    List.of(e1, e2, e3), Collections.emptyList());

            for (SettlementTransactionDto tx : txs) {
                BigDecimal fromBal = balances.getOrDefault(tx.getFromUserId(), BigDecimal.ZERO);
                BigDecimal toBal = balances.getOrDefault(tx.getToUserId(), BigDecimal.ZERO);
                balances.put(tx.getFromUserId(), fromBal.add(tx.getAmount()));
                balances.put(tx.getToUserId(), toBal.subtract(tx.getAmount()));
            }

            balances.forEach((uid, bal) ->
                    assertThat(bal.abs()).isLessThanOrEqualTo(new BigDecimal("0.01")));
        }

        @Test
        @DisplayName("complex group — all balances zero after applying transactions")
        void complexGroupBalancesZeroAfterTransactions() {
            User u1 = user(1L); // Alice
            User u2 = user(2L); // Bob
            User u3 = user(3L); // Charlie
            User u4 = user(4L); // Dave

            // Various expenses
            Expense e1 = expense(u1, bd("200.00"), Collections.emptyList());
            e1.setSplits(List.of(
                    split(e1, u1, bd("50.00")),
                    split(e1, u2, bd("50.00")),
                    split(e1, u3, bd("50.00")),
                    split(e1, u4, bd("50.00"))
            ));

            Expense e2 = expense(u2, bd("100.00"), Collections.emptyList());
            e2.setSplits(List.of(
                    split(e2, u2, bd("50.00")),
                    split(e2, u3, bd("50.00"))
            ));

            Expense e3 = expense(u4, bd("60.00"), Collections.emptyList());
            e3.setSplits(List.of(
                    split(e3, u1, bd("20.00")),
                    split(e3, u4, bd("40.00"))
            ));

            List<SettlementTransactionDto> txs = service.computeMinimumTransactions(
                    List.of(e1, e2, e3), Collections.emptyList());

            // Verify: applying transactions should zero out all net balances
            Map<Long, BigDecimal> postTxBalances = service.computeNetBalances(
                    List.of(e1, e2, e3), Collections.emptyList());

            // Apply transactions as settlements
            BigDecimal adjustedSum = BigDecimal.ZERO;
            for (SettlementTransactionDto tx : txs) {
                BigDecimal fromBal = postTxBalances.getOrDefault(tx.getFromUserId(), BigDecimal.ZERO);
                BigDecimal toBal = postTxBalances.getOrDefault(tx.getToUserId(), BigDecimal.ZERO);
                postTxBalances.put(tx.getFromUserId(), fromBal.add(tx.getAmount()));
                postTxBalances.put(tx.getToUserId(), toBal.subtract(tx.getAmount()));
            }

            // All balances should be within epsilon of zero
            postTxBalances.forEach((uid, bal) ->
                    assertThat(bal.abs()).isLessThanOrEqualTo(new BigDecimal("0.01")));

            // Should be at most N-1 = 3 transactions
            assertThat(txs.size()).isLessThanOrEqualTo(3);
        }
    }
}
