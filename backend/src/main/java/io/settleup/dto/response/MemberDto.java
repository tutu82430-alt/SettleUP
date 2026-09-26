package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberDto {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String role;
    private LocalDateTime joinedAt;
}
