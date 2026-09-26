package io.settleup.controller;

import io.settleup.dto.request.CreateGroupRequest;
import io.settleup.dto.response.*;
import io.settleup.repository.UserRepository;
import io.settleup.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(request, userId));
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<GroupDto> joinGroup(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(groupService.joinGroup(inviteCode, userId));
    }

    @GetMapping
    public ResponseEntity<List<GroupSummaryDto>> getMyGroups(
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(groupService.getGroupById(groupId, userId));
    }

    private Long resolveUserId(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow().getId();
    }
}
