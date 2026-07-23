package com.qinnotek.photo.service;

import com.qinnotek.photo.config.AppProperties;
import com.qinnotek.photo.repository.CompanyManagerRepository;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 문자 발송 서비스 (Solapi 연동).
 * 고객 제출 시 해당 기업에 지정된 담당자들에게 알림 문자를 발송한다.
 * 발송 실패가 제출 흐름을 막지 않도록 예외는 모두 흡수한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final AppProperties appProperties;
    private final AdminSettingService adminSettingService;
    private final CompanyManagerRepository companyManagerRepository;

    public void notifyManagersOnSubmission(Long companyId, String companyName) {
        var sms = appProperties.getSms();

        if (!adminSettingService.isSmsEnabled()) {
            log.info("[SMS] 발송 토글 off - '{}' 제출 알림 발송 생략", companyName);
            return;
        }
        if (isBlank(sms.getApiKey()) || isBlank(sms.getApiSecret())) {
            log.warn("[SMS] API Key/Secret 미설정 - 발송 생략");
            return;
        }

        List<String> recipients = companyManagerRepository.findByCompanyIdWithManager(companyId).stream()
                .map(cm -> cm.getManager().getPhoneNumber())
                .filter(p -> !isBlank(p))
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            log.warn("[SMS] '{}'에 지정된 담당자가 없어 발송 생략", companyName);
            return;
        }

        // 발신번호: SOLAPI_SENDER 설정이 있으면 사용, 없으면 첫 담당자 번호
        String from = isBlank(sms.getSenderNumber()) ? recipients.get(0) : sms.getSenderNumber();
        String text = companyName + "에서 사진을 등록했습니다. 검토 바랍니다."; // SMS 한글 45자 이내

        DefaultMessageService messageService =
                SolapiClient.INSTANCE.createInstance(sms.getApiKey(), sms.getApiSecret());

        int ok = 0;
        for (String to : recipients) {
            try {
                Message message = new Message();
                message.setFrom(from);
                message.setTo(to);
                message.setText(text);
                messageService.send(message);
                ok++;
            } catch (SolapiMessageNotReceivedException e) {
                log.error("[SMS] 발송 실패({}): {} / {}", to, e.getFailedMessageList(), e.getMessage());
            } catch (Exception e) {
                log.error("[SMS] 발송 오류({}): {}", to, e.getMessage());
            }
        }
        log.info("[SMS] '{}' 담당자 알림 발송 {}/{}건", companyName, ok, recipients.size());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
