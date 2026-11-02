package com.homesync.identity.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("El correo electrónico o la contraseña no son correctos.");
    }

}