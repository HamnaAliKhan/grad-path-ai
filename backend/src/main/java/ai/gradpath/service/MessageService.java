package ai.gradpath.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.domain.Message;
import ai.gradpath.domain.Student;
import ai.gradpath.domain.User;
import ai.gradpath.repository.MessageRepository;
import ai.gradpath.repository.UserRepository;
import ai.gradpath.validation.Validators;

/**
 * Direct messaging between students and career officers. Only the two participants of a conversation can read it.
 * Students message officers and officers message students; fictional demo accounts can only talk to each other,
 * so demo data is never mixed with real conversations.
 */
@Service
public class MessageService {

    private final MessageRepository messages;
    private final UserRepository users;

    public MessageService(MessageRepository messages, UserRepository users) {
        this.messages = messages;
        this.users = users;
    }

    /** One conversation in the list. */
    public record Conversation(Integer partnerId, String name, String role, String last, LocalDateTime time, int unread) { }

    /** A person the user may start a conversation with. */
    public record Contact(Integer id, String label) { }

    /** People on the other side whom this user may message. */
    @Transactional(readOnly = true)
    public List<Contact> contacts(User me) {
        List<Contact> out = new ArrayList<>();
        if (me instanceof CareerOfficer) {
            for (Student s : users.registeredStudents()) {
                if (s.isDemo() == me.isDemo()) {
                    out.add(new Contact(s.getId(), s.getName() + " (" + (s.getDepartment().isBlank() ? "Student" : s.getDepartment()) + ")"));
                }
            }
        } else {
            for (CareerOfficer o : users.officers()) {
                if (o.canSignIn() && o.isDemo() == me.isDemo()) {
                    out.add(new Contact(o.getId(), o.getName() + " - " + o.getOffice()));
                }
            }
        }
        return out;
    }

    /** Whether the two accounts may exchange messages. */
    public boolean canMessage(User me, User other) {
        return other != null && !other.getId().equals(me.getId()) && other.getRole() != me.getRole()
                && other.isDemo() == me.isDemo() && other.canSignIn();
    }

    @Transactional
    public Result<Message> send(User me, Integer recipientId, String body) {
        String err = Validators.checkText(body, "Message", 1, 1500);
        if (err != null) {
            return Result.fail(err);
        }
        Optional<User> recipient = recipientId == null ? Optional.empty() : users.findById(recipientId);
        if (recipient.isEmpty() || !canMessage(me, recipient.get())) {
            return Result.fail("You can only message " + (me instanceof Student ? "Career Officers" : "registered students")
                    + (me.isDemo() ? " in the demo" : "") + ".");
        }
        return Result.ok(messages.save(new Message(me.getId(), recipientId, body.strip(), null)));
    }

    /** Each conversation's latest message and unread count, newest first. */
    @Transactional(readOnly = true)
    public List<Conversation> conversations(Integer userId) {
        Map<Integer, Message> latest = new LinkedHashMap<>();
        Map<Integer, Integer> unread = new HashMap<>();
        for (Message m : messages.involving(userId)) {
            Integer other = m.getSenderId().equals(userId) ? m.getRecipientId() : m.getSenderId();
            latest.put(other, m);
            if (m.getRecipientId().equals(userId) && !m.isRead()) {
                unread.merge(other, 1, Integer::sum);
            }
        }
        Map<Integer, User> partners = new HashMap<>();
        users.findAllById(latest.keySet()).forEach(u -> partners.put(u.getId(), u));
        List<Conversation> out = new ArrayList<>();
        latest.forEach((other, m) -> {
            User u = partners.get(other);
            if (u != null) {
                String body = m.getBody();
                out.add(new Conversation(other, u.getName(), u.displayRole(),
                        body.length() > 70 ? body.substring(0, 70) + "..." : body, m.getCreatedAt(),
                        unread.getOrDefault(other, 0)));
            }
        });
        out.sort(Comparator.comparing(Conversation::time).reversed());
        return out;
    }

    /** Mark the partner's messages to me as read and return the thread (only my own conversations). */
    @Transactional
    public List<Message> thread(Integer userId, Integer partnerId) {
        messages.markRead(userId, partnerId);
        return messages.between(userId, partnerId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Integer userId) {
        return messages.unreadCount(userId);
    }

    @Transactional(readOnly = true)
    public Optional<User> partner(Integer partnerId) {
        return users.findById(partnerId);
    }
}
