package com.nalssilog.location.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.config.KakaoMapProperties;
import com.nalssilog.location.domain.LocationErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class KakaoMapClient {

    /** 기존 Location 데이터가 사용하는 법정동 코드 체계(B)를 유지한다. */
    private static final String LEGAL_REGION = "B";

    private final RestClient restClient;
    private final String reverseGeocodePath;

    @Autowired
    public KakaoMapClient(KakaoMapProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                .build();
        this.reverseGeocodePath = properties.reverseGeocodePath();
    }

    KakaoMapClient(RestClient restClient, String reverseGeocodePath) {
        this.restClient = restClient;
        this.reverseGeocodePath = reverseGeocodePath;
    }

    public KakaoRegion reverseGeocode(double latitude, double longitude) {
        try {
            KakaoRegionResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(reverseGeocodePath)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("input_coord", "WGS84")
                            .build())
                    .retrieve()
                    .body(KakaoRegionResponse.class);

            if (response == null || response.documents() == null) {
                throw new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND);
            }

            return response.documents().stream()
                    .filter(document -> LEGAL_REGION.equals(document.regionType()))
                    .filter(KakaoRegionDocument::hasRequiredValues)
                    .findFirst()
                    .map(KakaoRegionDocument::toRegion)
                    .orElseThrow(() -> new NalssiLogException(LocationErrorCode.LOCATION_NOT_FOUND));
        } catch (NalssiLogException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Kakao Map API response error (status={})", e.getStatusCode().value());
            throw new NalssiLogException(LocationErrorCode.KAKAO_MAP_API_UNAVAILABLE);
        } catch (RestClientException e) {
            log.warn("Kakao Map API request failed: {}", e.getMessage());
            throw new NalssiLogException(LocationErrorCode.KAKAO_MAP_API_UNAVAILABLE);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoRegionResponse(List<KakaoRegionDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoRegionDocument(
            @JsonProperty("region_type") String regionType,
            String code,
            @JsonProperty("region_1depth_name") String sido,
            @JsonProperty("region_2depth_name") String sigungu,
            @JsonProperty("region_3depth_name") String dong,
            @JsonProperty("region_4depth_name") String ri,
            double x,
            double y
    ) {

        boolean hasRequiredValues() {
            return code != null && !code.isBlank()
                    && sido != null && !sido.isBlank()
                    && dong != null && !dong.isBlank();
        }

        KakaoRegion toRegion() {
            String legalDongName = ri == null || ri.isBlank() ? dong : dong + " " + ri;

            return new KakaoRegion(
                    code,
                    sido,
                    sigungu == null ? "" : sigungu,
                    legalDongName,
                    y,
                    x
            );
        }
    }
}
