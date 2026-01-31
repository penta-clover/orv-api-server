package com.orv.auth.service;

import com.orv.auth.repository.MemberRepository;
import com.orv.auth.service.MemberService;
import com.orv.auth.domain.Member;
import com.orv.auth.domain.MemberInfo;
import com.orv.auth.domain.ValidationResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;


    @ParameterizedTest
    @ValueSource(strings = {"abc", "가나다", "ㅏㅑㅓㅕㅗㅛㅜㅠ", "가a나b123", "123"})
    public void testValidateNickname_whenValidAndNotExists(String nickname) {
        // given
        when(memberRepository.findByNickname(nickname)).thenReturn(Optional.empty());

        // when
        ValidationResult result = memberService.validateNickname(nickname);

        // then
        assertEquals(nickname, result.getNickname());
        assertTrue(result.getIsValid(), String.format("닉네임 '%s'이 유효해야 합니다.", nickname));
        assertFalse(result.getIsExists(), String.format("닉네임 '%s'는 존재하지 않는 닉네임이어야 합니다.", nickname));
    }


    @ParameterizedTest
    @ValueSource(strings = {"invalidnickname", "", "abc!", "%abc", "\n", "hi hi"})
    public void testValidateNickname_whenInvalid(String invalidNickname) {
        // given
        when(memberRepository.findByNickname(invalidNickname)).thenReturn(Optional.empty());

        // when
        ValidationResult result = memberService.validateNickname(invalidNickname);

        // then
        assertEquals(result.getNickname(), invalidNickname);
        assertFalse(result.getIsValid(), String.format("닉네임 '%s'이 유효하지 않아야 합니다", invalidNickname));
        assertFalse(result.getIsExists(), String.format("닉네임 '%s'은 유효하지 않으므로 존재하지 않아야 합니다", invalidNickname));
    }


    @ParameterizedTest
    @ValueSource(strings = {"abc", "가나다", "ㅏㅑㅓㅕㅗㅛㅜㅠ", "가a나b123", "123"})
    public void testValidateNickname_whenExists(String nickname) {
        // given
        Member dummy = new Member();
        dummy.setNickname(nickname);
        when(memberRepository.findByNickname(nickname)).thenReturn(Optional.of(dummy));

        // when
        ValidationResult result = memberService.validateNickname(nickname);

        // then
        assertEquals(nickname, result.getNickname());
        assertTrue(result.getIsValid(), String.format("닉네임 '%s'이 유효해야 합니다.", nickname));
        assertTrue(result.getIsExists(), String.format("닉네임 '%s'는 존재해야.", nickname));
    }


    @Test
    void testJoin() {
        // given: 테스트 데이터 준비
        String id = UUID.randomUUID().toString();
        String nickname = "test123";
        String gender = "MALE"; // CHECK 제약조건에 따라 "MALE" 또는 "FEMALE" 사용
        LocalDate birthday = LocalDate.of(1990, 1, 1);
        String provider = "testProvider";
        String socialId = "social123";
        String phoneNumber = "01012345678";

        // repository.save()가 호출될 때 반환할 Member 객체 생성
        Member savedMember = new Member();
        UUID generatedId = UUID.randomUUID();
        savedMember.setId(generatedId);
        savedMember.setNickname(nickname);
        savedMember.setGender(gender);
        savedMember.setBirthday(birthday);
        savedMember.setProvider(provider);
        savedMember.setSocialId(socialId);
        savedMember.setPhoneNumber(phoneNumber);
        // 필요 시 다른 필드도 설정할 수 있음

        // when: memberRepository.save() 호출 시 savedMember를 반환하도록 설정
        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

        // when: join() 메서드 실행
        boolean result = memberService.join(id, nickname, gender, birthday, provider, socialId, phoneNumber);

        // then: join()이 true를 반환해야 함
        assertTrue(result, "Join 메서드는 true를 반환해야 합니다.");

        // 반환된 savedMember의 id가 null이 아니어야 함
        assertThat(savedMember.getId()).isNotNull();
    }

    @Test
    void testGetMyInfo() {
        // given
        String id = UUID.randomUUID().toString();
        String nickname = "test123";
        String gender = "MALE"; // CHECK 제약조건에 따라 "MALE" 또는 "FEMALE" 사용
        LocalDateTime createdAt = LocalDateTime.now().minusDays(3);
        LocalDate birthday = LocalDate.of(1990, 1, 1);
        String provider = "testProvider";
        String socialId = "social123";
        String profileImageUrl = "https://naver.com/blabla";

        // repository.findById()가 호출될 때 반환할 Member 객체 생성
        Member foundMember = new Member();
        UUID generatedId = UUID.randomUUID();
        foundMember.setId(generatedId);
        foundMember.setNickname(nickname);
        foundMember.setCreatedAt(createdAt);
        foundMember.setGender(gender);
        foundMember.setBirthday(birthday);
        foundMember.setProvider(provider);
        foundMember.setSocialId(socialId);
        foundMember.setProfileImageUrl(profileImageUrl);

        when(memberRepository.findById(any())).thenReturn(Optional.of(foundMember));

        // when
        MemberInfo memberInfo = memberService.getMyInfo(UUID.fromString(id));

        // then
        assertEquals(memberInfo.getId(), foundMember.getId());
        assertEquals(memberInfo.getNickname(), foundMember.getNickname());
        assertEquals(memberInfo.getProfileImageUrl(), foundMember.getProfileImageUrl());
        assertEquals(memberInfo.getCreatedAt(), foundMember.getCreatedAt());
    }
}

