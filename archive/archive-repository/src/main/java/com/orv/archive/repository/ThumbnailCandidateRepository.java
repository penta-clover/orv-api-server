package com.orv.archive.repository;

import com.orv.archive.domain.ThumbnailCandidate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThumbnailCandidateRepository {
    Long save(ThumbnailCandidate candidate);

    Optional<ThumbnailCandidate> findById(Long id);

    List<ThumbnailCandidate> findByJobId(Long jobId);

    List<ThumbnailCandidate> findByVideoId(UUID videoId);

    void deleteByJobId(Long jobId);
}
