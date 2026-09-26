package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardDto {
    private Long userId;
    private BigDecimal totalOwed;     // total the user owes to others
    private BigDecimal totalOwedToYou; // total others owe to this user
    private BigDecimal netBalance;    // totalOwedToYou - totalOwed
    private List<GroupSummaryDto> groupSummaries;
}
