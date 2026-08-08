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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FcmPushService {
    private final FcmTokenRepository tokens;
    private final String serviceAccountJson;
    public FcmPushService(FcmTokenRepository tokens, @Value("${app.fcm.service-account-json:}") String serviceAccountJson) { this.tokens=tokens; this.serviceAccountJson=serviceAccountJson; }
    public void send(Notification notification) {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) return;
        FirebaseMessaging messaging = messaging();
        Message.Builder base = Message.builder()
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle("성공잇다")
                        .setBody(NotificationResponse.message(notification.getType()))
                        .build())
                .putData("notificationId", String.valueOf(notification.getId()));
        if (notification.getPetition() != null) base.putData("petitionId", String.valueOf(notification.getPetition().getId()));
        for (FcmToken token : tokens.findByUserId(notification.getReceiver().getId())) {
            try { messaging.send(base.setToken(token.getToken()).build()); }
            catch (FirebaseMessagingException exception) { if (isInvalid(exception)) tokens.delete(token); }
        }
    }
    private FirebaseMessaging messaging() {
        if (FirebaseApp.getApps().isEmpty()) try { FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)))).build()); } catch (Exception exception) { throw new IllegalStateException("Firebase initialization failed", exception); }
        return FirebaseMessaging.getInstance();
    }
    private boolean isInvalid(FirebaseMessagingException exception) { return exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED || exception.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT; }
}
