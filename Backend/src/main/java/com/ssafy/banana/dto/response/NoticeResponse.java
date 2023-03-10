package com.ssafy.banana.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.banana.db.entity.Notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Accessors(chain = true)
public class NoticeResponse {

	@JsonProperty(value = "noticeSeq")
	private Long id;

	private String noticeTitle;

	private String noticeContent;

	private LocalDateTime noticeTime;

	private Long userSeq;

	private String nickname;

	public NoticeResponse(Notice notice) {
		this.id = notice.getId();
		this.noticeTitle = notice.getNoticeTitle();
		this.noticeContent = notice.getNoticeContent();
		this.noticeTime = notice.getNoticeTime();
		this.userSeq = notice.getArtist().getUser().getId();
		this.nickname = notice.getArtist().getUser().getNickname();
	}
}
