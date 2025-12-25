package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.auth.dto.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Optional<Member> findByProviderAndSocialId(String provider, String socialId);
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findById(UUID memberId);
    Optional<List<Role>> findRolesById(UUID memberId);
    List<Member> findByProvider(String provider);
    Member save(Member member);
}
