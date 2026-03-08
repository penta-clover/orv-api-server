package com.orv.storyboard.service;

import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardStatus;
import com.orv.storyboard.repository.StoryboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryboardServiceTest {

    @Mock
    private StoryboardRepository storyboardRepository;

    private StoryboardService storyboardService;

    @BeforeEach
    void setUp() {
        storyboardService = new StoryboardService(storyboardRepository);
    }

    @Test
    void participateStoryboard_usesPlainFindByIdAndSafeIncrement() {
        UUID storyboardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Storyboard storyboard = activeStoryboard(storyboardId);

        when(storyboardRepository.findById(storyboardId)).thenReturn(Optional.of(storyboard));
        when(storyboardRepository.incrementParticipationCountSafely(storyboardId)).thenReturn(1);

        storyboardService.participateStoryboard(storyboardId, memberId);

        verify(storyboardRepository).findById(storyboardId);
        verify(storyboardRepository).incrementParticipationCountSafely(storyboardId);
        verify(storyboardRepository, never()).findByIdForNoKeyUpdate(storyboardId);
        verify(storyboardRepository, never()).incrementParticipationCount(storyboardId);
    }

    @Test
    void participateStoryboardP_usesNoKeyUpdateAndPlainIncrement() {
        UUID storyboardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Storyboard storyboard = activeStoryboard(storyboardId);

        when(storyboardRepository.findByIdForNoKeyUpdate(storyboardId)).thenReturn(Optional.of(storyboard));
        when(storyboardRepository.incrementParticipationCount(storyboardId)).thenReturn(1);

        storyboardService.participateStoryboardP(storyboardId, memberId);

        verify(storyboardRepository).findByIdForNoKeyUpdate(storyboardId);
        verify(storyboardRepository).incrementParticipationCount(storyboardId);
        verify(storyboardRepository, never()).findById(storyboardId);
        verify(storyboardRepository, never()).incrementParticipationCountSafely(storyboardId);
    }

    private Storyboard activeStoryboard(UUID storyboardId) {
        Storyboard storyboard = new Storyboard();
        storyboard.setId(storyboardId);
        storyboard.setStatus(StoryboardStatus.ACTIVE);
        storyboard.setParticipationCount(0);
        storyboard.setMaxParticipationLimit(100);
        return storyboard;
    }
}
