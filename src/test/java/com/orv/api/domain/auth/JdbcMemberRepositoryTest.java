package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest  // 애플리케이션 컨텍스트를 로드하여 실제 DB와 연결해 테스트
@Transactional
class JdbcMemberRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void cleanDatabase() {
        // member 테이블의 데이터를 모두 삭제 (필요 시 RESTART IDENTITY 옵션 추가)
        jdbcTemplate.execute("TRUNCATE TABLE member RESTART IDENTITY CASCADE");
    }

    @Test
    void testSaveAndFindByProviderAndSocialId_userFound() {
        // given
        Member member = new Member();
        member.setNickname("testUser");
        member.setProvider("testProvider");
        member.setSocialId("socialId123");
        member.setEmail("test@example.com");

        // when
        Member savedMember = memberRepository.save(member);
        assertNotNull(savedMember.getId(), "저장된 멤버의 id는 null이 아니어야 합니다.");

        // then
        Optional<Member> retrievedOpt = memberRepository.findByProviderAndSocialId("testProvider", "socialId123");
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Member retrieved = retrievedOpt.get();
        assertEquals(savedMember.getId(), retrieved.getId(), "저장된 멤버와 조회된 멤버의 id가 동일해야 합니다.");
    }

    @Test
    void testFindByProviderAndSocialId_userNotFound() {
        // given
        // 테스트를 위한 provider, socialId 값 (저장된 사용자가 없다고 가정)
        String provider = "nonexistentProvider";
        String socialId = "nonexistentSocialId";

        // when
        Optional<Member> retrievedOpt = memberRepository.findByProviderAndSocialId(provider, socialId);

        // then
        assertTrue(retrievedOpt.isEmpty(), "조회 결과가 없어야 합니다.");
    }

    @Test
    void testSaveAndFindByNickname_userFound() {
        // given
        Member member = new Member();
        member.setNickname("abc가나123");
        member.setProvider("testProvider");
        member.setSocialId("socialId123");
        member.setEmail("test@example.com");

        // when
        Member savedMember = memberRepository.save(member);
        assertNotNull(savedMember.getId(), "저장된 멤버의 id는 null이 아니어야 합니다.");

        // then
        Optional<Member> retrievedOpt = memberRepository.findByNickname("abc가나123");
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Member retrieved = retrievedOpt.get();
        assertEquals(savedMember.getId(), retrieved.getId(), "저장된 멤버와 조회된 멤버의 id가 동일해야 합니다.");
    }

    @Test
    void testSaveAndFindByNickname_userNotFound() {
        // given
        Member member = new Member();
        member.setNickname("abc가나123");
        member.setProvider("testProvider");
        member.setSocialId("socialId123");
        member.setEmail("test@example.com");

        // when
        Member savedMember = memberRepository.save(member);
        assertNotNull(savedMember.getId(), "저장된 멤버의 id는 null이 아니어야 합니다.");

        // then
        List<String> testNicknames = List.of("abc가나12", "", "abc", "123");
        for (String testNickname: testNicknames) {
            Optional<Member> retrieved = memberRepository.findByNickname(testNickname);
            assertTrue(retrieved.isEmpty(), String.format("닉네임 '%s'에 대한 조회 결과가 없어야 합니다.", testNickname));
        }
    }
}