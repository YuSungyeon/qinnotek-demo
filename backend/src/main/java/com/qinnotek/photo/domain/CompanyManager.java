package com.qinnotek.photo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업 ↔ 담당자 지정(연결). 이 연결이 있는 담당자가 해당 기업의 제출 알림을 받는다.
 */
@Entity
@Table(
        name = "company_manager",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "manager_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private Manager manager;

    public CompanyManager(Company company, Manager manager) {
        this.company = company;
        this.manager = manager;
    }
}
