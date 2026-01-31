package com.orv.auth.repository.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.orv.auth.domain.Member;
import com.orv.auth.domain.Role;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.orv.auth.repository.MemberRepository;
@Slf4j
@Repository
public class MemoryMemberRepository implements MemberRepository {
    ConcurrentHashMap<String, Member> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Member> findByProviderAndSocialId(String provider, String socialId) {
        for (Map.Entry<String, Member> keyAndValue : store.entrySet()) {
            Member member = keyAndValue.getValue();

            if (member.getProvider().equals(provider) && member.getSocialId().equals(socialId)) {
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Member> findByNickname(String nickname) {
        for (Map.Entry<String, Member> keyAndValue : store.entrySet()) {
            Member member = keyAndValue.getValue();

            if (member.getNickname().equals(nickname)) {
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<List<Role>> findRolesById(UUID memberId) {
        return Optional.of(new ArrayList<>());
    }

    @Override
    public Member save(Member member) {
        UUID id = member.getId();

        if (id == null) {
            id = UUID.randomUUID();
        }

        member.setId(id);
        store.put(id.toString(), member);

        return member;
    }

    @Override
    public Optional<Member> findById(UUID memberId) {
        Member member = store.get(memberId.toString());
        return Optional.of(member);
    }

    @Override
    public List<Member> findByProvider(String provider) {
        List<Member> result = new ArrayList<>();
        for (Member member : store.values()) {
            if (provider.equals(member.getProvider())) {
                result.add(member);
            }
        }
        return result;
    }
}
