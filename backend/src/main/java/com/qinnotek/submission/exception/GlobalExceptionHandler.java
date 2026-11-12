package com.qinnotek.submission.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * API 오류 응답과 로그 수준을 한곳에서 정한다.
 *
 * <p>모니터링 기준:
 * <ul>
 *   <li>5xx 는 서버 쪽 문제일 때만 낸다 - ERROR 로그 + 스택트레이스</li>
 *   <li>잘못된 요청은 알맞은 4xx 로 돌려준다 (없는 주소 404, 잘못된 메서드 405, 형식 오류 400, 용량 초과 413 …)
 *       - 404/405 는 봇 스캔이 많아 DEBUG, 나머지 4xx 는 WARN 한 줄</li>
 *   <li>사용자가 화면을 닫아 연결이 끊긴 경우(다운로드 중단 등)는 오류가 아니므로 DEBUG 로만 남기고 응답하지 않는다</li>
 *   <li>500 응답에는 내부 예외 메시지를 싣지 않는다</li>
 * </ul>
 * 응답 본문 형식 {status, message, path, timestamp} 은 프론트가 message 를 그대로 보여준다.
 *
 * <p>Spring MVC 표준 예외의 상태 코드는 {@link ResponseEntityExceptionHandler} 가 정하고,
 * 여기서는 본문·메시지·로그만 맞춘다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public record ErrorResponse(int status, String message, String path, LocalDateTime timestamp) {
        static ErrorResponse of(HttpStatusCode status, String message, String path) {
            return new ErrorResponse(status.value(), message, path, LocalDateTime.now());
        }
    }

    static final String SERVER_ERROR_MESSAGE = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

    /** 비즈니스 규칙 위반 - 서비스가 정한 상태와 메시지를 그대로 쓴다 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        return respond(e.getStatus(), e.getMessage(), request.getRequestURI(), e);
    }

    /** 유니크 제약 등 - 동시에 같은 값을 등록하는 경합에서 생긴다 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e,
                                                             HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "이미 등록된 값이 있어 처리하지 못했습니다. 새로고침 후 확인해주세요.",
                request.getRequestURI(), e);
    }

    /** 그 밖의 모든 예외 - 진짜 서버 오류만 여기로 온다 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        if (isClientAbort(e)) {
            log.debug("클라이언트 연결 끊김 {} {}: {}", request.getMethod(), request.getRequestURI(), e.toString());
            return null; // 응답을 쓸 연결이 없다
        }
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR_MESSAGE, request.getRequestURI(), e);
    }

    /** 405 - 상위 클래스는 여기서 PageNotFound 로거에 WARN 을 남기므로, 404 와 같이 DEBUG 로만 남기도록 가로챈다 */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatusCode status,
                                                                         WebRequest request) {
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    /** Spring MVC 표준 예외(404/405/400/413/415 …) - 상태는 상위 클래스가 정한 값을 따른다 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String path = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
        if (statusCode.is5xxServerError() && isClientAbort(ex)) {
            log.debug("클라이언트 연결 끊김 {}: {}", path, ex.toString());
            return null;
        }
        ResponseEntity<ErrorResponse> res = respond(statusCode, messageFor(ex, statusCode), path, ex);
        return ResponseEntity.status(res.getStatusCode()).headers(headers).body(res.getBody());
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatusCode status, String message, String path, Exception e) {
        int code = status.value();
        if (status.is5xxServerError()) {
            log.error("[{}] {} - {}", code, path, e.toString(), e);
        } else if (code == 404 || code == 405) {
            log.debug("[{}] {} - {}", code, path, message);
        } else {
            log.warn("[{}] {} - {}", code, path, message);
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, path));
    }

    private String messageFor(Exception ex, HttpStatusCode status) {
        if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) {
            return "요청한 주소를 찾을 수 없습니다.";
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return "지원하지 않는 요청 방식입니다.";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "지원하지 않는 요청 형식입니다.";
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return "파일 용량이 너무 큽니다. 더 작은 파일로 올려주세요.";
        }
        if (ex instanceof MethodArgumentNotValidException e) {
            String fields = e.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            return fields.isBlank() ? "요청 값이 올바르지 않습니다." : fields;
        }
        if (ex instanceof MissingServletRequestParameterException e) {
            return "필수 값이 빠졌습니다: " + e.getParameterName();
        }
        if (ex instanceof MissingServletRequestPartException e) {
            return "필수 값이 빠졌습니다: " + e.getRequestPartName();
        }
        if (ex instanceof MissingPathVariableException) {
            return "요청한 주소를 찾을 수 없습니다.";
        }
        if (ex instanceof TypeMismatchException e) {
            return "요청 값 형식이 올바르지 않습니다" + (e.getPropertyName() != null ? ": " + e.getPropertyName() : ".");
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "요청 본문을 읽을 수 없습니다.";
        }
        if (status.is5xxServerError()) {
            return SERVER_ERROR_MESSAGE;
        }
        return status.is4xxClientError() ? "요청을 처리할 수 없습니다." : SERVER_ERROR_MESSAGE;
    }

    /**
     * 사용자가 페이지를 닫거나 이동해 응답을 쓰던 연결이 끊긴 경우.
     * Tomcat 의 ClientAbortException, Spring 의 AsyncRequestNotUsableException,
     * 또는 원인 사슬의 IOException("Broken pipe", "Connection reset") 으로 드러난다.
     */
    static boolean isClientAbort(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String name = t.getClass().getSimpleName();
            if (name.equals("ClientAbortException") || name.equals("AsyncRequestNotUsableException")) {
                return true;
            }
            if (t instanceof IOException && t.getMessage() != null) {
                String msg = t.getMessage().toLowerCase(Locale.ROOT);
                if (msg.contains("broken pipe") || msg.contains("connection reset")) {
                    return true;
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
