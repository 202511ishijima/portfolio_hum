package com.ishijima.portfoliobackend.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = {
	InquiryApiController.class,
	MemberApiController.class,
	HamsterApiController.class,
	ProductApiController.class
})
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException exception) {
		Map<String, String> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(
				FieldError::getField,
				FieldError::getDefaultMessage,
				(first, second) -> first,
				LinkedHashMap::new
			));

		String message = errors.values()
			.stream()
			.findFirst()
			.orElse("入力内容を確認してください。");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
			"message", message,
			"errors", errors
		));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
			"message", exception.getMessage()
		));
	}
}
