package com.likelion.openapi_assignment.restWeather.application;

import com.likelion.openapi_assignment.common.error.ErrorCode;
import com.likelion.openapi_assignment.common.exception.BusinessException;
import com.likelion.openapi_assignment.restWeather.api.dto.response.WeatherResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor //final 필드를 자동으로 생성자 넣어준다
@Slf4j //log 남기게 객체 생성
public class RestWeatherService {
    private final RestTemplate restTemplate; //Http 요청 보내기 위한 객체

    @Value("${weather-api.base-url}")
    private String baseUrl;

    @Value("${weather-api.service-key}")
    private String serviceKey;

    //외부 API 데이터 받아오는 메서드
    public List<WeatherResponseDto> getWeather(String sdate, String stdHour) {
        //요청 URL
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("key", serviceKey)
                .queryParam("type", "json")
                .queryParam("sdate", sdate) //날짜 입력 받는다
                .queryParam("stdHour", stdHour) //시간 입력 받는다
                .build()
                .toUri();

        //외부 API 호출
        ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
        log.info("API URL :{} " , uri.toString());

        //응답 body가 null인 경우 예외 발생
        Map<String, Object> body = Optional.ofNullable(response.getBody())
                .orElseThrow(() -> new BusinessException(ErrorCode.WEATHER_API_RESPONSE_NULL, ErrorCode.WEATHER_API_RESPONSE_NULL.getMessage()));

        //응답에서 list 항목 추출
        List<Map<String, Object>> items = extractWeatherItemList(body);

        //list 항목을(items) WeatherResponseDto 형태로 변환
        return items.stream()
                .map(this::toDto)//Map -> DTO
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")//Java 컴파일러 경고를 무시 애노테이션
    private List<Map<String, Object>> extractWeatherItemList(Map<String, Object> responseMap) {
        Object listObj = responseMap.get("list"); //Map형태로 받아온 (JSON 응답 중)list 값 추출

        if (listObj instanceof List<?> list) { //List타입이면 Map으로 형변환
            return (List<Map<String, Object>>) list;
        }

        throw new BusinessException(ErrorCode.WEATHER_API_ITEM_MALFORMED, ErrorCode.WEATHER_API_ITEM_MALFORMED.getMessage());
    }
    //Map 형태의 날씨 데이터 DTO로 변환
    private WeatherResponseDto toDto(Map<String, Object> item) {
        return new WeatherResponseDto(
                String.valueOf(item.getOrDefault("unitCode", "")), //휴게소 코드
                String.valueOf(item.getOrDefault("unitName", "")), //휴게소 이름
                String.valueOf(item.getOrDefault("weatherContents", "")) //날씨 상태
        );
    }
}