BEGIN;

-- 1. 스토리보드 생성
INSERT INTO storyboard (id, title, start_scene_id)
VALUES (
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',  -- 월요병을 물리치는 법 (1)
           '월요병을 물리치는 법 (1)',
           NULL
       );

-- 2. 스토리보드 미리보기(Preview) 생성
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES (
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           ARRAY [
               '월요병이라는 말에 동의하시나요?',
               '돌아오는 주말은 어떻게 시간을 보낼 계획인가요?'
           ]
       );

-- 3. 스토리보드에 속하는 씬들 생성
-- (총 8개의 씬: 6개 QUESTION + 1 EPILOGUE + 1 END)

-- scene1
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '420d3f95-037d-4c15-b8bb-4e5c8fcb2f92',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'QUESTION',
           '{
              "question": "가벼운 인사 한마디 부탁 드립니다.",
              "hint": "오늘 하루의 나. 나이, 이름 등을 함께 표현해주시면 좋아요. 이외에도 자유롭게 말해주세요.",
              "nextSceneId": "b94cdf83-0863-419b-9094-13a2446cbf64"
           }'::json
       );

-- scene2
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'b94cdf83-0863-419b-9094-13a2446cbf64',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'QUESTION',
           '{
              "question": "월요병이라는 말에 동의하시나요?",
              "hint": "당신의 생각을 들려주세요. 이번 인터뷰를 진행하게 된 이유와 함께 고민해보시면 좋아요.",
              "nextSceneId": "805340f8-424d-4f7c-ba62-b49691c12d1c"
           }'::json
       );

-- scene3
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '805340f8-424d-4f7c-ba62-b49691c12d1c',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'QUESTION',
           '{
              "question": "내가 현재 가지고 있는 걱정거리를 말해주세요.",
              "hint": "왜 월요일이 오는 걸 싫어하나요? 월요일을 걱정하는 이유를 알려주세요.",
              "nextSceneId": "6c89db11-5c60-4fac-a22e-815647f8ceb1"
           }'::json
       );

-- scene4
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '6c89db11-5c60-4fac-a22e-815647f8ceb1',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'QUESTION',
           '{
              "question": "지금 하고 있는 일은 나한테 어떤 의미인가요?",
              "hint": "월요병은 자신이 하는 일에 대한 열정이 떨어진 신호일 수 있어요. 스스로 점검해보세요.",
              "nextSceneId": "37cf3e73-2cba-456b-b3df-5a6abfd7867c"
           }'::json
       );

-- scene5
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '37cf3e73-2cba-456b-b3df-5a6abfd7867c',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'QUESTION',
           '{
              "question": "돌아오는 주말은 어떻게 시간을 보낼 계획인가요?",
              "hint": "평일을 열심히 보낸 후 주말에는 어떻게 시간을 보낼지 들려주세요.",
              "nextSceneId": "964a9dda-dbf3-436e-b3ca-eb90b8ad8df0"
           }'::json
       );

-- scene6
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '964a9dda-dbf3-436e-b3ca-eb90b8ad8df0',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'QUESTION',
           '{
              "question": "기분 좋은 월요일을 만들기 위해 한가지 이벤트를 떠올려보세요.",
              "hint": "월요일에 자신에게 줄 보상이나 약속을 하나 정해보세요. 좋아하는 곳을 가거나 낮잠을 자는 것도 좋아요.",
              "nextSceneId": "432b9dd4-3a66-4239-bdc9-0874c5f38d7c"
           }'::json
       );

-- scene7 (EPILOGUE)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '432b9dd4-3a66-4239-bdc9-0874c5f38d7c',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'EPILOGUE',
           (
               '{ ' ||
               '   "question": "아래 문구를 따라 읽어주세요", ' ||
               '   "hint": "\"2025년 3월 30일 오늘은 여기까지\"", ' ||
               '   "nextSceneId": "27bbfdaf-29f8-451e-86df-8b3f9bd0e845"' ||
               '}'
           )::json
       );

-- scene8 (END)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '27bbfdaf-29f8-451e-86df-8b3f9bd0e845',
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'scene_title',
           'END',
           '{}'::json
       );

-- 4. 스토리보드의 start_scene_id 설정
UPDATE storyboard
SET start_scene_id = '420d3f95-037d-4c15-b8bb-4e5c8fcb2f92'
WHERE id = '0afecfc8-62a4-4398-85a8-0cff8b8f698f';

-- 5. 새 토픽 생성
INSERT INTO topic (id, name, description, thumbnail_url)
VALUES (
           'f70d7ebf-9058-4c6b-9afd-03bd23ea10d9',
           '월요병을 물리치는 법',
           '유독 월요일에는 몸이 축축 늘어지고 더 피곤한 것 같아요. 이번 기회에 월요병에 대해서 함께 탐구해봐요. 어쩌면 기대가 되는 날로 바뀔 수도 있어요.',
           'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'
       );

-- 6. 스토리보드와 토픽 연결
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES (
           '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
           'f70d7ebf-9058-4c6b-9afd-03bd23ea10d9'
       );

COMMIT;
