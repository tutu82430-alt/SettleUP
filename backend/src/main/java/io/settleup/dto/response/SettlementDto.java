package io.settleup.dto.response;

import io.settleup.entity.Settlement;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SettlementDto {
    private Long id;
    private Long groupId;
    private UserDto fromUser;
    private UserDto toUser;
    private BigDecimal amount;
    private String note;
    private Settlement.SettlementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}
