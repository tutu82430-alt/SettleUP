package io.settleup.service;

import io.settleup.dto.request.CreateGroupRequest;
import io.settleup.dto.response.*;
import io.settleup.entity.*;
import io.settleup.exception.*;
import io.settleup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final String INVITE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupDto createGroup(CreateGroupRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));

        Group.GroupCurrency currency = Group.GroupCurrency.USD;
        if (request.getCurrency() != null) {
            try {
                currency = Group.GroupCurrency.valueOf(request.getCurrency().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // defaults to USD
            }
        }

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .inviteCode(generateUniqueInviteCode())
                .createdBy(creator)
                .currency(currency)
                .build();

        group = groupRepository.save(group);

        // Add creator as ADMIN member
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(creator)
                .role(GroupMember.MemberRole.ADMIN)
                .build();
        groupMemberRepository.save(member);

        return toDto(group, List.of(member));
    }

    @Transactional
    public GroupDto joinGroup(String inviteCode, Long userId) {
        Group group = groupRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Group with invite code: " + inviteCode));

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
            throw new DuplicateResourceException("User is already a member of this group.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupMember.MemberRole.MEMBER)
                .build();
        groupMemberRepository.save(member);

        List<GroupMember> members = groupMemberRepository.findAll()
                .stream()
                .filter(m -> m.getGroup().getId().equals(group.getId()))
                .collect(Collectors.toList());

        return toDto(group, members);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryDto> getUserGroups(Long userId) {
        return groupRepository.findGroupsByUserId(userId).stream()
                .map(g -> GroupSummaryDto.builder()
                        .groupId(g.getId())
                        .groupName(g.getName())
                        .currency(g.getCurrency().name())
                        .memberCount((int) groupMemberRepository.countByGroupId(g.getId()))
                        .userNetBalance(java.math.BigDecimal.ZERO) // populated by balance service
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupDto getGroupById(Long groupId, Long requestingUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        if (!groupRepository.isUserMemberOfGroup(groupId, requestingUserId)) {
            throw new UnauthorizedAccessException("You are not a member of this group.");
        }

        List<GroupMember> members = groupMemberRepository.findAll()
                .stream()
                .filter(m -> m.getGroup().getId().equals(groupId))
                .collect(Collectors.toList());

        return toDto(group, members);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateCode();
        } while (groupRepository.findByInviteCode(code).isPresent());
        return code;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CHARS.charAt(RANDOM.nextInt(INVITE_CHARS.length())));
        }
        return sb.toString();
    }

    public static GroupDto toDto(Group group, List<GroupMember> members) {
        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .inviteCode(group.getInviteCode())
                .currency(group.getCurrency().name())
                .createdBy(AuthService.toDto(group.getCreatedBy()))
                .members(members.stream().map(GroupService::toMemberDto).collect(Collectors.toList()))
                .createdAt(group.getCreatedAt())
                .memberCount(members.size())
                .build();
    }

    public static MemberDto toMemberDto(GroupMember member) {
        return MemberDto.builder()
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .displayName(member.getUser().getDisplayName())
                .avatarUrl(member.getUser().getAvatarUrl())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
