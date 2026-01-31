package com.orv.auth.domain;

import lombok.Data;

import java.util.UUID;

@Data
public class Role {
    private UUID id;
    private String name;
}
