package com.huydung.custmapapplication.music;

import com.huydung.custmapapplication.auth.security.CustomUserDetails;
import com.huydung.custmapapplication.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MusicAccess {
    private final UserRepository users;

    public MusicAccess(UserRepository users) {
        this.users = users;
    }

    public List<String> currentRoles(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            return List.of();
        }
        return users.findActiveRoleCodes(user.getId());
    }

    public boolean canListen(Authentication authentication) {
        return currentRoles(authentication).contains("ROLE_MUSIC");
    }

    public boolean canUpload(Authentication authentication) {
        List<String> roles = currentRoles(authentication);
        return roles.contains("ROLE_MUSIC") && roles.contains("ROLE_ADMIN");
    }
}
