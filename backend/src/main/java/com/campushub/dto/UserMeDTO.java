package com.campushub.dto;

import lombok.Data;

@Data
public class UserMeDTO {

    private Long userId;
    private String username;
    private String studentIdMasked;
    private String realName;
    private String nickname;
    private String college;
    private String contact;
    private String authStatus;
    private String role;
}
