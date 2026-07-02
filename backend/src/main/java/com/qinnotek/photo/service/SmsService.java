package com.qinnotek.photo.service;

import com.qinnotek.photo.config.AppProperties;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 문자 발송 서비스 (Solapi 연동).
 * 고객 제출 시 관리자에게 알림 문자를 발송한다.
 * 발송 실패가 제출 흐름을 막지 않도록 예외는 모두 흡수한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final AppProperties appProperties;
    private final AdminSettingService adminSettingService;

    public void notifyAdminOnSubmission(String companyName) {
        var sms = appProperties.getSms();

        if (!sms.isEnabled()) {
            log.info("[SMS] 비활성화 상태 - '{}' 제출 알림 발송 생략", companyName);
            return;
        }
        if (isBlank(sms.getApiKey()) || isBlank(sms.getApiSecret())) {
            log.warn("[SMS] API Key/Secret 미설정 - 발송 생략");
            return;
        }
        String adminPhone = adminSettingService.getAdminPhoneNumber();
        if (isBlank(adminPhone)) {
            log.warn("[SMS] 관리자 전화번호 미설정 - 발송 생략 (관리자 설정에서 등록 필요)");
            return;
        }

        // 발신번호: SOLAPI_SENDER 설정이 있으면 사용, 없으면 관리자 번호
        String from = isBlank(sms.getSenderNumber()) ? adminPhone : sms.getSenderNumber();

        // SMS 한글 45자 이내
        String text = companyName + "에서 사진을 등록했습니다. 검토 바랍니다.";

        try {
            DefaultMessageService messageService =
                    SolapiClient.INSTANCE.createInstance(sms.getApiKey(), sms.getApiSecret());
            Message message = new Message();
            message.setFrom(from);
            message.setTo(adminPhone);
            message.setText(text);

            messageService.send(message);
            log.info("[SMS] 관리자 알림 발송 완료 -> {}", adminPhone);
        } catch (SolapiMessageNotReceivedException e) {
            log.error("[SMS] 발송 실패 목록: {} / {}", e.getFailedMessageList(), e.getMessage());
        } catch (Exception e) {
            log.error("[SMS] 발송 오류: {}", e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
