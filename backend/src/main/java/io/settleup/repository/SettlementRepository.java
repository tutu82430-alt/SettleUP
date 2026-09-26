package io.settleup.repository;

import io.settleup.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("""
        SELECT s FROM Settlement s
        JOIN FETCH s.fromUser
        JOIN FETCH s.toUser
        WHERE s.group.id = :groupId
        ORDER BY s.createdAt DESC
        """)
    List<Settlement> findByGroupIdWithDetails(@Param("groupId") Long groupId);

    @Query("""
        SELECT s FROM Settlement s
        JOIN FETCH s.fromUser
        JOIN FETCH s.toUser
        WHERE s.group.id = :groupId
        AND s.status = io.settleup.entity.Settlement$SettlementStatus.COMPLETED
        """)
    List<Settlement> findCompletedByGroupId(@Param("groupId") Long groupId);
}
