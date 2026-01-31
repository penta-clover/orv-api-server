package com.orv.admin.external.auth;

import java.util.List;
import java.util.UUID;

/**
 * Cross-domain API for Admin to access Auth domain
 */
public interface AdminMemberApi {
    List<MemberInfo> getMembersByProvider(String provider);

    class MemberInfo {
        private final UUID id;
        private final String nickname;
        private final String provider;
        private final String socialId;

        public MemberInfo(UUID id, String nickname, String provider, String socialId) {
            this.id = id;
            this.nickname = nickname;
            this.provider = provider;
            this.socialId = socialId;
        }

        public UUID getId() { return id; }
        public String getNickname() { return nickname; }
        public String getProvider() { return provider; }
        public String getSocialId() { return socialId; }
    }
}
