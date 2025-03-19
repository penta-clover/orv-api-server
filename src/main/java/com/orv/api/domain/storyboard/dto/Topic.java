package com.orv.api.domain.storyboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
public class Topic {
    private UUID id;
    private String name;
    private String description;
}
