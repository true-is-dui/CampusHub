package com.campushub.config;

import com.campushub.entity.enums.AuthStatus;
import com.campushub.entity.enums.UserRole;
import lombok.Data;

@Data
public class CurrentUserContext {
    private Long currentUserId;
    private UserRole role;
    private AuthStatus authStatus;
}
