package com.orv.storyboard.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Topic {
    private UUID id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private List<Hashtag> hashtags;
}
