package com.orv.api.domain.recap.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SceneInfo {
    @JsonProperty("scene_id")
    private String sceneId;

    private String question;
}
