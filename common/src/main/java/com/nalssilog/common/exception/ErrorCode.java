package com.nalssilog.common.exception;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

/**
 * 각 모듈이 자신의 에러 코드 enum 으로 구현한다. (lombok @Getter 로 충족 가능)
 * getCode() 는 프론트가 분기하는 안정적인 문자열 코드 (예: NICKNAME_DUPLICATED).
 * 구현체는 전부 enum(직렬화 가능) — NalssiLogException(Serializable) 필드로 안전하게 담기도록 Serializable 로 둔다.
 */
public interface ErrorCode extends Serializable {

    String getCode();

    String getMessage();

    HttpStatus getStatus();
}
