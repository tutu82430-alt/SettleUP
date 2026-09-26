package io.settleup.service;

import io.settleup.dto.request.SettlementRequest;
import io.settleup.dto.response.*;
import io.settleup.entity.*;
import io.settleup.event.GroupBalanceUpdatedEvent;
import io.settleup.exception.*;
import io.settleup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final DebtSimplificationService debtSimplificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public GroupBalanceDto getGroupBalance(Long groupId, Long requestingUserId) {
        if (!groupRepository.isUserMemberOfGroup(groupId, requestingUserId)) {
            throw new UnauthorizedAccessException("You are not a member of this group.");
        }

        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        List<Expense> expenses = expenseRepository.findByGroupIdForBalanceCalc(groupId);
        List<Settlement> settlements = settlementRepository.findCompletedByGroupId(groupId);

        Map<Long, BigDecimal> balances = debtSimplificationService
                .computeNetBalances(expenses, settlements);

        List<SettlementTransactionDto> suggested = debtSimplificationService
                .computeMinimumTransactions(expenses, settlements);

        // Enrich suggested settlements with display names
        Map<Long, String> displayNames = new HashMap<>();
        for (Expense e : expenses) {
            displayNames.put(e.getPaidBy().getId(), e.getPaidBy().getDisplayName());
            e.getSplits().forEach(s ->
                    displayNames.put(s.getUser().getId(), s.getUser().getDisplayName()));
        }

        suggested.forEach(t -> {
            t.setFromUserName(displayNames.getOrDefault(t.getFromUserId(), "Unknown"));
            t.setToUserName(displayNames.getOrDefault(t.getToUserId(), "Unknown"));
        });

        return GroupBalanceDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .memberBalances(balances)
                .memberDisplayNames(displayNames)
                .suggestedSettlements(suggested)
                .build();
    }

    @Transactional
    @CacheEvict(value = "groupBalances", key = "#groupId")
    public SettlementDto recordSettlement(Long groupId, SettlementRequest request, Long fromUserId) {
        if (!groupRepository.isUserMemberOfGroup(groupId, fromUserId)) {
            throw new UnauthorizedAccessException("You are not a member of this group.");
        }

        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));
        var fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", fromUserId));
        var toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getToUserId()));

        if (!groupRepository.isUserMemberOfGroup(groupId, request.getToUserId())) {
            throw new UnauthorizedAccessException("Target user is not a member of this group.");
        }

        Settlement settlement = Settlement.builder()
                .group(group)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(request.getAmount())
                .note(request.getNote())
                .status(Settlement.SettlementStatus.PENDING)
                .build();

        settlement = settlementRepository.save(settlement);
        eventPublisher.publishEvent(new GroupBalanceUpdatedEvent(this, groupId));

        return toDto(settlement);
    }

    @Transactional
    @CacheEvict(value = "groupBalances", key = "#groupId")
    public SettlementDto confirmSettlement(Long groupId, Long settlementId, Long requestingUserId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", settlementId));

        if (!settlement.getGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Settlement not found in this group.");
        }
        if (!settlement.getToUser().getId().equals(requestingUserId)) {
            throw new UnauthorizedAccessException("Only the recipient can confirm a settlement.");
        }

        settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
        settlement.setSettledAt(LocalDateTime.now());
        settlement = settlementRepository.save(settlement);

        eventPublisher.publishEvent(new GroupBalanceUpdatedEvent(this, groupId));
        return toDto(settlement);
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Long userId) {
        var groups = groupRepository.findGroupsByUserId(userId);
        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalOwedToYou = BigDecimal.ZERO;

        List<GroupSummaryDto> summaries = new java.util.ArrayList<>();

        for (var group : groups) {
            List<Expense> expenses = expenseRepository.findByGroupIdForBalanceCalc(group.getId());
            List<Settlement> settlements = settlementRepository.findCompletedByGroupId(group.getId());
            Map<Long, BigDecimal> balances = debtSimplificationService
                    .computeNetBalances(expenses, settlements);

            BigDecimal userBalance = balances.getOrDefault(userId, BigDecimal.ZERO);
            if (userBalance.compareTo(BigDecimal.ZERO) > 0) {
                totalOwedToYou = totalOwedToYou.add(userBalance);
            } else {
                totalOwed = totalOwed.add(userBalance.abs());
            }

            summaries.add(GroupSummaryDto.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .currency(group.getCurrency().name())
                    .userNetBalance(userBalance)
                    .memberCount((int) group.getMembers().size())
                    .build());
        }

        return DashboardDto.builder()
                .userId(userId)
                .totalOwed(totalOwed)
                .totalOwedToYou(totalOwedToYou)
                .netBalance(totalOwedToYou.subtract(totalOwed))
                .groupSummaries(summaries)
                .build();
    }

    public static SettlementDto toDto(Settlement s) {
        return SettlementDto.builder()
                .id(s.getId())
                .groupId(s.getGroup().getId())
                .fromUser(AuthService.toDto(s.getFromUser()))
                .toUser(AuthService.toDto(s.getToUser()))
                .amount(s.getAmount())
                .note(s.getNote())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .settledAt(s.getSettledAt())
                .build();
    }
}
