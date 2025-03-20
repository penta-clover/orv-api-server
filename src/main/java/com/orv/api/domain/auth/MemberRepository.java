package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Optional<Member> findByProviderAndSocialId(String provider, String socialId);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findById(UUID memberId);

    Member save(Member member);
}
