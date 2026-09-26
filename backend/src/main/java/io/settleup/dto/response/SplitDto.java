package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SplitDto {
    private Long userId;
    private String username;
    private String displayName;
    private BigDecimal amount;
    private BigDecimal percentage;
}
