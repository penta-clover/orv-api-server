package com.orv.admin.external.archive;

import com.orv.archive.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminVideoApiImpl implements AdminVideoApi {
    private final VideoRepository videoRepository;

    @Override
    public List<VideoInfo> getVideosByMemberId(UUID memberId) {
        return videoRepository.findAllByMemberId(memberId).stream()
            .map(video -> new VideoInfo(
                video.getId(),
                video.getMemberId(),
                video.getTitle(),
                video.getStatus()
            ))
            .collect(Collectors.toList());
    }
}
