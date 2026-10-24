package com.huydung.custmapapplication.auth.security;

import com.huydung.custmapapplication.constant.LoginAttemptServiceConstant;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống dò mật khẩu: sai {@value #MAX_FAILS} lần trong {@value #WINDOW_MINUTES} phút thì khóa đăng nhập {@value #LOCK_MINUTES} phút.
 * Khóa theo cặp (tên đăng nhập + địa chỉ IP). Lưu trong bộ nhớ nên khởi động lại ứng dụng sẽ xóa trạng thái khóa.
 */
@Component
public class LoginAttemptService {

    private static final class Entry {
        int fails;
        long firstFailAt;
        long lockedUntil;
    }

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username, String ip) {
        Entry e = attempts.get(key(username, ip));
        if (e == null) return false;
        synchronized (e) {
            return e.lockedUntil > System.currentTimeMillis();
        }
    }

    public void recordFailure(String username, String ip) {
        long now = System.currentTimeMillis();
        if (attempts.size() > LoginAttemptServiceConstant.PURGE_THRESHOLD) purge(now);
        Entry e = attempts.computeIfAbsent(key(username, ip), k -> new Entry());
        synchronized (e) {
            if (e.lockedUntil > now) return;
            if (e.firstFailAt == 0 || now - e.firstFailAt > LoginAttemptServiceConstant.WINDOW_MS) {
                e.fails = 0;
                e.firstFailAt = now;
            }
            e.fails++;
            if (e.fails >= LoginAttemptServiceConstant.MAX_FAILS) {
                e.lockedUntil = now + LoginAttemptServiceConstant.LOCK_MS;
                e.fails = 0;
                e.firstFailAt = 0;
            }
        }
    }

    public void reset(String username, String ip) {
        attempts.remove(key(username, ip));
    }

    // Dọn các bản ghi đã hết hạn để bộ nhớ không phình ra khi bị dò nhiều tài khoản
    private void purge(long now) {
        attempts.entrySet().removeIf(en -> {
            Entry e = en.getValue();
            synchronized (e) {
                boolean lockExpired = e.lockedUntil != 0 && e.lockedUntil <= now;
                boolean windowExpired = e.lockedUntil == 0 && now - e.firstFailAt > LoginAttemptServiceConstant.WINDOW_MS;
                return lockExpired || windowExpired;
            }
        });
    }

    private static String key(String username, String ip) {
        String u = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return u + "|" + (ip == null ? "" : ip);
    }
}
