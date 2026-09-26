package io.settleup.controller;

import io.settleup.dto.request.ExpenseRequest;
import io.settleup.dto.response.ExpenseDto;
import io.settleup.repository.UserRepository;
import io.settleup.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ExpenseDto> addExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.addExpense(groupId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpenses(
            @PathVariable Long groupId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long memberId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(
                expenseService.getExpenses(groupId, userId, category, fromDate, toDate, memberId));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        expenseService.deleteExpense(groupId, expenseId, userId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow().getId();
    }
}
