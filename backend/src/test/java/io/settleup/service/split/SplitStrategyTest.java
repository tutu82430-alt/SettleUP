package io.settleup.service.split;

import io.settleup.dto.request.ExpenseRequest;
import io.settleup.entity.*;
import io.settleup.exception.InvalidSplitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class SplitStrategyTest {

    private User alice, bob, charlie;
    private Expense expense;
    private Map<Long, User> participants;

    @BeforeEach
    void setUp() {
        alice = makeUser(1L, "alice");
        bob = makeUser(2L, "bob");
        charlie = makeUser(3L, "charlie");

        expense = new Expense();
        expense.setId(1L);
        expense.setSplitType(Expense.SplitType.EQUAL);
        expense.setDescription("Test Expense");
        expense.setExpenseDate(LocalDate.now());
        expense.setGroup(new Group());

        participants = new LinkedHashMap<>();
        participants.put(1L, alice);
        participants.put(2L, bob);
        participants.put(3L, charlie);
    }

    private User makeUser(Long id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        u.setDisplayName(name);
        u.setEmail(name + "@test.com");
        u.setPasswordHash("hash");
        return u;
    }

    // ─────── EqualSplitStrategy ───────

    @Test
    @DisplayName("Equal split: divides evenly among 3 with penny remainder on first")
    void equalSplit_threePeople_evenWithRemainder() {
        expense.setAmount(new BigDecimal("100.00"));
        EqualSplitStrategy strategy = new EqualSplitStrategy();
        ExpenseRequest request = new ExpenseRequest();

        var splits = strategy.computeSplits(expense, participants, request);

        assertThat(splits).hasSize(3);
        BigDecimal total = splits.stream()
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Equal split: $10 among 3 people — sum equals total")
    void equalSplit_sumEqualsTotalAlways() {
        expense.setAmount(new BigDecimal("10.00"));
        EqualSplitStrategy strategy = new EqualSplitStrategy();
        var splits = strategy.computeSplits(expense, participants, new ExpenseRequest());

        BigDecimal total = splits.stream()
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Equal split: empty participants throws exception")
    void equalSplit_emptyParticipants_throws() {
        expense.setAmount(new BigDecimal("50.00"));
        EqualSplitStrategy strategy = new EqualSplitStrategy();
        assertThatThrownBy(() ->
                strategy.computeSplits(expense, Collections.emptyMap(), new ExpenseRequest()))
                .isInstanceOf(InvalidSplitException.class);
    }

    // ─────── PercentageSplitStrategy ───────

    @Test
    @DisplayName("Percentage split: 50/30/20 computes correct amounts")
    void percentageSplit_correct() {
        expense.setAmount(new BigDecimal("200.00"));
        PercentageSplitStrategy strategy = new PercentageSplitStrategy();

        ExpenseRequest request = new ExpenseRequest();
        request.setPercentageSplits(Map.of(
                1L, new BigDecimal("50"),
                2L, new BigDecimal("30"),
                3L, new BigDecimal("20")
        ));

        var splits = strategy.computeSplits(expense, participants, request);
        BigDecimal total = splits.stream()
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Percentage split: percentages not summing to 100 throws exception")
    void percentageSplit_notSummingTo100_throws() {
        expense.setAmount(new BigDecimal("100.00"));
        PercentageSplitStrategy strategy = new PercentageSplitStrategy();

        ExpenseRequest request = new ExpenseRequest();
        request.setPercentageSplits(Map.of(
                1L, new BigDecimal("40"),
                2L, new BigDecimal("30"),
                3L, new BigDecimal("20")  // only 90
        ));

        assertThatThrownBy(() ->
                strategy.computeSplits(expense, participants, request))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("100");
    }

    // ─────── ExactAmountSplitStrategy ───────

    @Test
    @DisplayName("Exact split: amounts summing to total succeeds")
    void exactSplit_correct() {
        expense.setAmount(new BigDecimal("150.00"));
        ExactAmountSplitStrategy strategy = new ExactAmountSplitStrategy();

        ExpenseRequest request = new ExpenseRequest();
        request.setExactSplits(Map.of(
                1L, new BigDecimal("75.00"),
                2L, new BigDecimal("50.00"),
                3L, new BigDecimal("25.00")
        ));

        var splits = strategy.computeSplits(expense, participants, request);
        BigDecimal total = splits.stream()
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Exact split: amounts not matching total throws exception")
    void exactSplit_wrongTotal_throws() {
        expense.setAmount(new BigDecimal("100.00"));
        ExactAmountSplitStrategy strategy = new ExactAmountSplitStrategy();

        ExpenseRequest request = new ExpenseRequest();
        request.setExactSplits(Map.of(
                1L, new BigDecimal("60.00"),
                2L, new BigDecimal("20.00"),
                3L, new BigDecimal("10.00")  // sum = 90, not 100
        ));

        assertThatThrownBy(() ->
                strategy.computeSplits(expense, participants, request))
                .isInstanceOf(InvalidSplitException.class);
    }

    @Test
    @DisplayName("Exact split: negative amount throws exception")
    void exactSplit_negativeAmount_throws() {
        expense.setAmount(new BigDecimal("100.00"));
        ExactAmountSplitStrategy strategy = new ExactAmountSplitStrategy();

        ExpenseRequest request = new ExpenseRequest();
        request.setExactSplits(Map.of(
                1L, new BigDecimal("150.00"),
                2L, new BigDecimal("-50.00"),  // negative
                3L, new BigDecimal("0.00")
        ));

        assertThatThrownBy(() ->
                strategy.computeSplits(expense, participants, request))
                .isInstanceOf(InvalidSplitException.class);
    }
}
