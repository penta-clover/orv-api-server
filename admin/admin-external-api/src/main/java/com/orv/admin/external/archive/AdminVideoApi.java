package com.orv.admin.external.archive;

import java.util.List;
import java.util.UUID;

/**
 * Cross-domain API for Admin to access Archive domain
 */
public interface AdminVideoApi {
    List<VideoInfo> getVideosByMemberId(UUID memberId);

    class VideoInfo {
        private final UUID id;
        private final UUID memberId;
        private final String title;
        private final String status;

        public VideoInfo(UUID id, UUID memberId, String title, String status) {
            this.id = id;
            this.memberId = memberId;
            this.title = title;
            this.status = status;
        }

        public UUID getId() { return id; }
        public UUID getMemberId() { return memberId; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
    }
}
