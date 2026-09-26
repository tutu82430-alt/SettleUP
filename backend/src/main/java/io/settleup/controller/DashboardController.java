package io.settleup.controller;

import io.settleup.dto.response.DashboardDto;
import io.settleup.repository.UserRepository;
import io.settleup.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BalanceService balanceService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<DashboardDto> getDashboard(
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = userRepository.findByEmail(principal.getUsername())
                .orElseThrow().getId();
        return ResponseEntity.ok(balanceService.getDashboard(userId));
    }
}
