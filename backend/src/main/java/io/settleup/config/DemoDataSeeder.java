package io.settleup.config;

import io.settleup.dto.request.ExpenseRequest;
import io.settleup.entity.*;
import io.settleup.repository.*;
import io.settleup.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseService expenseService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemoData || userRepository.count() > 0) {
            log.info("Demo data seeding skipped (already populated or disabled).");
            return;
        }

        log.info("Seeding demo data for SettleUp application...");

        // 1. Create Users
        User demoUser = userRepository.save(User.builder()
                .username("demouser")
                .email("demo@settleup.app")
                .displayName("Demo User")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        User alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@settleup.app")
                .displayName("Alice Miller")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        User bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@settleup.app")
                .displayName("Bob Davis")
                .passwordHash(passwordEncoder.encode("password123"))
                .build());

        // 2. Create Group
        Group group = groupRepository.save(Group.builder()
                .name("Goa Trip 2026")
                .description("Beach vacation expenses with friends")
                .inviteCode("GOATRIP1")
                .currency(Group.GroupCurrency.USD)
                .createdBy(demoUser)
                .build());

        // Add Members
        groupMemberRepository.save(GroupMember.builder()
                .group(group).user(demoUser).role(GroupMember.MemberRole.ADMIN).build());
        groupMemberRepository.save(GroupMember.builder()
                .group(group).user(alice).role(GroupMember.MemberRole.MEMBER).build());
        groupMemberRepository.save(GroupMember.builder()
                .group(group).user(bob).role(GroupMember.MemberRole.MEMBER).build());

        // 3. Add Sample Expenses via ExpenseService
        List<Long> participants = List.of(demoUser.getId(), alice.getId(), bob.getId());

        ExpenseRequest req1 = new ExpenseRequest();
        req1.setDescription("Resort Beach Villa Stay");
        req1.setAmount(new BigDecimal("600.00"));
        req1.setSplitType(Expense.SplitType.EQUAL);
        req1.setCategory("Accommodation");
        req1.setExpenseDate(LocalDate.now().minusDays(3));
        req1.setParticipantIds(participants);
        expenseService.addExpense(group.getId(), req1, demoUser.getId());

        ExpenseRequest req2 = new ExpenseRequest();
        req2.setDescription("Seafood Dinner & Drinks");
        req2.setAmount(new BigDecimal("150.00"));
        req2.setSplitType(Expense.SplitType.EQUAL);
        req2.setCategory("Food & Drink");
        req2.setExpenseDate(LocalDate.now().minusDays(2));
        req2.setParticipantIds(participants);
        expenseService.addExpense(group.getId(), req2, alice.getId());

        ExpenseRequest req3 = new ExpenseRequest();
        req3.setDescription("Water Sports & Scuba");
        req3.setAmount(new BigDecimal("210.00"));
        req3.setSplitType(Expense.SplitType.EQUAL);
        req3.setCategory("Entertainment");
        req3.setExpenseDate(LocalDate.now().minusDays(1));
        req3.setParticipantIds(participants);
        expenseService.addExpense(group.getId(), req3, bob.getId());

        log.info("Demo data seeding completed successfully! Demo user: demo@settleup.app / password123");
    }
}
