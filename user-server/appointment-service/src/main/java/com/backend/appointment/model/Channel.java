package com.backend.appointment.model;

public enum Channel {
    // legacy
    VIDEO,
    IN_PERSON,

    // new explicit modes
    VIDEO_CALL,
    VOICE_CALL,
    CHAT
}
