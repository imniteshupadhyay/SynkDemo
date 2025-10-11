package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.PostDto;
import com.playmotech.api.core.dto.PostRequestDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IPostService;
import com.playmotech.api.core.utils.ThumbnailGenerator;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts")
public class PostController extends BaseController {

    @Autowired
    private IPostService postService;

    @PostMapping
    public ResponseEntity<Response<PostDto>> createPost(@Valid PostRequestDto post,
            @RequestParam(value = "file", required = false) MultipartFile[] mediaFile) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        try {
            // media multipart[]
            List<FileObjectDto> fileObjectDtos = new ArrayList<>();
            if (mediaFile != null) {
                for (MultipartFile file : mediaFile) {
                    FileObjectDto fileObjectDto = new FileObjectDto();
                    fileObjectDto.setOriginalFilename(file.getOriginalFilename());
                    fileObjectDto.setContent(file.getBytes());
                    fileObjectDto.setContentType(file.getContentType());
                    fileObjectDtos.add(fileObjectDto);
                }
            }

            // thumbnail multipart[]
            List<FileObjectDto> thumbnailFileObjectDtos = new ArrayList<>();
            if (mediaFile != null) {
                for (MultipartFile thumbnailMediaFile : mediaFile) {

                    FileObjectDto geneatedThumbnail = ThumbnailGenerator.generateThumbnail(thumbnailMediaFile);
                    if (geneatedThumbnail != null) {
                        thumbnailFileObjectDtos.add(geneatedThumbnail);
                    }
                }
            }
            PostDto createdPost = postService.createPost(post, fileObjectDtos, currentUser, thumbnailFileObjectDtos);
            return ResponseEntity.status(HttpStatus.OK).body(Response.<PostDto>builder().status(HttpStatus.OK.value())
                    .message("success").body(createdPost).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<PostDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Response<PostDto>> updatePost(@PathVariable("postId") String postId,
            @RequestBody PostRequestDto updatePostRequestDto,
            @RequestParam(value = "file", required = false) MultipartFile[] mediaFile) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            List<FileObjectDto> fileObjectDtos = new ArrayList<>();
            List<FileObjectDto> thumbnailFileObjectDtos = new ArrayList<>();
            if (mediaFile != null) {
                for (MultipartFile file : mediaFile) {
                    FileObjectDto fileObjectDto = new FileObjectDto();
                    fileObjectDto.setOriginalFilename(file.getOriginalFilename());
                    fileObjectDto.setContent(file.getBytes());
                    fileObjectDto.setContentType(file.getContentType());
                    fileObjectDtos.add(fileObjectDto);

                    FileObjectDto generatedThumbnail = ThumbnailGenerator.generateThumbnail(file);
                    if (generatedThumbnail != null) {
                        thumbnailFileObjectDtos.add(generatedThumbnail);
                    }
                }
            }

            PostDto updatedPost = postService.updatePost(postId, updatePostRequestDto, fileObjectDtos, currentUser,
                    thumbnailFileObjectDtos);

            return ResponseEntity.status(HttpStatus.OK).body(Response.<PostDto>builder().status(HttpStatus.OK.value())
                    .message("Post updated successfully").body(updatedPost).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<PostDto>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @PostMapping("/{postId}/report")
    public ResponseEntity<Response> reportPost(@PathVariable("postId") String postId) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();
        postService.reportPost(currentUser.getUserId(), postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Response.<PostDto>builder()
                .status(HttpStatus.CREATED.value()).message("Post Reported request accepted").build());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<List<PostDto>>> getAllPostsForUser(
            @RequestParam(defaultValue = "", required = false) String orgId,
            @RequestParam(defaultValue = "", required = false) String packageName,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            // TODO: Remove orgId once clients starts sending packageName
            if (page != null || size != null || sortBy != null) {
                List<PostDto> posts = postService.getPostsForUserPaginated(currentUser.getUserId(), orgId, packageName,
                        page, size, sortBy, sortDir);
                // Get pagination information from the service
                Pair<Integer, Long> totalPagesAndElements = postService
                        .getTotalPagesAndElements(currentUser.getUserId(), orgId, packageName, size);

                return ResponseEntity.status(HttpStatus.OK).body(Response.<List<PostDto>>builder()
                        .status(HttpStatus.OK.value())
                        .message("success")
                        .body(posts)
                        .totalPages(totalPagesAndElements.getLeft())
                        .totalElements(totalPagesAndElements.getRight())
                        .build());
            }
            List<PostDto> posts = postService.getAllPostsForUser(currentUser.getUserId(), orgId, packageName);
            return ResponseEntity.status(HttpStatus.OK).body(Response.<List<PostDto>>builder()
                    .status(HttpStatus.OK.value()).message("success").body(posts).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<List<PostDto>>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @DeleteMapping(value = "/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Void>> deletePost(@PathVariable("postId") String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            // Ensure the user is allowed to delete the post
            postService.deletePost(postId, currentUser);

            return ResponseEntity.status(HttpStatus.OK).body(Response.<Void>builder().status(HttpStatus.OK.value())
                    .message("Post deleted successfully").build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<Void>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @PostMapping(value = "/{postId}/like", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Void>> likePost(@PathVariable("postId") String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            postService.likePost(id, currentUser.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(
                    Response.<Void>builder().status(HttpStatus.OK.value()).message("Post liked successfully").build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<Void>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @PostMapping(value = "/{postId}/dislike", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Void>> dislikePost(@PathVariable("postId") String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            postService.dislikePost(id, currentUser.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(Response.<Void>builder().status(HttpStatus.OK.value())
                    .message("Post disliked successfully").build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<Void>builder()
                    .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
        }
    }

    @GetMapping(value = "{postId}/likedusers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<List<UserProfileMinDto>>> getAllLikedUserForPost(
            @PathVariable("postId") String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            List<UserProfileMinDto> likedUserForPost = postService.getAllLikedUserForPost(currentUser.getUserId(),
                    postId);
            return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
                    .status(HttpStatus.OK.value()).message("success").body(likedUserForPost).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                    .body(Response.<List<UserProfileMinDto>>builder().status(e.getErrorCodes().getCustomError())
                            .message(e.getMessage()).build());
        }
    }

    @GetMapping(value = "{postId}/dislikedusers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<List<UserProfileMinDto>>> getAllDisLikedUserForPost(
            @PathVariable("postId") String postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetail currentUser = (UserDetail) authentication.getPrincipal();

        try {
            List<UserProfileMinDto> likedUserForPost = postService.getAllDisLikedUserForPost(currentUser.getUserId(),
                    postId);
            return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileMinDto>>builder()
                    .status(HttpStatus.OK.value()).message("success").body(likedUserForPost).build());
        } catch (ResourceException e) {
            return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
                    .body(Response.<List<UserProfileMinDto>>builder().status(e.getErrorCodes().getCustomError())
                            .message(e.getMessage()).build());
        }
    }

}
