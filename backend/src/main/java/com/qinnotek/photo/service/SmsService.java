package com.qinnotek.photo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 문자 발송 서비스(스텁). 실제 문자 API는 별도 연동.
 * 데모에서는 로그로 대체한다.
 */
@Slf4j
@Service
public class SmsService {

    /**
     * 관리자에게 사진 제출 알림 발송.
     */
    public void notifyAdminOnSubmission(String companyName) {
        // TODO: 실제 문자 API 연동 (예: NHN Cloud, CoolSMS 등)
        log.info("[SMS] 관리자 알림 - '{}' 기업이 사진을 제출했습니다.", companyName);
    }
}
