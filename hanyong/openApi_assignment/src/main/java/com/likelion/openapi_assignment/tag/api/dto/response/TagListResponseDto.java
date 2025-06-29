package com.likelion.openapi_assignment.tag.api.dto.response;

import com.likelion.openapi_assignment.tag.domain.Tag;
import lombok.Builder;

import java.util.List;

@Builder
public record TagListResponseDto(
        List<TagInfoResponseDto> tags
) {
    public static TagListResponseDto from(List<TagInfoResponseDto> tags){
        return TagListResponseDto.builder()
                .tags(tags)
                .build();
    }
}