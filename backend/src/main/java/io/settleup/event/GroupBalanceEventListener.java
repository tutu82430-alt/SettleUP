package io.settleup.event;

import io.settleup.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for GroupBalanceUpdatedEvent and broadcasts the new balance
 * to all subscribers on /topic/groups/{groupId}/balances via STOMP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBalanceEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final BalanceService balanceService;

    @Async
    @EventListener
    public void onBalanceUpdated(GroupBalanceUpdatedEvent event) {
        Long groupId = event.getGroupId();
        log.debug("Broadcasting balance update for group {}", groupId);
        try {
            // We broadcast the groupId and a notification flag;
            // clients re-fetch the balance from /api/groups/{id}/balance
            var notification = java.util.Map.of(
                    "groupId", groupId,
                    "type", "BALANCE_UPDATED"
            );
            messagingTemplate.convertAndSend(
                    "/topic/groups/" + groupId + "/balances",
                    notification
            );
        } catch (Exception e) {
            log.error("Failed to broadcast balance update for group {}: {}", groupId, e.getMessage());
        }
    }
}
