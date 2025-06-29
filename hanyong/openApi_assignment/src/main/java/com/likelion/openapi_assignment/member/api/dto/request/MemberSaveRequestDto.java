package com.likelion.openapi_assignment.member.api.dto.request;

import com.likelion.openapi_assignment.member.domain.Part;

public record MemberSaveRequestDto(
        String name,
        int age,
        Part part
) {
}
