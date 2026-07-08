package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업. 전화번호는 기업당 1개만 등록 가능(unique).
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

    public Company(String name) {
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
    }
}
