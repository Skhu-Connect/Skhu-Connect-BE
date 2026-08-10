package org.skhuconnect.notice.controller;
import jakarta.validation.Valid;
import org.skhuconnect.notice.dto.*;
import org.skhuconnect.notice.service.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/connect")
public class NoticeController { private final NoticeService s; public NoticeController(NoticeService s){this.s=s;}
 @GetMapping("/notices") public Page<NoticeResponse> published(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.published(page,size);}
 @PostMapping("/admin/notices") public NoticeResponse create(@RequestAttribute("adminId") Long a,@Valid @RequestBody NoticeCreateRequest r){return s.create(a,r);}
 @PutMapping("/admin/notices/{id}") public NoticeResponse update(@PathVariable Long id,@Valid @RequestBody NoticeCreateRequest r){return s.update(id,r);}
 @PatchMapping("/admin/notices/{id}/publish") public NoticeResponse publish(@RequestAttribute("adminId") Long adminId,@PathVariable Long id){return s.publish(adminId,id);}
 @PatchMapping("/admin/notices/{id}/hide") public NoticeResponse hide(@PathVariable Long id){return s.hide(id);}
 @PatchMapping("/admin/notices/{id}/republish") public NoticeResponse republish(@PathVariable Long id){return s.republish(id);}
}
