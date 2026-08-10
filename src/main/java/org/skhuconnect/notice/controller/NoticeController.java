package org.skhuconnect.notice.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.skhuconnect.notice.dto.*;
import org.skhuconnect.notice.service.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
@Tag(name="Notice", description="공지사항 API")
@RestController @RequestMapping("/connect")
public class NoticeController { private final NoticeService s; public NoticeController(NoticeService s){this.s=s;}
 @Operation(summary="게시된 공지사항 조회") @GetMapping("/notices") public Page<NoticeResponse> published(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.published(page,size);}
 @Operation(summary="공지사항 작성") @PostMapping("/admin/notices") public NoticeResponse create(@RequestAttribute("adminId") Long a,@Valid @RequestBody NoticeCreateRequest r){return s.create(a,r);}
 @Operation(summary="공지사항 수정") @PutMapping("/admin/notices/{id}") public NoticeResponse update(@PathVariable Long id,@Valid @RequestBody NoticeCreateRequest r){return s.update(id,r);}
 @Operation(summary="공지사항 발행") @PatchMapping("/admin/notices/{id}/publish") public NoticeResponse publish(@RequestAttribute("adminId") Long adminId,@PathVariable Long id){return s.publish(adminId,id);}
 @Operation(summary="공지사항 숨김") @PatchMapping("/admin/notices/{id}/hide") public NoticeResponse hide(@PathVariable Long id){return s.hide(id);}
 @Operation(summary="공지사항 재공개") @PatchMapping("/admin/notices/{id}/republish") public NoticeResponse republish(@PathVariable Long id){return s.republish(id);}
}