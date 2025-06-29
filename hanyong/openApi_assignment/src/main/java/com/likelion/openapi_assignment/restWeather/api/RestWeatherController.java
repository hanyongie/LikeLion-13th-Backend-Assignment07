package com.likelion.openapi_assignment.restWeather.api;

import com.likelion.openapi_assignment.common.error.SuccessCode;
import com.likelion.openapi_assignment.common.template.ApiResTemplate;
import com.likelion.openapi_assignment.restWeather.api.dto.response.WeatherListResponseDto;
import com.likelion.openapi_assignment.restWeather.api.dto.response.WeatherResponseDto;
import com.likelion.openapi_assignment.restWeather.application.RestWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/weather")
public class RestWeatherController {
    private final RestWeatherService restWeatherService;

    @GetMapping("/all")
    public ApiResTemplate<WeatherListResponseDto> getAllWeather(
            @RequestParam String sdate, //날짜
            @RequestParam String stdHour //시간대 파라미터로 입력받는다
    ) {
        //외부 API로 특정 날짜와 시간에 해당하는 닐씨를 가져온다
        List<WeatherResponseDto> weatherList = restWeatherService.getWeather(sdate, stdHour);
        WeatherListResponseDto responseDto = new WeatherListResponseDto(weatherList.size(),weatherList);//WeatherListResponseDto 형태로 감싼것
        return ApiResTemplate.successResponse(SuccessCode.GET_SUCCESS, responseDto);
    }
}
