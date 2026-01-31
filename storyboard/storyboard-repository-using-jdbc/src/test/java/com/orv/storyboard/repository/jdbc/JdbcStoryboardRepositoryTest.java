package com.orv.storyboard.repository.jdbc;

import com.orv.storyboard.domain.Storyboard;

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
public class JdbcStoryboardRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFindStoryboardById_storyboardFound() {
        // given
        UUID storyboardId = UUID.randomUUID();
        Storyboard storyboard = new Storyboard();
        storyboard.setId(storyboardId);
        storyboard.setTitle("testStoryboard");

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenReturn(storyboard);

        JdbcStoryboardRepository repository = new JdbcStoryboardRepository(jdbcTemplate);

        // when
        Optional<Storyboard> retrievedOpt = repository.findById(storyboardId);

        // then
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Storyboard retrieved = retrievedOpt.get();
        assertEquals(storyboardId, retrieved.getId(), "저장된 스토리보드와 조회된 스토리보드의 id가 동일해야 합니다.");
        assertEquals("testStoryboard", retrieved.getTitle());
    }

    @Test
    void testFindStoryboardById_storyboardNotFound() {
        // given
        UUID storyboardId = UUID.randomUUID();

        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), any(BeanPropertyRowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        JdbcStoryboardRepository repository = new JdbcStoryboardRepository(jdbcTemplate);

        // when
        Optional<Storyboard> retrievedOpt = repository.findById(storyboardId);

        // then
        assertTrue(retrievedOpt.isEmpty(), "조회 결과가 없어야 합니다.");
    }
}
