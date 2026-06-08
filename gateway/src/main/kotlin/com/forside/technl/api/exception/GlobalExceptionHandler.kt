package com.forside.technl.api.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.forside.technl.generated.api.BadRequestException
import com.forside.technl.generated.api.InternalServerErrorException
import com.forside.technl.generated.api.NotFoundException
import com.forside.technl.generated.model.ErrorResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.server.ResponseStatusException
import io.grpc.Status
import io.grpc.StatusRuntimeException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    message = ex.message ?: "Resource not found",
                    code = HttpStatus.NOT_FOUND.toString()
                )
            )
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(ex: BadRequestException, response: HttpServletResponse): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = ex.message ?: "Bad request",
                code = HttpStatus.BAD_REQUEST.toString()))
    }

    @ExceptionHandler(InternalServerErrorException::class)
    fun handleInternalServerErrorException(ex: InternalServerErrorException, response: HttpServletResponse): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                message = ex.message ?: "Internal server error",
                code = HttpStatus.INTERNAL_SERVER_ERROR.toString()))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(ex.statusCode)
            .body(ErrorResponse(
                message = ex.reason.toString(),
                code = ex.statusCode.toString()))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                message = "Unexpected server error",
                code = HttpStatus.INTERNAL_SERVER_ERROR.toString()))
    }

    @ExceptionHandler(RestClientException::class)
    fun handleRestClientException(ex: RestClientException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ErrorResponse(
                    message = "Invoice service is unavailable",
                    code = HttpStatus.SERVICE_UNAVAILABLE.toString()))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {

        val message = when (val cause = ex.cause) {
            is InvalidFormatException -> {
                val fieldName = cause.path.lastOrNull()?.fieldName ?: "unknown"
                "Invalid value for field: $fieldName"
            }
            else -> {
                val rawMessage = ex.mostSpecificCause.message ?: "Invalid JSON request body"
                when {
                    "Array contains no element matching the predicate" in rawMessage ->
                        "Invalid Status value in request body"
                    "Cannot construct instance" in rawMessage -> "Invalid request body format"
                    else -> "Invalid JSON request body"
                }
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = message,
                code = HttpStatus.BAD_REQUEST.toString()))
    }

    @ExceptionHandler(StatusRuntimeException::class)
    fun handleGrpcException(ex: StatusRuntimeException): ResponseEntity<ErrorResponse> {
        val httpStatus = when (ex.status.code) {
            Status.Code.UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
            Status.Code.NOT_FOUND -> HttpStatus.NOT_FOUND
            Status.Code.INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST
            Status.Code.DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val message = when (ex.status.code) {
            Status.Code.UNAVAILABLE -> "beverage service is unavailable"
            Status.Code.NOT_FOUND -> "Requested resource was not found"
            Status.Code.INVALID_ARGUMENT -> "Invalid request"
            Status.Code.DEADLINE_EXCEEDED -> "service timeout"
            else -> "gRPC service error"
        }

        return ResponseEntity.status(httpStatus)
            .body(
                ErrorResponse(
                    message = message,
                    code = httpStatus.toString()
                )
            )
    }
}