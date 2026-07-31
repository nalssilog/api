package com.nalssilog.report.api.dto;

import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.report.application.dto.AuthorInfo;
import com.nalssilog.report.application.dto.LocationSummary;
import com.nalssilog.report.application.dto.ReportData;
import com.nalssilog.report.domain.ActorType;
import com.nalssilog.report.domain.AnonymousNicknameGenerator;
import com.nalssilog.report.domain.Precipitation;
import com.nalssilog.report.domain.Sunlight;
import com.nalssilog.report.domain.Temperature;
import java.time.Instant;
import java.util.List;

/**
 * 제보 응답. location 은 위경도 제외(시도/시군구/동 + label).
 * 작성자는 활성 회원이면 type=MEMBER + id(문자열), 익명/탈퇴면 type=ANONYMOUS + id=null + 익명#10자리.
 * imageUrls 는 storage_key 에 공개 base(R2/CDN)를 붙인 완전한 URL (서비스가 조립해 넘긴다).
 * isMine 은 현재 회원 ID와 같은 브라우저의 anonymous_id 를 모두 확인한 소유 여부다.
 */
public record ReportResponse(
        String id,
        Location location,
        Author author,
        Temperature temperature,
        Precipitation precipitation,
        Sunlight sunlight,
        String comment,
        List<String> imageUrls,
        long thanksCount,
        boolean isThanked,
        boolean isMine,
        Instant createdAt
) {

    public record Location(
            String id,
            String sido,
            String sigungu,
            String dong,
            String label,
            String shortLabel
    ) {
    }

    public record Author(ActorType type, String id, String nickname, Avatar avatar) {
    }

    public record Avatar(AvatarType type, String value) {
    }

    public static ReportResponse of(ReportData data, LocationSummary location, AuthorInfo author,
                                    long thanksCount, boolean isThanked, boolean isMine, List<String> imageUrls) {
        Author authorDto = author != null
                ? new Author(ActorType.MEMBER, String.valueOf(author.id()), author.nickname(),
                new Avatar(author.avatarType(), author.avatarValue()))
                : new Author(
                        ActorType.ANONYMOUS,
                        null,
                        AnonymousNicknameGenerator.generate(anonymousAuthorKey(data)),
                        null);
        Location locationDto = new Location(
                String.valueOf(location.id()),
                location.sido(),
                location.sigungu(),
                location.dong(),
                location.label(),
                location.shortLabel());

        return new ReportResponse(
                String.valueOf(data.id()),
                locationDto,
                authorDto,
                data.temperature(),
                data.precipitation(),
                data.sunlight(),
                data.comment(),
                imageUrls,
                thanksCount,
                isThanked,
                isMine,
                data.createdAt()
        );
    }

    private static String anonymousAuthorKey(ReportData data) {
        if (data.authorAnonymousKey() != null) {
            return data.authorAnonymousKey();
        }

        if (data.authorMemberId() != null) {
            return "member:" + data.authorMemberId();
        }

        return "report:" + data.id();
    }
}
