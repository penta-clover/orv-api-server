package com.orv.recap.external.archive;

import com.orv.archive.service.ArchiveService;
import com.orv.archive.domain.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecapArchiveApiImpl implements RecapArchiveApi {
    private final ArchiveService archiveService;

    @Override
    public Optional<VideoInfo> getVideo(UUID videoId) {
        return archiveService.getVideo(videoId)
            .map(video -> new VideoInfo(
                video.getId(),
                video.getStoryboardId(),
                video.getMemberId(),
                video.getTitle(),
                video.getRunningTime()
            ));
    }

    @Override
    public Optional<InputStream> getVideoStream(UUID videoId) {
        return archiveService.getVideoStream(videoId);
    }
}
