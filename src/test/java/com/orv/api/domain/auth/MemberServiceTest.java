package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.auth.dto.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
        String nickname = "test123";
        String gender = "MALE"; // CHECK 제약조건에 따라 "MALE" 또는 "FEMALE" 사용
        LocalDate birthday = LocalDate.of(1990, 1, 1);
        String provider = "testProvider";
        String socialId = "social123";

        // repository.save()가 호출될 때 반환할 Member 객체 생성
        Member savedMember = new Member();
        UUID generatedId = UUID.randomUUID();
        savedMember.setId(generatedId);
        savedMember.setNickname(nickname);
        savedMember.setGender(gender);
        savedMember.setBirthday(birthday);
        savedMember.setProvider(provider);
        savedMember.setSocialId(socialId);
        // 필요 시 다른 필드도 설정할 수 있음

        // when: memberRepository.save() 호출 시 savedMember를 반환하도록 설정
        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

        // when: join() 메서드 실행
        boolean result = memberService.join(nickname, gender, birthday, provider, socialId);

        // then: join()이 true를 반환해야 함
        assertTrue(result, "Join 메서드는 true를 반환해야 합니다.");

        // 그리고, repository.save()에 전달된 Member 객체의 필드들을 캡처하여 검증
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository, times(1)).save(memberCaptor.capture());
        Member capturedMember = memberCaptor.getValue();

        // 전달된 데이터가 올바르게 설정되었는지 확인
        assertThat(capturedMember.getNickname()).isEqualTo(nickname);
        assertThat(capturedMember.getGender()).isEqualTo(gender);
        assertThat(capturedMember.getBirthday()).isEqualTo(birthday);
        assertThat(capturedMember.getProvider()).isEqualTo(provider);
        assertThat(capturedMember.getSocialId()).isEqualTo(socialId);

        // 반환된 savedMember의 id가 null이 아니어야 함
        assertThat(savedMember.getId()).isNotNull();
    }
}

