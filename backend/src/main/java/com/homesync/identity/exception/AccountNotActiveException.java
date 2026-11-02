package com.homesync.identity.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException() {
        super("La cuenta no está activa.");
    }

}