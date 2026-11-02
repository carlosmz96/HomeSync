package com.homesync.household.exception;

public class InvalidInvitationException extends RuntimeException {

    public InvalidInvitationException() {
        super("La invitación no es válida, ha caducado o ya se ha utilizado.");
    }

}