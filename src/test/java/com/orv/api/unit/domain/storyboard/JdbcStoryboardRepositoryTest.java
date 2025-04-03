package com.orv.api.unit.domain.storyboard;

import com.orv.api.domain.auth.MemberRepository;
import com.orv.api.domain.storyboard.StoryboardRepository;
import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Storyboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JdbcStoryboardRepositoryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StoryboardRepository storyboardRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE storyboard RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE scene RESTART IDENTITY CASCADE");
    }

    @Test
    void testSaveAndFindStoryboardById_storyboardFound() {
        // given
        Storyboard storyboard = new Storyboard();
        storyboard.setTitle("testStoryboard");
        Scene scene = new Scene();
        scene.setName("testScene");
        scene.setSceneType("testSceneType");
        scene.setContent("{ \"name\": \"testContent\" }");

        // when
        Storyboard savedStoryboard = storyboardRepository.save(storyboard);
        scene.setStoryboardId(savedStoryboard.getId());
        Scene savedScene = storyboardRepository.saveScene(scene);

        // then
        Optional<Storyboard> retrievedOpt = storyboardRepository.findById(savedStoryboard.getId());
        assertTrue(retrievedOpt.isPresent(), "조회 결과가 있어야 합니다.");
        Storyboard retrieved = retrievedOpt.get();
        assertEquals(savedStoryboard.getId(), retrieved.getId(), "저장된 스토리보드와 조회된 스토리보드의 id가 동일해야 합니다.");
    }
}
