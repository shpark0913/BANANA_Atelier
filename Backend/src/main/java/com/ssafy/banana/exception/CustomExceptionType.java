package com.ssafy.banana.exception;

import org.springframework.http.HttpStatus;

public enum CustomExceptionType {
	RUNTIME_EXCEPTION(HttpStatus.BAD_REQUEST, "E001", "잘못된 요청입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "서버 오류 입니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E003", "사용자 정보가 존재하지 않습니다."),
	ACCESS_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "E004", "액세스 토큰 오류입니다."),
	REFRESH_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "E005", "리프레쉬 토큰 오류입니다."),
	EMAIL_CODE_ERROR(HttpStatus.UNAUTHORIZED, "E006", "이메일 인증코드 오류입니다."),
	EXPIRED_AUTH_INFO(HttpStatus.NOT_FOUND, "E007", "인증정보가 만료되었습니다."),
	AUTHORITY_ERROR(HttpStatus.FORBIDDEN, "E008", "해당 기능을 요청할 권한이 없습니다."),
	USER_CONFLICT(HttpStatus.CONFLICT, "E009", "이미 가입된 사용자입니다."),
	NO_CONTENT(HttpStatus.NOT_FOUND, "E010", "데이터가 존재하지 않습니다."),
	DO_NOT_DELETE(HttpStatus.BAD_REQUEST, "E011", "삭제할 수 없습니다."),
	FILE_UPLOAD_ERROR(HttpStatus.BAD_REQUEST, "E012", "파일을 업로드 할 수 없습니다."),
	FILE_DOWNLOAD_ERROR(HttpStatus.CONFLICT, "E013", "파일을 다운로드 할 수 없습니다."),
	FILE_EXTENSION_ERROR(HttpStatus.FORBIDDEN, "E014", "jpg, jpeg, png의 이미지 파일만 업로드해주세요."),
	MAX_UPLOAD_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "E015", "한번에 업로드 가능한 용량(10MB)을 초과했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private String message;

	CustomExceptionType(HttpStatus httpStatus, String code) {
		this.httpStatus = httpStatus;
		this.code = code;
	}

	CustomExceptionType(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
