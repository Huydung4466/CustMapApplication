package com.huydung.custmapapplication.auth.service;

import com.huydung.custmapapplication.auth.dto.CurrentUserResponse;

public interface AuthService {
    CurrentUserResponse getCurrentLoggedUser();
}
