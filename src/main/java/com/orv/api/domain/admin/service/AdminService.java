package com.orv.api.domain.admin.service;

import com.orv.api.domain.archive.repository.VideoRepository;
import com.orv.api.domain.archive.service.dto.Video;
import com.orv.api.domain.auth.repository.MemberRepository;
import com.orv.api.domain.auth.service.dto.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final MemberRepository memberRepository;
    private final VideoRepository videoRepository;

    public List<Member> getMembersByProvider(String provider) {
        return memberRepository.findByProvider(provider);
    }

    public List<Video> getVideosByMemberId(UUID memberId) {
        return videoRepository.findAllByMemberId(memberId);
    }
}
