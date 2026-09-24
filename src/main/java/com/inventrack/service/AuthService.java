package com.inventrack.service;

import com.inventrack.dto.request.LoginRequest;
import com.inventrack.dto.request.RegisterRequest;
import com.inventrack.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
