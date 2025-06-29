package com.likelion.openapi_assignment.restWeather.api.dto.response;

public record WeatherResponseDto(
        String unitCode, //휴게소 코드
        String unitName, //휴게소 이름
        String weatherContents //날씨 상태
) {
}
