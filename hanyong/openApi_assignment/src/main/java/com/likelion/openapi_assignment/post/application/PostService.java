package com.likelion.openapi_assignment.post.application;

import com.likelion.openapi_assignment.common.client.TagRecommendationClient;
import com.likelion.openapi_assignment.common.error.ErrorCode;
import com.likelion.openapi_assignment.common.exception.BusinessException;
import com.likelion.openapi_assignment.common.s3.S3Uploader;
import com.likelion.openapi_assignment.member.domain.Member;
import com.likelion.openapi_assignment.member.domain.repository.MemberRepository;
import com.likelion.openapi_assignment.post.api.dto.request.PostSaveRequestDto;
import com.likelion.openapi_assignment.post.api.dto.request.PostUpdateRequestDto;
import com.likelion.openapi_assignment.post.api.dto.response.PostInfoResponseDto;
import com.likelion.openapi_assignment.post.api.dto.response.PostListResponseDto;
import com.likelion.openapi_assignment.post.domain.Post;
import com.likelion.openapi_assignment.post.domain.repository.PostRepository;
import com.likelion.openapi_assignment.posttag.domain.PostTag;
import com.likelion.openapi_assignment.posttag.domain.repository.PostTagRepository;
import com.likelion.openapi_assignment.tag.domain.Tag;
import com.likelion.openapi_assignment.tag.domain.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final TagRecommendationClient tagClient;
    private final S3Uploader s3Uploader;

    // 게시물 저장
    @Transactional
    public PostInfoResponseDto postSave(PostSaveRequestDto postSaveRequestDto, MultipartFile imageFile) {
        //회원 조회
        Member member = memberRepository.findById(postSaveRequestDto.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_EXCEPTION,
                        ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + postSaveRequestDto.memberId()));

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3Uploader.upload(imageFile, "post-images");
        }

        //게시물 생성
        Post post = Post.builder()
                .title(postSaveRequestDto.title())
                .contents(postSaveRequestDto.contents())
                .imageUrl(imageUrl)
                .member(member)
                .build();

        postRepository.save(post);

        List<String> tagNames = tagClient.getRecommendedTags(post.getContents());
        registerTagsToPost(post, tagNames);
        //Fetch Join으로 태그 포홤된 post 다시 조회
        Post postWithTags = postRepository.findByIdWithTags(post.getPostId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION,
                        ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + post.getPostId()));

        return PostInfoResponseDto.from(post);
    }

    // 특정 작성자가 작성한 게시글 목록을 조회
    public PostListResponseDto postFindMember(Long memberId) {
        //회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND_EXCEPTION,
                        ErrorCode.MEMBER_NOT_FOUND_EXCEPTION.getMessage() + memberId));
        //회원이 작성한 모든 게시글 가져온다
        List<Post> posts = postRepository.findByMember(member);
        List<PostInfoResponseDto> postInfoResponseDtos = posts.stream()
                .map(PostInfoResponseDto::from) //태그 포함
                .toList();

        return PostListResponseDto.from(postInfoResponseDtos);
    }

    // 게시물 수정
    @Transactional
    public PostInfoResponseDto postUpdate(Long postId, PostUpdateRequestDto postUpdateRequestDto, MultipartFile imageFile) {
        Post postWithTags = postRepository.findByIdWithTags(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION,
                        ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));

        if (imageFile != null && !imageFile.isEmpty()) {
            //이전 이미지가 있다면 삭제
            String oldImageUrl = postWithTags.getImageUrl();
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                s3Uploader.delete(oldImageUrl);
            }
            //새 이미지 업로드
            String newImageUrl = s3Uploader.upload(imageFile, "post-images");
            postWithTags.updateImage(newImageUrl);
        }
        //게시글 내용 수정
        postWithTags.update(postUpdateRequestDto);

        // 기존 태그(PostTag) 제거
        postTagRepository.deleteAllByPost(postWithTags);
        postWithTags.getPostTags().clear(); // 양방향 관계 유지

        // 수정된 내용으로 추천 태그 재생성 및 등록
        List<String> tagNames = tagClient.getRecommendedTags(postWithTags.getContents());
        registerTagsToPost(postWithTags, tagNames);

        return PostInfoResponseDto.from(postWithTags);
    }

    // 게시물 삭제
    @Transactional
    public void postDelete(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION,
                        ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));

        //게시글 삭제
        postRepository.delete(post);
    }
    //이미지만 삭제
    @Transactional
    public PostInfoResponseDto deleteImage(Long postId) {
        Post post = getPostWithTags(postId);

        String oldImageUrl =  post.getImageUrl();

        if (oldImageUrl == null || oldImageUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND_EXCEPTION, ErrorCode.IMAGE_NOT_FOUND_EXCEPTION.getMessage());
        }
        s3Uploader.delete(oldImageUrl);
        post.updateImage(null);

        Post postWithTags = getPostWithTags(postId);
        return PostInfoResponseDto.from(postWithTags);
    }

    // 게시물 추천 태그 목록 등록 및 PostTag 연관 엔티티 저장
    private void registerTagsToPost(Post post, List<String> tagNames) {
        for (String tagName : tagNames) {
            // 기존 태그가 있다면 사용, 없으면 새로 생성
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));

            // PostTag 생성 및 연관 관계 추가
            PostTag postTag = new PostTag(post, tag);
            post.getPostTags().add(postTag);   // 양방향 매핑 유지
            postTagRepository.save(postTag);
        }
    }
    private Post getPostWithTags(Long postId) {
        return postRepository.findByIdWithTags(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND_EXCEPTION,
                        ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage() + postId));
    }

}