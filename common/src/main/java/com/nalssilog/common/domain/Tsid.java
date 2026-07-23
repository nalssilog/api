package com.nalssilog.common.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * PK 를 TSID(시간순 정렬 가능한 64bit id, Long) 로 생성한다. 사용: {@code @Id @Tsid private Long id;}
 */
@IdGeneratorType(TsidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Tsid {
}
