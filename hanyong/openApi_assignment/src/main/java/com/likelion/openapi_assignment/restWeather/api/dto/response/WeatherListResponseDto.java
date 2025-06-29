package com.likelion.openapi_assignment.restWeather.api.dto.response;

import java.util.List;

public record WeatherListResponseDto( //응답 데이터를 감싸는 DTO
        int count, //날씨 데이터의 총 개수
        List<WeatherResponseDto> list //날씨 데이터 리스트
) {
}
