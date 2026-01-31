package com.orv.auth.repository.jdbc;

import com.orv.auth.domain.Member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "deprecation"})
class JdbcMemberRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcMemberRepository createRepository() {
        return new JdbcMemberRepository(jdbcTemplate);
    }

    private Member createTestMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setNickname("testUser");
        member.setProvider("testProvider");
        member.setSocialId("socialId123");
        member.setEmail("test@example.com");
        member.setName("USER");
        return member;
    }

    @Test
    void testFindByProviderAndSocialId_userFound() {
        // given
        Member member = createTestMember();

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenReturn(member);

        JdbcMemberRepository repository = createRepository();

        // when
        Optional<Member> retrievedOpt = repository.findByProviderAndSocialId("testProvider", "socialId123");

        // then
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Member retrieved = retrievedOpt.get();
        assertEquals(member.getId(), retrieved.getId(), "저장된 멤버와 조회된 멤버의 id가 동일해야 합니다.");
    }

    @Test
    void testFindByProviderAndSocialId_userNotFound() {
        // given
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        JdbcMemberRepository repository = createRepository();

        // when
        Optional<Member> retrievedOpt = repository.findByProviderAndSocialId("nonexistentProvider", "nonexistentSocialId");

        // then
        assertTrue(retrievedOpt.isEmpty(), "조회 결과가 없어야 합니다.");
    }

    @Test
    void testFindByNickname_userFound() {
        // given
        Member member = createTestMember();
        member.setNickname("abc가나123");

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenReturn(member);

        JdbcMemberRepository repository = createRepository();

        // when
        Optional<Member> retrievedOpt = repository.findByNickname("abc가나123");

        // then
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Member retrieved = retrievedOpt.get();
        assertEquals(member.getId(), retrieved.getId(), "저장된 멤버와 조회된 멤버의 id가 동일해야 합니다.");
    }

    @Test
    void testFindByNickname_userNotFound() {
        // given
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        JdbcMemberRepository repository = createRepository();

        // when
        Optional<Member> retrievedOpt = repository.findByNickname("nonexistentNickname");

        // then
        assertTrue(retrievedOpt.isEmpty(), "조회 결과가 없어야 합니다.");
    }

    @Test
    void testFindById_userFound() {
        // given
        Member member = createTestMember();

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenReturn(member);

        JdbcMemberRepository repository = createRepository();

        // when
        Optional<Member> retrievedOpt = repository.findById(member.getId());

        // then
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Member retrieved = retrievedOpt.get();
        assertEquals(member.getId(), retrieved.getId(), "저장된 멤버와 조회된 멤버의 id가 동일해야 합니다.");
    }

    @Test
    void testFindById_userNotFound() {
        // given
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        JdbcMemberRepository repository = createRepository();

        // when
        Optional<Member> retrievedOpt = repository.findById(UUID.randomUUID());

        // then
        assertTrue(retrievedOpt.isEmpty(), "조회 결과가 없어야 합니다.");
    }
}
