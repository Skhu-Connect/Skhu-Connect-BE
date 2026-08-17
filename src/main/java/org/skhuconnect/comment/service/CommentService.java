package org.skhuconnect.comment.service;

import org.skhuconnect.comment.dto.request.*;
import org.skhuconnect.comment.dto.response.*;
import org.skhuconnect.comment.entity.Comment;
import org.skhuconnect.comment.exception.*;
import org.skhuconnect.comment.exception.CommentException.Reason;
import org.skhuconnect.comment.repository.*;
import org.skhuconnect.petition.repository.PetitionRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private static final int MAX_PAGE_SIZE = 100;
    private final CommentCreationTransaction creationTransaction;
    private final AnonymousNumberRetryService retryService;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PetitionRepository petitionRepository;
    private final Clock clock;

    public CommentService(CommentCreationTransaction creationTransaction,
            AnonymousNumberRetryService retryService, CommentRepository commentRepository,
            CommentLikeRepository commentLikeRepository, PetitionRepository petitionRepository, Clock clock) {
        this.creationTransaction=creationTransaction; this.retryService=retryService;
        this.commentRepository=commentRepository; this.commentLikeRepository=commentLikeRepository;
        this.petitionRepository=petitionRepository; this.clock=clock;
    }

    public CommentResponse create(Long userId, Long petitionId, CommentCreateRequest request) {
        try {
            return request.parentCommentId() == null
                    ? creationTransaction.create(userId, petitionId, request.content())
                    : creationTransaction.create(userId, petitionId, request.content(), request.parentCommentId());
        } catch (AnonymousNumberCollisionException exception) {
            return request.parentCommentId() == null
                    ? retryService.retryOnce(userId, petitionId, request.content())
                    : retryService.retryOnce(userId, petitionId, request.content(), request.parentCommentId());
        }
    }

    @Transactional(readOnly = true)
    public CommentPageResponse findAll(Long userId, Long petitionId, int page, int size) {
        validatePage(page,size);
        petitionRepository.findByIdAndDeletedFalseAndHiddenFalse(petitionId)
                .orElseThrow(() -> new CommentException(Reason.PETITION_NOT_FOUND));
        Pageable pageRequest = PageRequest.of(page,size,Sort.by(Sort.Order.asc("createdAt"),Sort.Order.asc("id")));
        Page<Comment> roots = userId == null
                ? commentRepository.findRootPage(petitionId, pageRequest)
                : commentRepository.findRootPageExcludingBlocked(petitionId, userId, pageRequest);
        List<Long> rootIds=roots.getContent().stream().map(Comment::getId).toList();
        List<Comment> replies = rootIds.isEmpty() ? List.of() : userId == null
                ? commentRepository.findByParentCommentIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(rootIds)
                : commentRepository.findRepliesExcludingBlocked(rootIds, userId);
        Map<Long,List<Comment>> grouped=replies.stream().collect(Collectors.groupingBy(
                c->c.getParentComment().getId(),LinkedHashMap::new,Collectors.toList()));
        List<Comment> all=new ArrayList<>(roots.getContent()); all.addAll(replies);
        List<Long> ids=all.stream().map(Comment::getId).toList();
        Map<Long,Long> counts=findLikeCounts(ids); Set<Long> liked=findLikedIds(userId,ids);
        List<CommentResponse> content=roots.getContent().stream().map(root->{
            List<CommentResponse> childResponses=grouped.getOrDefault(root.getId(),List.of()).stream()
                    .map(reply->CommentResponse.from(reply,counts.getOrDefault(reply.getId(),0L),userId,liked.contains(reply.getId())))
                    .toList();
            return CommentResponse.from(root,counts.getOrDefault(root.getId(),0L),userId,
                    liked.contains(root.getId()),childResponses);
        }).toList();
        return new CommentPageResponse(content,roots.getNumber(),roots.getSize(),roots.getTotalElements(),
                roots.getTotalPages(),roots.isFirst(),roots.isLast());
    }

    @Transactional
    public CommentResponse update(Long userId,Long petitionId,Long commentId,CommentUpdateRequest request){
        Comment c=findComment(petitionId,commentId); validateOwner(c,userId);
        if(c.isHidden()) throw new CommentException(Reason.COMMENT_NOT_EDITABLE);
        c.update(request.content());
        return CommentResponse.from(c,commentLikeRepository.countByCommentId(commentId),userId,
                commentLikeRepository.existsByCommentIdAndUserId(commentId,userId));
    }
    @Transactional
    public void delete(Long userId,Long petitionId,Long commentId){
        Comment c=findComment(petitionId,commentId); validateOwner(c,userId); c.delete(LocalDateTime.now(clock));
    }
    private Comment findComment(Long petitionId,Long commentId){
        return commentRepository.findByIdAndPetitionIdAndDeletedFalse(commentId,petitionId)
                .orElseThrow(()->new CommentException(Reason.COMMENT_NOT_FOUND));
    }
    private void validateOwner(Comment c,Long userId){if(!c.isWrittenBy(userId))throw new CommentException(Reason.COMMENT_FORBIDDEN);}
    private Map<Long,Long> findLikeCounts(List<Long> ids){
        Map<Long,Long> result=new HashMap<>();
        if(!ids.isEmpty()) for(CommentLikeCount c:commentLikeRepository.countByCommentIds(ids)) result.put(c.getCommentId(),c.getLikeCount());
        return result;
    }
    private Set<Long> findLikedIds(Long userId,List<Long> ids){
        if(userId==null||ids.isEmpty())return Set.of();
        return new HashSet<>(commentLikeRepository.findLikedCommentIds(userId,ids));
    }
    private void validatePage(int page,int size){if(page<0||size<1||size>MAX_PAGE_SIZE)throw new CommentException(Reason.INVALID_PAGE);}
}
