package io.settleup.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private String inviteCode;
    private String currency;
    private UserDto createdBy;
    private List<MemberDto> members;
    private LocalDateTime createdAt;
    private int memberCount;
}
