package com.campushub.dto;

import lombok.Data;

@Data
public class UserPublicProfileDTO {

    private String nickname;
    private String college;
    private String contact;
    private Object ratingSummary;
}
