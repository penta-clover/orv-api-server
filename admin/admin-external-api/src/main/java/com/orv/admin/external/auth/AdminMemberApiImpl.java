package com.orv.admin.external.auth;

import com.orv.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminMemberApiImpl implements AdminMemberApi {
    private final MemberRepository memberRepository;

    @Override
    public List<MemberInfo> getMembersByProvider(String provider) {
        return memberRepository.findByProvider(provider).stream()
            .map(member -> new MemberInfo(
                member.getId(),
                member.getNickname(),
                member.getProvider(),
                member.getSocialId()
            ))
            .collect(Collectors.toList());
    }
}
