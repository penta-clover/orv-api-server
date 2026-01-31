BEGIN;

-- 1. 새로운 스토리보드 생성
INSERT INTO storyboard (id, title, start_scene_id)
VALUES (
           '18779df7-a80d-497c-9206-9e61540bb465',  -- 하루의 계획은 아침에 달려있다 (1)
           '하루의 계획은 아침에 달려있다 (1)',
           NULL
       );

-- 2. 스토리보드 미리보기(Preview) 생성
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES (
           '18779df7-a80d-497c-9206-9e61540bb465',
           ARRAY [
               '오늘 중요한 일정이 있다면 3가지만 말해주세요.',
               '하루 일과가 끝나고 누웠을 때 어떤 모습이기를 원하나요?'
               ]
       );

-- 3. 스토리보드에 속하는 씬들 생성

-- scene 1
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '20fbca77-249f-4826-8c30-b1dc3af847c0',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "가벼운 인사 한마디 부탁 드립니다.",
             "hint": "나이, 이름, 오늘의 인터뷰 주제를 함께 표현해주시면 좋아요. 이외에도 자유롭게 말해주세요.",
             "nextSceneId": "76d71829-3273-452b-8d26-6d392f5960dc"
           }'::json
       );

-- scene 2
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '76d71829-3273-452b-8d26-6d392f5960dc',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "오늘 하루를 감사 인사로 시작해보세요",
             "hint": "오늘 아침에 눈을 뜨면서 어떤 점이 감사한가요? 평소에 당연하게 느껴지던 순간을 한번 되돌아보세요.",
             "nextSceneId": "fc0b8b3e-4ec0-4eb8-a3ee-19dfd7125a8e"
           }'::json
       );

-- scene 3
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'fc0b8b3e-4ec0-4eb8-a3ee-19dfd7125a8e',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "눈을 감고 3분 정도만 노래를 들으며 명상해볼까요?",
             "hint": "꼭 노트북 앞이 아니어도 좋아요. 조금 더 편하게 땅바닥에 자세를 잡고 앉아도 좋습니다.",
             "nextSceneId": "e1fb9171-ab65-47bf-a8af-cadf78567b0b"
           }'::json
       );

-- scene 4
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'e1fb9171-ab65-47bf-a8af-cadf78567b0b',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "오늘 중요한 일정이 있다면 3가지만 말해주세요.",
             "hint": "잘하고 싶고 중요하다고 느끼는 일정을 공유해주세요.",
             "nextSceneId": "552b80bb-5728-4019-bca6-96e56d904836"
           }'::json
       );

-- scene 5
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '552b80bb-5728-4019-bca6-96e56d904836',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "나 스스로를 위한 노래 1곡을 추천해주세요.",
             "hint": "오늘 해야 할 일을 위한 노동요, 등교/출근길 전투곡, 혹은 심신 안정 음악 등 무엇이든 좋습니다.",
             "nextSceneId": "f3021af7-f513-43c2-b3f3-a93001ff689d"
           }'::json
       );

-- scene 6
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'f3021af7-f513-43c2-b3f3-a93001ff689d',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "하루 일과가 끝나고 누웠을 때 어떤 모습이기를 원하나요?",
             "hint": "일정을 끝내고 침대에 딱 누웠을 때, 나는 어떤 기분과 상태이길 바라나요?",
             "nextSceneId": "5b7b0939-c28c-4c86-afdc-c9fd96de3629"
           }'::json
       );

-- scene 7
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '5b7b0939-c28c-4c86-afdc-c9fd96de3629',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'QUESTION',
           '{
             "question": "하루를 잘 보내기 위해 스스로를 위한 응원 한마디 해주세요.",
             "hint": "오늘 하루를 만족스럽게 살기 위해 나에게 가장 필요한 말은 무엇일까요? 스스로를 위한 응원의 한마디 부탁드려요.",
             "nextSceneId": "7fc6e476-4074-4d3f-b5cb-2b091ddba680"
           }'::json
       );

-- scene 8 (EPILOGUE)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '7fc6e476-4074-4d3f-b5cb-2b091ddba680',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'EPILOGUE',
           (
                   '{ ' ||
                   '   "question": "아래 문구를 따라 읽어주세요", ' ||
                   '   "hint": "\"2025년 3월 30일 오늘은 여기까지\"", ' ||
                   '   "nextSceneId": "53deb936-122b-4fdd-a639-4189a8745c59"' ||
                   '}'
           )::json
       );

-- scene 9 (END)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '53deb936-122b-4fdd-a639-4189a8745c59',
           '18779df7-a80d-497c-9206-9e61540bb465',
           'scene_title',
           'END',
           '{}'::json
       );

-- 4. start_scene_id를 첫 번째 씬으로 업데이트
UPDATE storyboard
SET start_scene_id = '20fbca77-249f-4826-8c30-b1dc3af847c0'
WHERE id = '18779df7-a80d-497c-9206-9e61540bb465';

-- 5. 새 토픽 생성
INSERT INTO topic (id, name, description, thumbnail_url)
VALUES (
           'eb64bdf7-c270-4bea-b84c-195d6a6e81d6',
           '하루의 계획은 아침에 달려있다',
           '하루를 잘 보내고 싶은 당신에게 추천드려요. 위대한 사람들 중 많은 분들이 하루가 아침에 달려있다고 말합니다. 오늘 아침은 오브와 함께 시작해봐요.',
           'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'
       );

-- 6. storyboard_topic으로 연결
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES (
           '18779df7-a80d-497c-9206-9e61540bb465',
           'eb64bdf7-c270-4bea-b84c-195d6a6e81d6'
       );

COMMIT;
