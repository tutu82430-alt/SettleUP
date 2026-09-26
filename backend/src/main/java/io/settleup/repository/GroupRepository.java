package io.settleup.repository;

import io.settleup.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByInviteCode(String inviteCode);

    @Query("""
        SELECT g FROM Group g
        JOIN g.members m
        WHERE m.user.id = :userId
        ORDER BY g.createdAt DESC
        """)
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM GroupMember m
        WHERE m.group.id = :groupId AND m.user.id = :userId
        """)
    boolean isUserMemberOfGroup(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
