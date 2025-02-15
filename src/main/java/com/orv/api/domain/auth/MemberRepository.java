package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByProviderAndSocialId(String provider, String socialId);

    Member save(Member member);
}
