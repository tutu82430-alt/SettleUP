package io.settleup.service;

import io.settleup.dto.request.ExpenseRequest;
import io.settleup.dto.response.*;
import io.settleup.entity.*;
import io.settleup.event.GroupBalanceUpdatedEvent;
import io.settleup.exception.*;
import io.settleup.repository.*;
import io.settleup.service.split.SplitStrategy;
import io.settleup.service.split.SplitStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SplitStrategyFactory splitStrategyFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = "groupBalances", key = "#groupId")
    public ExpenseDto addExpense(Long groupId, ExpenseRequest request, Long payerId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        if (!groupRepository.isUserMemberOfGroup(groupId, payerId)) {
            throw new UnauthorizedAccessException("You are not a member of this group.");
        }

        User paidBy = userRepository.findById(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", payerId));

        // Validate and load all participants
        Map<Long, User> participants = new LinkedHashMap<>();
        for (Long participantId : request.getParticipantIds()) {
            if (!groupRepository.isUserMemberOfGroup(groupId, participantId)) {
                throw new UnauthorizedAccessException(
                        "User " + participantId + " is not a member of this group.");
            }
            User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", participantId));
            participants.put(participantId, participant);
        }

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidBy)
                .description(request.getDescription())
                .amount(request.getAmount())
                .splitType(request.getSplitType())
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate() != null ?
                        request.getExpenseDate() : LocalDate.now())
                .build();

        // Apply split strategy
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        List<ExpenseSplit> splits = strategy.computeSplits(expense, participants, request);
        expense.setSplits(splits);

        expense = expenseRepository.save(expense);

        // Publish event for WebSocket broadcast
        eventPublisher.publishEvent(new GroupBalanceUpdatedEvent(this, groupId));

        return toDto(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getExpenses(Long groupId, Long requestingUserId,
                                        String category, LocalDate fromDate,
                                        LocalDate toDate, Long memberId) {
        if (!groupRepository.isUserMemberOfGroup(groupId, requestingUserId)) {
            throw new UnauthorizedAccessException("You are not a member of this group.");
        }

        List<Expense> expenses;
        if (category == null && fromDate == null && toDate == null && memberId == null) {
            expenses = expenseRepository.findByGroupIdWithDetails(groupId);
        } else {
            expenses = expenseRepository.findByGroupIdWithFilters(
                    groupId, category, fromDate, toDate, memberId);
        }

        return expenses.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "groupBalances", key = "#groupId")
    public void deleteExpense(Long groupId, Long expenseId, Long requestingUserId) {
        if (!groupRepository.isUserMemberOfGroup(groupId, requestingUserId)) {
            throw new UnauthorizedAccessException("You are not a member of this group.");
        }
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        if (!expense.getGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Expense not found in this group.");
        }
        expenseRepository.delete(expense);
        eventPublisher.publishEvent(new GroupBalanceUpdatedEvent(this, groupId));
    }

    public static ExpenseDto toDto(Expense expense) {
        return ExpenseDto.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .paidBy(AuthService.toDto(expense.getPaidBy()))
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .splitType(expense.getSplitType())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .splits(expense.getSplits().stream()
                        .map(s -> SplitDto.builder()
                                .userId(s.getUser().getId())
                                .username(s.getUser().getUsername())
                                .displayName(s.getUser().getDisplayName())
                                .amount(s.getAmount())
                                .percentage(s.getPercentage())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
