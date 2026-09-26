package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
