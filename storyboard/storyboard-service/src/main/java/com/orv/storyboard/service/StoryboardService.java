package com.orv.storyboard.service;

import com.orv.storyboard.repository.StoryboardRepository;
import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardPreviewInfo;
import com.orv.storyboard.domain.StoryboardUsageStatus;
import com.orv.storyboard.domain.Topic;
import com.orv.storyboard.common.StoryboardErrorCode;
import com.orv.storyboard.common.StoryboardException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoryboardService {
    private final StoryboardRepository storyboardRepository;

    public Optional<Storyboard> getStoryboard(UUID storyboardId) {
        return storyboardRepository.findById(storyboardId);
    }

    public Optional<List<Scene>> getAllScenes(UUID storyboardId) {
        return storyboardRepository.findScenesByStoryboardId(storyboardId);
    }

    public Optional<Scene> getScene(UUID sceneId) {
        return storyboardRepository.findSceneById(sceneId);
    }

    public Optional<StoryboardPreviewInfo> getStoryboardPreview(UUID storyboardId) {
        Optional<List<Scene>> scenesOrEmpty = storyboardRepository.findScenesByStoryboardId(storyboardId);

        if (scenesOrEmpty.isEmpty()) {
            return Optional.empty();
        }

        List<Scene> scenes = scenesOrEmpty.get();

        // Count questions
        long questionCount = scenes.stream()
                .filter(scene -> scene.getSceneType().equals("QUESTION"))
                .count();

        Optional<String[]> examplesOrEmpty = storyboardRepository.getStoryboardPreview(storyboardId);

        if (examplesOrEmpty.isEmpty()) {
            return Optional.empty();
        }

        String[] examples = examplesOrEmpty.get();
        List<String> exampleList = Arrays.stream(examples).toList();

        return Optional.of(new StoryboardPreviewInfo(
                storyboardId,
                (int) questionCount,
                exampleList
        ));
    }

    public Optional<List<Topic>> getTopicsOfStoryboard(UUID storyboardId) {
        return storyboardRepository.findTopicsOfStoryboard(storyboardId);
    }

    public void saveUsageHistory(UUID storyboardId, UUID memberId, StoryboardUsageStatus status) {
        storyboardRepository.saveUsageHistory(storyboardId, memberId, status);
    }

    @Transactional
    public void participateStoryboard(UUID storyboardId, UUID memberId) {
        storyboardRepository.saveUsageHistory(storyboardId, memberId, StoryboardUsageStatus.STARTED);

        Storyboard storyboard = storyboardRepository.findByIdForShare(storyboardId)
                .orElseThrow(() -> new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_FOUND));

        validateParticipationLimit(storyboard);
        storyboardRepository.incrementParticipationCount(storyboardId);
    }

    private void validateParticipationLimit(Storyboard storyboard) {
        boolean hasLimit = storyboard.getMaxParticipationLimit() != null;
        boolean isLimitExceeded = hasLimit && storyboard.getMaxParticipationLimit() <= storyboard.getParticipationCount();

        if (isLimitExceeded) {
            throw new StoryboardException(StoryboardErrorCode.PARTICIPATION_LIMIT_EXCEEDED);
        }
    }
}
