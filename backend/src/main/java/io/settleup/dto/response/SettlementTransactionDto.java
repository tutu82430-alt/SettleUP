package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SettlementTransactionDto {
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    // Populated for display purposes
    private String fromUserName;
    private String toUserName;
}
