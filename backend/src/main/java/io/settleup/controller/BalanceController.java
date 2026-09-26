package io.settleup.controller;

import io.settleup.dto.request.SettlementRequest;
import io.settleup.dto.response.*;
import io.settleup.repository.UserRepository;
import io.settleup.service.BalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final UserRepository userRepository;

    @GetMapping("/balance")
    public ResponseEntity<GroupBalanceDto> getGroupBalance(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(balanceService.getGroupBalance(groupId, userId));
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementDto> recordSettlement(
            @PathVariable Long groupId,
            @Valid @RequestBody SettlementRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(balanceService.recordSettlement(groupId, request, userId));
    }

    @PatchMapping("/settlements/{settlementId}/confirm")
    public ResponseEntity<SettlementDto> confirmSettlement(
            @PathVariable Long groupId,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(balanceService.confirmSettlement(groupId, settlementId, userId));
    }

    private Long resolveUserId(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow().getId();
    }
}
