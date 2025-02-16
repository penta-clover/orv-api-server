package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private boolean isNicknameExists(String nickname) {
        return memberRepository.findByNickname(nickname).isPresent();
    }
}