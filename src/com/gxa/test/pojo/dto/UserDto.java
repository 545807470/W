package com.gxa.test.pojo.dto;

import com.gxa.wyq.anno.Component;

@Component
public class UserDto {
    private Integer id;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
