package com.orv.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.auth.domain.Member;
import com.orv.auth.domain.Role;

public interface MemberRepository {
    Optional<Member> findByProviderAndSocialId(String provider, String socialId);
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findById(UUID memberId);
    Optional<List<Role>> findRolesById(UUID memberId);
    List<Member> findByProvider(String provider);
    Member save(Member member);
}
