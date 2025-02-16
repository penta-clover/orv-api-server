package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.auth.dto.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

}

