package com.orv.storyboard.service;

import com.orv.storyboard.common.StoryboardErrorCode;
import com.orv.storyboard.common.StoryboardException;
import com.orv.storyboard.repository.StoryboardRepository;
import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardPreviewInfo;
import com.orv.storyboard.domain.StoryboardStatus;
import com.orv.storyboard.domain.StoryboardUsageStatus;
import com.orv.storyboard.domain.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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
        Storyboard storyboard = storyboardRepository.findById(storyboardId)
                .orElseThrow(() -> new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_FOUND));

        if (storyboard.getStatus() != StoryboardStatus.ACTIVE) {
            throw new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_ACTIVE);
        }

        storyboardRepository.saveUsageHistory(storyboardId, memberId, status);
    }

    @Transactional
    public void participateStoryboardO(UUID storyboardId, UUID memberId) {
        long totalStart = System.nanoTime();
        storyboardRepository.saveUsageHistory(storyboardId, memberId, StoryboardUsageStatus.STARTED);

        Long readLockPhaseNs = null;
        Long updateLockStartNs = null;
        boolean success = false;

        try {
            long readLockStart = System.nanoTime();
            Storyboard storyboard = storyboardRepository.findById(storyboardId)
                    .orElseThrow(() -> new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_FOUND));

            validateStoryboardActive(storyboard);
            validateParticipationLimit(storyboard);
            readLockPhaseNs = toElapsedNanos(readLockStart);

            updateLockStartNs = System.nanoTime();
            int updateCount = storyboardRepository.incrementParticipationCountSafely(storyboardId);

            if (updateCount > 0) {
                success = true;
                return;
            }

            throwExceptionWithFailureReason(storyboardId);
        } finally {
            long endNs = System.nanoTime();
            long totalNs = endNs - totalStart;
            log.info(
                    "participateStoryboard metrics mode=B method=participateStoryboardO storyboardId={} memberId={} readLockPhaseNs={} updateLockHeldNs={} totalNs={} success={}",
                    storyboardId,
                    memberId,
                    readLockPhaseNs != null ? readLockPhaseNs : -1,
                    updateLockStartNs != null ? endNs - updateLockStartNs : -1,
                    totalNs,
                    success
            );
        }
    }

    @Transactional
    public void participateStoryboard(UUID storyboardId, UUID memberId) {
        long totalStart = System.nanoTime();
        storyboardRepository.saveUsageHistory(storyboardId, memberId, StoryboardUsageStatus.STARTED);

        Long readLockPhaseNs = null;
        Long updateLockStartNs = null;
        boolean success = false;

        try {
            long readLockStart = System.nanoTime();
            updateLockStartNs = System.nanoTime();
            Storyboard storyboard = storyboardRepository.findByIdForNoKeyUpdate(storyboardId)
                    .orElseThrow(() -> new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_FOUND));

            validateStoryboardActive(storyboard);
            validateParticipationLimit(storyboard);
            readLockPhaseNs = toElapsedNanos(readLockStart);

            storyboardRepository.incrementParticipationCount(storyboardId);

            success = true;
        } finally {
            long endNs = System.nanoTime();
            long totalNs = endNs - totalStart;
            log.info(
                    "participateStoryboard metrics mode=A method=participateStoryboardP storyboardId={} memberId={} readLockPhaseNs={} updateLockHeldNs={} totalNs={} success={}",
                    storyboardId,
                    memberId,
                    readLockPhaseNs != null ? readLockPhaseNs : -1,
                    updateLockStartNs != null ? endNs - updateLockStartNs : -1,
                    totalNs,
                    success
            );
        }
    }

    public void participateStoryboardN(UUID storyboardId, UUID memberId) {
        long totalStart = System.nanoTime();
        storyboardRepository.saveUsageHistory(storyboardId, memberId, StoryboardUsageStatus.STARTED);

        Long readLockPhaseNs = null;
        Long updateLockStartNs = null;
        boolean success = false;

        try {
            long readLockStart = System.nanoTime();
            Storyboard storyboard = storyboardRepository.findById(storyboardId)
                    .orElseThrow(() -> new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_FOUND));

            validateStoryboardActive(storyboard);
            validateParticipationLimit(storyboard);
            readLockPhaseNs = toElapsedNanos(readLockStart);

            updateLockStartNs = System.nanoTime();
            storyboardRepository.incrementParticipationCount(storyboardId);

            success = true;
        } finally {
            long endNs = System.nanoTime();
            long totalNs = endNs - totalStart;
            log.info(
                    "participateStoryboard metrics mode=C method=participateStoryboardN storyboardId={} memberId={} readLockPhaseNs={} updateLockHeldNs={} totalNs={} success={}",
                    storyboardId,
                    memberId,
                    readLockPhaseNs != null ? readLockPhaseNs : -1,
                    updateLockStartNs != null ? endNs - updateLockStartNs : -1,
                    totalNs,
                    success
            );
        }
    }

    private void validateParticipationLimit(Storyboard storyboard) {
        boolean hasLimit = storyboard.getMaxParticipationLimit() != null;
        boolean isLimitExceeded = hasLimit && storyboard.getMaxParticipationLimit() <= storyboard.getParticipationCount();

        if (isLimitExceeded) {
            throw new StoryboardException(StoryboardErrorCode.PARTICIPATION_LIMIT_EXCEEDED);
        }
    }

    private void validateStoryboardActive(Storyboard storyboard) {
        boolean isStoryboardActive = storyboard.getStatus() == StoryboardStatus.ACTIVE;

        if (!isStoryboardActive) {
            throw new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_ACTIVE);
        }
    }

    private void throwExceptionWithFailureReason(UUID storyboardId) {
        Storyboard storyboard = storyboardRepository.findById(storyboardId)
            .orElseThrow(() -> new StoryboardException(StoryboardErrorCode.STORYBOARD_NOT_FOUND));

        validateStoryboardActive(storyboard);
        validateParticipationLimit(storyboard);

        throw new RuntimeException("Unexpected Error while update storyboard participation count");
    }
    private long toElapsedNanos(long startNanoTime) {
        return System.nanoTime() - startNanoTime;
    }
}
