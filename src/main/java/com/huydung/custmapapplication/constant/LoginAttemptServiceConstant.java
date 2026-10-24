package com.huydung.custmapapplication.constant;

public class LoginAttemptServiceConstant {

    private LoginAttemptServiceConstant() {

    }

    public static final int MAX_FAILS = 5;
    public static final int WINDOW_MINUTES = 15;
    public static final int LOCK_MINUTES = 15;
    public static final long WINDOW_MS = WINDOW_MINUTES * 60_000L;
    public static final long LOCK_MS = LOCK_MINUTES * 60_000L;
    public static final int PURGE_THRESHOLD = 10_000;
}
