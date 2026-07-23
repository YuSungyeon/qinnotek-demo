package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 문자 담당자(마스터). 이름·직책·전화번호를 보관하고,
 * 기업별로 이 담당자들을 지정해 제출 알림을 받게 한다.
 */
@Entity
@Table(name = "manager")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 직책 (예: 점장, 사장) */
    private String position;

    /** 전화번호 (숫자만 저장) */
    @Column(nullable = false, length = 20)
    private String phoneNumber;

    public Manager(String name, String position, String phoneNumber) {
        this.name = name.trim();
        this.position = normalize(position);
        this.phoneNumber = digits(phoneNumber);
    }

    public void update(String name, String position, String phoneNumber) {
        this.name = name.trim();
        this.position = normalize(position);
        this.phoneNumber = digits(phoneNumber);
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
