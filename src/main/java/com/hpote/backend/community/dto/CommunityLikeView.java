package com.hpote.backend.community.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommunityLikeView {
    String postId;
    boolean liked;
    long likeCount;
}
