package com.orv.api.domain.recap.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScenario {
    private String title;
    private List<SceneInfo> scenes;
}
