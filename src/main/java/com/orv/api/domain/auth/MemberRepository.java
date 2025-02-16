package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByProviderAndSocialId(String provider, String socialId);

    Optional<Member> findByNickname(String nickname);

    Member save(Member member);
}
