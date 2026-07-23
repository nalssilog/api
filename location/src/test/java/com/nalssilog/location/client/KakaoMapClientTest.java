package com.nalssilog.location.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.location.domain.LocationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoMapClientTest {

    private static final String REVERSE_GEOCODE_PATH = "/v2/local/geo/coord2regioncode.json";

    private MockRestServiceServer server;
    private KakaoMapClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoMapClient(builder.build(), REVERSE_GEOCODE_PATH);
    }

    @Test
    void convertsCoordinatesToExistingLegalDongCodeSystem() {
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
                                + "?x=127.1086228&y=37.4012191&input_coord=WGS84"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "region_type": "H",
                              "code": "4113565500",
                              "region_1depth_name": "경기도",
                              "region_2depth_name": "성남시 분당구",
                              "region_3depth_name": "삼평동",
                              "x": 127.1163593869371,
                              "y": 37.40612091848614
                            },
                            {
                              "region_type": "B",
                              "code": "4113510900",
                              "region_1depth_name": "경기도",
                              "region_2depth_name": "성남시 분당구",
                              "region_3depth_name": "삼평동",
                              "x": 127.10459896729914,
                              "y": 37.40269721785548
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoRegion region = client.reverseGeocode(37.4012191, 127.1086228);

        assertThat(region).isEqualTo(new KakaoRegion(
                "4113510900",
                "경기도",
                "성남시 분당구",
                "삼평동",
                37.40269721785548,
                127.10459896729914
        ));
        server.verify();
    }

    @Test
    void mapsKakaoAuthenticationFailureToBadGatewayError() {
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
                                + "?x=127.0&y=37.0&input_coord=WGS84"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.reverseGeocode(37.0, 127.0))
                .isInstanceOfSatisfying(NalssiLogException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(LocationErrorCode.KAKAO_MAP_API_UNAVAILABLE));
        server.verify();
    }

    @Test
    void appendsRiForRuralLegalDong() {
        server.expect(requestTo(
                        "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
                                + "?x=127.423084873712&y=37.0789561558879&input_coord=WGS84"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "region_type": "B",
                              "code": "4155040021",
                              "region_1depth_name": "경기도",
                              "region_2depth_name": "안성시",
                              "region_3depth_name": "죽산면",
                              "region_4depth_name": "죽산리",
                              "x": 127.423084873712,
                              "y": 37.0789561558879
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoRegion region = client.reverseGeocode(37.0789561558879, 127.423084873712);

        assertThat(region.dong()).isEqualTo("죽산면 죽산리");
        server.verify();
    }
}
