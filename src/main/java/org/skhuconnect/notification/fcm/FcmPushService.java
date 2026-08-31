package org.skhuconnect.notification.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.skhuconnect.notification.dto.NotificationResponse;
import org.skhuconnect.notification.entity.FcmToken;
import org.skhuconnect.notification.entity.Notification;
import org.skhuconnect.notification.repository.FcmTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final FcmTokenRepository tokens;
    private final FirebaseMessaging messaging;

    public FcmPushService(FcmTokenRepository tokens,
            @Value("${app.fcm.service-account-json:}") String serviceAccountJson) {
        this.tokens = tokens;
        this.messaging = initialize(serviceAccountJson);
    }

    /**
     * Runs after the notification transaction commits, on a separate thread: a rolled back
     * notification never produces a push, and the FCM round trip never delays the API response.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(PushMessage message) {
        if (messaging == null) return;
        List<FcmToken> targets = tokens.findByUserId(message.receiverId());
        if (targets.isEmpty()) {
            log.info("no fcm token, push skipped: receiverId={}, notificationId={}",
                    message.receiverId(), message.notificationId());
            return;
        }
        Message.Builder base = Message.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putData("notificationId", String.valueOf(message.notificationId()));
        if (message.petitionId() != null) base.putData("petitionId", String.valueOf(message.petitionId()));
        // ponytail: one call per token. MulticastMessage would collapse them into one, but its token
        // API is deprecated in firebase-admin 9.10 and a user owns a handful of devices at most.
        for (FcmToken token : targets) {
            try {
                messaging.send(base.setToken(token.getToken()).build());
            } catch (FirebaseMessagingException exception) {
                MessagingErrorCode code = exception.getMessagingErrorCode();
                // Never log the token itself - it is a device credential.
                log.warn("fcm send failed: receiverId={}, notificationId={}, tokenId={}, code={}",
                        message.receiverId(), message.notificationId(), token.getId(), code);
                if (isInvalid(code)) {
                    // deleteById, not delete(entity): the listener runs outside the transaction that
                    // loaded it, so the entity is detached and merging it would touch a stale proxy.
                    tokens.deleteById(token.getId());
                    log.info("stale fcm token deleted: tokenId={}, code={}", token.getId(), code);
                }
            }
        }
    }

    private boolean isInvalid(MessagingErrorCode code) {
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    /** Returns null when push is unavailable, so a credential problem disables push instead of breaking startup. */
    private static FirebaseMessaging initialize(String serviceAccountJson) {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            log.warn("app.fcm.service-account-json is empty - push notifications are disabled");
            return null;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))))
                        .build());
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception exception) {
            log.error("firebase initialization failed - push notifications are disabled", exception);
            return null;
        }
    }

    /**
     * Snapshot taken inside the notification transaction. The listener runs after commit on another
     * thread, where touching a lazy association on the entity would blow up.
     */
    public record PushMessage(Long receiverId, String title, String body, Long notificationId, Long petitionId) {
        public static PushMessage from(Notification notification) {
            return new PushMessage(
                    notification.getReceiver().getId(),
                    notification.getTitle() != null ? notification.getTitle() : "SKHU Connect",
                    notification.getBody() != null ? notification.getBody()
                            : NotificationResponse.message(notification),
                    notification.getId(),
                    notification.getPetition() == null ? null : notification.getPetition().getId());
        }
    }
}
