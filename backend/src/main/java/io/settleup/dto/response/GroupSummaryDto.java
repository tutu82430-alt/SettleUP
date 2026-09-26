package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GroupSummaryDto {
    private Long groupId;
    private String groupName;
    private String currency;
    private BigDecimal userNetBalance; // positive = owed to user, negative = user owes
    private int memberCount;
}
