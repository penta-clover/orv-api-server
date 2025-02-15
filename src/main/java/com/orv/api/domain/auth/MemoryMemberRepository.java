package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    public Member save(Member member) {
        String id = member.getId();

        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        member.setId(id);
        store.put(id, member);

        return member;
    }
}
