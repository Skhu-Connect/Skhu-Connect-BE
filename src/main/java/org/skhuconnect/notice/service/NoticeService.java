package org.skhuconnect.notice.service;

import org.skhuconnect.admin.entity.Admin;
import org.skhuconnect.admin.repository.AdminRepository;
import org.skhuconnect.notice.dto.*;
import org.skhuconnect.notice.entity.*;
import org.skhuconnect.notice.repository.NoticeRepository;
import org.skhuconnect.admin.notificationlog.entity.*;
import org.skhuconnect.admin.notificationlog.service.AdminNotificationLogService;
import org.skhuconnect.notification.service.NotificationEventService;
import org.skhuconnect.user.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {
    private final NoticeRepository notices; private final AdminRepository admins; private final UserRepository users; private final NotificationEventService events; private final AdminNotificationLogService logs;
    public NoticeService(NoticeRepository notices, AdminRepository admins, UserRepository users, NotificationEventService events, AdminNotificationLogService logs){this.notices=notices;this.admins=admins;this.users=users;this.events=events;this.logs=logs;}
    @Transactional public NoticeResponse create(Long adminId, NoticeCreateRequest r){Admin a=admins.findById(adminId).orElseThrow();return NoticeResponse.from(notices.save(Notice.create(a,r.title(),r.content())));}
    @Transactional public NoticeResponse update(Long id, NoticeCreateRequest r){Notice n=notices.findById(id).orElseThrow();n.update(r.title(),r.content());return NoticeResponse.from(n);}
    @Transactional public NoticeResponse publish(Long adminId, Long id){Notice n=notices.findById(id).orElseThrow();if(n.publish()){events.onNoticePublished(n.getId(),n.getTitle(),users.findAll());Admin a=admins.findById(adminId).orElseThrow();logs.record(NotificationLogType.NOTICE_PUBLISHED,a,NotificationLogTargetType.NOTICE,n.getId(),"공지사항 최초 발행");}return NoticeResponse.from(n);}
    @Transactional public NoticeResponse hide(Long id){Notice n=notices.findById(id).orElseThrow();n.hide();return NoticeResponse.from(n);}
    @Transactional public NoticeResponse republish(Long id){Notice n=notices.findById(id).orElseThrow();n.publish();return NoticeResponse.from(n);}
    @Transactional(readOnly=true) public Page<NoticeResponse> published(int page,int size){return notices.findByStatusOrderByCreatedAtDescIdDesc(NoticeStatus.PUBLISHED,PageRequest.of(page,size)).map(NoticeResponse::from);}
}
