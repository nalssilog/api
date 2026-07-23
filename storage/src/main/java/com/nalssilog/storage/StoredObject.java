package com.nalssilog.storage;

/** 저장된 오브젝트의 HEAD 메타데이터 (업로드 후 존재·크기·타입 검증용). */
public record StoredObject(String contentType, long contentLength) {
}
