package com.orv.api.domain.auth.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class Role {
    private UUID id;
    private String name;
}
