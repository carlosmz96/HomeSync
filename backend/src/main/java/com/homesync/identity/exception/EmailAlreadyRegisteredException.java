package com.homesync.identity.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Ya existe una cuenta con ese correo electrónico.");
    }
}