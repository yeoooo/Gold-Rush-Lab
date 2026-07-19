package io.devyeoooo.Gold_Rush_Lab.comm.exception;

import io.devyeoooo.Gold_Rush_Lab.presentation.dto.comm.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("USER_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(ActiveMineNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleActiveMineNotFound(
            ActiveMineNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("ACTIVE_MINE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(MineNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMineNotFound(MineNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("MINE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestParameter(
            InvalidRequestParameterException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("INVALID_REQUEST_PARAMETER", exception.getMessage()));
    }

    @ExceptionHandler(MineDepletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMineDepleted(MineDepletedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("MINE_DEPLETED", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        "INVALID_REQUEST_PARAMETER",
                        "요청 파라미터 형식이 올바르지 않습니다."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("처리되지 않은 예외가 발생했습니다.", exception);

        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다."
                ));
    }
}
