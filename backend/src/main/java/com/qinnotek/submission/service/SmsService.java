package com.qinnotek.submission.service;

import com.qinnotek.submission.config.AppProperties;
import com.qinnotek.submission.exception.BusinessException;
import com.qinnotek.submission.repository.CompanyManagerRepository;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    /** @param count 이번에 낸 항목 수 (일부만 먼저 낼 수 있어 매번 다르다) */
    public void notifyManagersOnSubmission(Long companyId, String companyName, int count) {
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
        String text = companyName + "에서 자료 " + count + "건을 제출했습니다. 검토 바랍니다."; // SMS 한글 45자 이내

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

    /** 고객에게 제출 링크를 보낸다. 관리자가 직접 누른 발송이라 실패하면 예외로 알린다. */
    public void sendLink(String to, String companyName, String url) {
        sendToCustomer(to, "[" + companyName + "] 자료 제출 링크입니다.\n" + url, "'" + companyName + "' 제출 링크");
    }

    /** 반환 안내 한 줄 - 항목 이름과 반환 사유 */
    public record ReturnLine(String name, String reason) {
    }

    /** 고객에게 반환된 항목을 모아 한 통으로 안내한다. 실패하면 예외로 알린다. */
    public void sendReturnNotice(String to, String companyName, List<ReturnLine> lines, String url) {
        String body = lines.stream()
                .map(l -> "· " + l.name() + "\n" + l.reason())
                .collect(Collectors.joining("\n\n"));
        String text = "[" + companyName + "] 제출하신 자료 중 " + lines.size() + "건을 다시 보내 주세요.\n\n"
                + body + "\n\n아래 링크에서 반환된 항목만 다시 올리면 됩니다.\n" + url;
        sendToCustomer(to, text, "'" + companyName + "' 반환 안내 " + lines.size() + "건");
    }

    /**
     * 고객에게 보내는 문자 공통. 관리자 설정의 발송 토글은 '담당자 제출 알림'용이라 여기에는 적용하지 않는다.
     * 링크가 들어가 단문(45자)을 넘으므로 Solapi 가 장문(LMS)으로 보낸다.
     */
    private void sendToCustomer(String to, String text, String label) {
        var sms = appProperties.getSms();
        if (isBlank(sms.getApiKey()) || isBlank(sms.getApiSecret())) {
            throw BusinessException.badRequest("문자 발송이 설정되지 않았습니다.");
        }
        if (isBlank(sms.getSenderNumber())) {
            throw BusinessException.badRequest("발신번호가 설정되지 않았습니다.");
        }
        Message message = new Message();
        message.setFrom(sms.getSenderNumber());
        message.setTo(to);
        message.setText(text);
        try {
            SolapiClient.INSTANCE.createInstance(sms.getApiKey(), sms.getApiSecret()).send(message);
            log.info("[SMS] {} 발송 → {}", label, mask(to));
        } catch (SolapiMessageNotReceivedException e) {
            log.error("[SMS] {} 발송 실패({}): {}", label, mask(to), e.getFailedMessageList());
            throw BusinessException.badRequest("문자를 보내지 못했습니다. 번호를 확인해주세요.");
        } catch (Exception e) {
            log.error("[SMS] {} 발송 오류({}): {}", label, mask(to), e.getMessage());
            throw BusinessException.badRequest("문자를 보내지 못했습니다: " + e.getMessage());
        }
    }

    private String mask(String phone) {
        return phone.length() < 8 ? phone : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
