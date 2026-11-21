package ai.gradpath.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import ai.gradpath.domain.Message;

/** Message threads and read status. */
public interface MessageRepository extends BaseRepository<Message> {

    @Query("select m from Message m where m.senderId = :userId or m.recipientId = :userId order by m.id")
    List<Message> involving(Integer userId);

    @Query("""
            select m from Message m
            where (m.senderId = :a and m.recipientId = :b) or (m.senderId = :b and m.recipientId = :a)
            order by m.id
            """)
    List<Message> between(Integer a, Integer b);

    /** Mark the partner's messages to me as read. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Message m set m.read = true where m.recipientId = :me and m.senderId = :other and m.read = false")
    int markRead(Integer me, Integer other);

    @Query("select count(m) from Message m where m.recipientId = :userId and m.read = false")
    long unreadCount(Integer userId);
}
