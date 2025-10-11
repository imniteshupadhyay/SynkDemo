package com.playmotech.api.core.services;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.PostDto;
import com.playmotech.api.core.dto.PostRequestDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IPostService {
        PostDto createPost(PostRequestDto post, List<FileObjectDto> fileObjectDtos, UserDetail user,
                        List<FileObjectDto> thumbnailFileObjectDtos)
                        throws ResourceException;

        PostDto updatePost(String postId, PostRequestDto updatePostDto, List<FileObjectDto> fileObjectDtos,
                        UserDetail userDetail, List<FileObjectDto> thumbnailFileObjectDtos) throws ResourceException;

        List<PostDto> getAllPostsForUser(String userId, String orgId, String packageName) throws ResourceException;

        void deletePost(String id, UserDetail userDetail) throws ResourceException;

        void likePost(String postId, String userId) throws ResourceException;

        void dislikePost(String postId, String userId) throws ResourceException;

        List<UserProfileMinDto> getAllLikedUserForPost(String userId, String postId) throws ResourceException;

        List<UserProfileMinDto> getAllDisLikedUserForPost(String userId, String postId) throws ResourceException;

        void reportPost(String reportedByUserId, String postId);

        List<PostDto> getPostsForUserPaginated(String userId, String orgId, String packageName,
                        Integer page, Integer size, String sortBy, String sortDir
        // , List<String> statusFilter
        ) throws ResourceException;

        /**
         * Get the total number of elements and pages for posts query
         * 
         * @param userId      The ID of the user
         * @param orgId       The organization ID
         * @param packageName The package name
         * @param size        The request size for paginated query
         * @return Pair of totalPages and totalElements
         * @throws ResourceException If there's an error retrieving the data
         */
        Pair<Integer, Long> getTotalPagesAndElements(String userId, String orgId, String packageName, Integer size);
}
