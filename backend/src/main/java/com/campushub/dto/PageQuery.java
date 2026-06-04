package com.campushub.dto;

import lombok.Data;

@Data
public class PageQuery {

    private Integer page = 1;
    private Integer pageSize = 20;
}
