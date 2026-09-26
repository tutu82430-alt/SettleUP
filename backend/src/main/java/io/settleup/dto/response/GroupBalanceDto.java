package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class GroupBalanceDto {
    private Long groupId;
    private String groupName;
    /**
     * Net balance per member: positive = others owe them, negative = they owe others.
     */
    private Map<Long, BigDecimal> memberBalances;
    /**
     * User display info keyed by user ID for rendering purposes.
     */
    private Map<Long, String> memberDisplayNames;
    /**
     * Minimum set of transactions to settle all debts.
     */
    private List<SettlementTransactionDto> suggestedSettlements;
}
