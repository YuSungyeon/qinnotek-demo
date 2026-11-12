package com.qinnotek.submission.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기업. 고객은 관리자가 발급한 제출 링크(accessToken)로만 접속한다.
 * 전화번호는 링크를 문자로 보낼 때의 기본 수신 번호로만 쓴다.
 */
@Entity
@Table(name = "company")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기업명 (중복 불가) */
    @Column(nullable = false, unique = true)
    private String name;

    /** 기업당 1개, 미등록 시 null */
    @Column(unique = true)
    private String phoneNumber;

    /** 제출 링크 토큰. 기업당 1개만 유효하며, 재발급하면 이전 토큰은 사라진다. 미발급/끔 상태면 null */
    @Column(unique = true, length = 40)
    private String accessToken;

    private LocalDateTime linkIssuedAt;

    /** null 이면 기한 없음 */
    private LocalDateTime linkExpiresAt;

    /** 고객이 링크를 마지막으로 연 시각 */
    private LocalDateTime linkOpenedAt;

    public Company(String name) {
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
    }

    public void issueLink(String token, LocalDateTime expiresAt) {
        this.accessToken = token;
        this.linkIssuedAt = LocalDateTime.now();
        this.linkExpiresAt = expiresAt;
        this.linkOpenedAt = null;
    }

    public void revokeLink() {
        this.accessToken = null;
        this.linkIssuedAt = null;
        this.linkExpiresAt = null;
        this.linkOpenedAt = null;
    }

    public boolean isLinkExpired(LocalDateTime now) {
        return linkExpiresAt != null && now.isAfter(linkExpiresAt);
    }

    public void markLinkOpened(LocalDateTime now) {
        this.linkOpenedAt = now;
    }
}
