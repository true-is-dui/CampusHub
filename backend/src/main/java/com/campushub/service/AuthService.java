package com.campushub.service;

import com.campushub.dto.LoginRequest;
import com.campushub.dto.LoginSession;
import com.campushub.dto.RegisterRequest;

public interface AuthService {
    LoginSession register(RegisterRequest request);
    LoginSession login(LoginRequest request);
}
