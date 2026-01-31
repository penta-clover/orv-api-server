package com.orv.auth.service;

import com.orv.auth.repository.MemberRepository;
import com.orv.auth.domain.Member;
import com.orv.auth.domain.MemberInfo;
import com.orv.auth.domain.MemberProfile;
import com.orv.auth.domain.Role;
import com.orv.auth.domain.ValidationResult;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public ValidationResult validateNickname(String nickname) {
        ValidationResult result = new ValidationResult();
        result.setNickname(nickname);
        result.setIsValid(isNicknameValid(nickname));
        result.setIsExists(isNicknameExists(nickname));

        return result;
    }

    private boolean isNicknameValid(String nickname) {
        // 정규식: 1~8자리, 한글/영어/숫자만 허용
        return nickname.matches("^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]{1,8}$");
    }

    public boolean join(String id, String nickname, String gender, LocalDate birthday, String provider, String socialId, String phoneNumber) {
        Member member = new Member();
        member.setId(UUID.fromString(id));
        member.setNickname(nickname);
        member.setGender(gender);
        member.setBirthday(birthday);
        member.setProvider(provider);
        member.setSocialId(socialId);
        member.setName("username");
        member.setPhoneNumber(phoneNumber);

        Member savedMember = memberRepository.save(member);
        return true;
    }

    public MemberInfo getMyInfo(UUID memberId) {
        Optional<Member> myInfoOrEmpty = memberRepository.findById(memberId);

        if (myInfoOrEmpty.isEmpty()) {
            return null;
        }

        Member me = myInfoOrEmpty.get();
        MemberInfo info = new MemberInfo();
        info.setId(me.getId());
        info.setNickname(me.getNickname());
        info.setProfileImageUrl(me.getProfileImageUrl());
        info.setCreatedAt(me.getCreatedAt());

        return info;
    }

    public MemberProfile getProfile(UUID memberId) {
        Optional<Member> memberOrEmpty = memberRepository.findById(memberId);

        if (memberOrEmpty.isEmpty()) {
            return null;
        }

        Member member = memberOrEmpty.get();
        MemberProfile info = new MemberProfile();
        info.setId(member.getId());
        info.setNickname(member.getNickname());
        info.setProfileImageUrl(member.getProfileImageUrl());
        info.setCreatedAt(member.getCreatedAt());

        return info;
    }

    private boolean isNicknameExists(String nickname) {
        return memberRepository.findByNickname(nickname).isPresent();
    }

    public Optional<Member> findByProviderAndSocialId(String provider, String socialId) {
        return memberRepository.findByProviderAndSocialId(provider, socialId);
    }

    public Optional<List<Role>> findRolesById(UUID memberId) {
        return memberRepository.findRolesById(memberId);
    }
}