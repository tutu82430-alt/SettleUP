package io.settleup.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a group's balance changes (expense added/deleted or settlement confirmed).
 * Triggers WebSocket broadcast to all group members.
 */
@Getter
public class GroupBalanceUpdatedEvent extends ApplicationEvent {

    private final Long groupId;

    public GroupBalanceUpdatedEvent(Object source, Long groupId) {
        super(source);
        this.groupId = groupId;
    }
}
