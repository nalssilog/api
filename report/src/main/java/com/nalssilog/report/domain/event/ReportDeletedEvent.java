package com.nalssilog.report.domain.event;

import java.util.List;

/** 제보 DB 삭제 커밋 후 오브젝트 스토리지 이미지를 정리하기 위한 이벤트. */
public record ReportDeletedEvent(List<String> imageKeys) {

    public ReportDeletedEvent {
        imageKeys = List.copyOf(imageKeys);
    }
}
