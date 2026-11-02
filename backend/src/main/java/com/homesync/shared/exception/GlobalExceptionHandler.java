package com.homesync.shared.exception;

import com.homesync.household.exception.InvalidInvitationException;
import com.homesync.identity.exception.AccountNotActiveException;
import com.homesync.identity.exception.EmailAlreadyRegisteredException;
import com.homesync.identity.exception.InvalidCredentialsException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage());

        problem.setTitle("Correo electrónico ya registrado");
        problem.setType(URI.create("https://homesync.local/problems/email-already-registered"));

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Uno o varios campos no son válidos.");

        problem.setTitle("Error de validación");

        String errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .reduce((first, second) -> first + "; " + second)
                .orElse("Revisa los datos enviados.");

        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ProblemDetail handleAccountNotActive(
            AccountNotActiveException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                exception.getMessage());

        problem.setTitle("Cuenta no activa");

        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(
            InvalidCredentialsException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage());

        problem.setTitle("Credenciales incorrectas");
        problem.setType(URI.create(
                "https://homesync.local/problems/invalid-credentials"));

        return problem;
    }

    @ExceptionHandler(InvalidInvitationException.class)
    public ProblemDetail handleInvalidInvitation(
            InvalidInvitationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La invitación no es válida, ha caducado o ya se ha utilizado.");

        problem.setTitle("Invitación no válida");
        problem.setType(URI.create(
                "https://homesync.local/problems/invalid-invitation"));

        return problem;
    }

}