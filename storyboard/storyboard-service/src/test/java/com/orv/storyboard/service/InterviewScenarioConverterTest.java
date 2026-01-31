package com.orv.storyboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.recap.domain.InterviewScenario;
import com.orv.recap.domain.SceneInfo;
import com.orv.storyboard.service.InterviewScenarioConverter;
import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InterviewScenarioConverterTest {

    private InterviewScenarioConverter interviewScenarioFactory;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        interviewScenarioFactory = new InterviewScenarioConverter(objectMapper);
    }

    @Test
    @DisplayName("정상적인 스토리보드 데이터로 InterviewScenario를 생성한다")
    void create_WithValidStoryboard_ShouldReturnCorrectInterviewScenario() {
        // given
        UUID storyboardId = UUID.fromString("b1f399c7-293f-4215-a717-e0c9becd6d9b");
        UUID startSceneId = UUID.fromString("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a");

        Storyboard storyboard = new Storyboard();
        storyboard.setId(storyboardId);
        storyboard.setTitle("HySpark Full Interview");
        storyboard.setStartSceneId(startSceneId);

        List<Scene> scenes = createFullSceneList(storyboardId);
        Collections.shuffle(scenes); // 순서가 섞여도 정렬이 잘 되는지 확인

        // when
        InterviewScenario result = interviewScenarioFactory.create(storyboard, scenes);

        // then
        assertNotNull(result);
        assertEquals("HySpark Full Interview", result.getTitle());
        assertEquals(9, result.getScenes().size());

        List<SceneInfo> sceneInfos = result.getScenes();
        assertEquals("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a", sceneInfos.get(0).getSceneId());
        assertEquals("가벼운 인사 한마디 부탁 드립니다.", sceneInfos.get(0).getQuestion());

        assertEquals("b7ca99d7-55d6-4eb1-9102-8957c1275ee5", sceneInfos.get(1).getSceneId());
        assertEquals("@{name}님은 왜 HySpark에 들어 오려고 했나요?", sceneInfos.get(1).getQuestion());

        assertEquals("b41be536-ad00-4f96-8600-54caffa28401", sceneInfos.get(8).getSceneId());
        assertEquals("HySpark에서 어떤 사람으로 기억되고 싶나요?", sceneInfos.get(8).getQuestion());
    }

    @Test
    @DisplayName("중간 Scene이 누락된 경우, IllegalStateException을 발생시킨다")
    void create_WithMissingScene_ShouldThrowIllegalStateException() {
        // given
        UUID storyboardId = UUID.fromString("b1f399c7-293f-4215-a717-e0c9becd6d9b");
        UUID startSceneId = UUID.fromString("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a");

        Storyboard storyboard = new Storyboard();
        storyboard.setId(storyboardId);
        storyboard.setTitle("Missing Scene Test");
        storyboard.setStartSceneId(startSceneId);

        List<Scene> scenes = new ArrayList<>();
        scenes.add(createScene(storyboardId, "b33dbf34-7f5d-47db-84f6-0c846eeb0b6a", "QUESTION", "{\"question\": \"Scene 1\", \"nextSceneId\": \"b7ca99d7-55d6-4eb1-9102-8957c1275ee5\"}"));
        // "b7ca99d7-55d6-4eb1-9102-8957c1275ee5" Scene is missing
        scenes.add(createScene(storyboardId, "c247a878-a3c0-4788-9af4-212e60795253", "QUESTION", "{\"question\": \"Scene 3\", \"nextSceneId\": null}"));

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            interviewScenarioFactory.create(storyboard, scenes);
        });
    }

    @Test
    @DisplayName("QUESTION 타입의 Scene이 없는 경우, 빈 리스트를 반환한다")
    void create_WithNoQuestionScenes_ShouldReturnEmptyList() {
        // given
        UUID storyboardId = UUID.fromString("b1f399c7-293f-4215-a717-e0c9becd6d9b");
        UUID startSceneId = UUID.fromString("b7aab9d7-bdfe-4fac-9757-55a87a7e0cce");

        Storyboard storyboard = new Storyboard();
        storyboard.setId(storyboardId);
        storyboard.setTitle("No Question Scene Test");
        storyboard.setStartSceneId(startSceneId);

        List<Scene> scenes = new ArrayList<>();
        scenes.add(createScene(storyboardId, "b7aab9d7-bdfe-4fac-9757-55a87a7e0cce", "EPILOGUE", "{\"question\": \"Epilogue\", \"nextSceneId\": \"b9dc9e42-be65-4b9f-a2d3-1aa0243c06eb\"}"));
        scenes.add(createScene(storyboardId, "b9dc9e42-be65-4b9f-a2d3-1aa0243c06eb", "END", "{}"));

        // when
        InterviewScenario result = interviewScenarioFactory.create(storyboard, scenes);

        // then
        assertNotNull(result);
        assertTrue(result.getScenes().isEmpty());
    }

    @Test
    @DisplayName("Scene이 순환 구조를 이룰 경우, IllegalStateException을 발생시킨다")
    void create_WithCircularReference_ShouldThrowIllegalStateException() {
        // given
        UUID storyboardId = UUID.fromString("b1f399c7-293f-4215-a717-e0c9becd6d9b");
        UUID startSceneId = UUID.fromString("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a");

        Storyboard storyboard = new Storyboard();
        storyboard.setId(storyboardId);
        storyboard.setTitle("Circular Reference Test");
        storyboard.setStartSceneId(startSceneId);

        List<Scene> scenes = new ArrayList<>();
        scenes.add(createScene(storyboardId, "b33dbf34-7f5d-47db-84f6-0c846eeb0b6a", "QUESTION", "{\"question\": \"Scene 1\", \"nextSceneId\": \"b7ca99d7-55d6-4eb1-9102-8957c1275ee5\"}"));
        scenes.add(createScene(storyboardId, "b7ca99d7-55d6-4eb1-9102-8957c1275ee5", "QUESTION", "{\"question\": \"Scene 2\", \"nextSceneId\": \"b33dbf34-7f5d-47db-84f6-0c846eeb0b6a\"}")); // Circular reference

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            interviewScenarioFactory.create(storyboard, scenes);
        });
    }

    private List<Scene> createFullSceneList(UUID storyboardId) {
        List<Scene> scenes = new ArrayList<>();
        scenes.add(createScene(storyboardId, "b33dbf34-7f5d-47db-84f6-0c846eeb0b6a", "QUESTION", "{\"question\": \"가벼운 인사 한마디 부탁 드립니다.\", \"nextSceneId\": \"b7ca99d7-55d6-4eb1-9102-8957c1275ee5\"}"));
        scenes.add(createScene(storyboardId, "b7ca99d7-55d6-4eb1-9102-8957c1275ee5", "QUESTION", "{\"question\": \"@{name}님은 왜 HySpark에 들어 오려고 했나요?\", \"nextSceneId\": \"c247a878-a3c0-4788-9af4-212e60795253\"}"));
        scenes.add(createScene(storyboardId, "c247a878-a3c0-4788-9af4-212e60795253", "QUESTION", "{\"question\": \"처음 창업에 관심을 갖게 된 계기가 무엇인가요?\", \"nextSceneId\": \"bf5ae112-bc05-4359-88fa-8052508a9347\"}"));
        scenes.add(createScene(storyboardId, "bf5ae112-bc05-4359-88fa-8052508a9347", "QUESTION", "{\"question\": \"지금까지의 HySpark 활동 중 가장 기억에 남는 활동은 무엇인가요?\", \"nextSceneId\": \"b5d247a7-0b90-49bb-bef5-379cd0ae8a22\"}"));
        scenes.add(createScene(storyboardId, "b5d247a7-0b90-49bb-bef5-379cd0ae8a22", "QUESTION", "{\"question\": \"4기 학회원들에게 10만원톤에 대한 조언을 해준다면 어떤 이야기를 하실 건가요?\", \"nextSceneId\": \"b7a7df57-f03d-4fbb-8c33-868140fae739\"}"));
        scenes.add(createScene(storyboardId, "b7a7df57-f03d-4fbb-8c33-868140fae739", "QUESTION", "{\"question\": \"3기 학회원 1명과 함께 창업을 해야 한다면 누구랑 함께 할 건가요?\", \"nextSceneId\": \"b9fbdf32-4c15-4fb9-9624-22b178612c0e\"}"));
        scenes.add(createScene(storyboardId, "b9fbdf32-4c15-4fb9-9624-22b178612c0e", "QUESTION", "{\"question\": \"이번 HySpark 3기가 끝날 때 @{name}은 어떤 모습일 것 같나요?\", \"nextSceneId\": \"b8785d13-a99f-453d-9ece-84065ce7ce95\"}"));
        scenes.add(createScene(storyboardId, "b8785d13-a99f-453d-9ece-84065ce7ce95", "QUESTION", "{\"question\": \"@{name}님은 본인이 창업과 잘 맞는다고 생각하시나요?\", \"nextSceneId\": \"b41be536-ad00-4f96-8600-54caffa28401\"}"));
        scenes.add(createScene(storyboardId, "b41be536-ad00-4f96-8600-54caffa28401", "QUESTION", "{\"question\": \"HySpark에서 어떤 사람으로 기억되고 싶나요?\", \"nextSceneId\": \"b7aab9d7-bdfe-4fac-9757-55a87a7e0cce\"}"));
        scenes.add(createScene(storyboardId, "b7aab9d7-bdfe-4fac-9757-55a87a7e0cce", "EPILOGUE", "{\"question\": \"아래 문구를 따라 읽어주세요\", \"nextSceneId\": \"b9dc9e42-be65-4b9f-a2d3-1aa0243c06eb\"}"));
        scenes.add(createScene(storyboardId, "b9dc9e42-be65-4b9f-a2d3-1aa0243c06eb", "END", "{}"));
        return scenes;
    }

    private Scene createScene(UUID storyboardId, String sceneId, String type, String content) {
        Scene scene = new Scene();
        scene.setId(UUID.fromString(sceneId));
        scene.setStoryboardId(storyboardId);
        scene.setSceneType(type);
        scene.setContent(content);
        return scene;
    }
}
