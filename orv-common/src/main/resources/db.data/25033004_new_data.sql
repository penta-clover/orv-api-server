BEGIN;

-- 1. 스토리보드 생성
INSERT INTO storyboard (id, title, start_scene_id)
VALUES (
           '9c570f84-16b6-4c5d-85b0-eadf05829056',  -- 미래는 등 뒤에 있지 않다 (1)
           '미래는 등 뒤에 있지 않다 (1)',
           NULL
       );

-- 2. 스토리보드 미리보기 (Preview) 생성
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES (
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           ARRAY [
               '작년 2025년은 OO님에게 어떤 의미였나요? 혹은 어떤 한해로 기억에 남나요?',
           '올해의 나는 어떤 목표를 가지고 있나요?'
               ]
       );

-- 3. 스토리보드에 속하는 씬들 생성

-- scene1
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'f2655dc4-8daa-40c6-853c-f01acf72b4ad',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'QUESTION',
           '{
              "question": "가벼운 인사 한마디 부탁 드립니다.",
              "hint": "시간이 지난 후에 이 영상을 다시 보았을 때, ''나는 이런 사람이었구나''라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 추가하는 것을 추천 드려요.",
              "nextSceneId": "8ca108b2-a13e-49eb-b281-6e607696f5d1"
           }'::json
       );

-- scene2
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '8ca108b2-a13e-49eb-b281-6e607696f5d1',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'QUESTION',
           '{
              "question": "작년 2025년은 OO님에게 어떤 의미였나요? 혹은 어떤 일년으로 기억에 남나요?",
              "hint": "작년을 돌아보며 한해를 정리했을 때 어떤 의미인지, 가장 기억에 남는 것은 무엇인지 등을 말해주세요.",
              "nextSceneId": "e35adebc-66eb-4b98-b41a-1c041dc45842"
           }'::json
       );

-- scene3
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'e35adebc-66eb-4b98-b41a-1c041dc45842',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'QUESTION',
           '{
              "question": "작년 중에 특정 시점으로 돌아가서 다시 살 수 있다면 언제로 돌아가실 건가요?",
              "hint": "그 이유와 돌아간다면 어떤 것이 많이 바뀔 것 같은지도 함께 이야기해주세요.",
              "nextSceneId": "76e64310-b4ea-4ae1-b924-25303f2a3db3"
           }'::json
       );

-- scene4
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '76e64310-b4ea-4ae1-b924-25303f2a3db3',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'QUESTION',
           '{
              "question": "올해의 나는 어떤 목표를 가지고 있나요?",
              "hint": "올해가 끝나기 전까지 이루고 싶은 목표를 말해주세요.",
              "nextSceneId": "ac04db35-cdf9-4c61-9e53-df6661896dbf"
           }'::json
       );

-- scene5
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'ac04db35-cdf9-4c61-9e53-df6661896dbf',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'QUESTION',
           '{
              "question": "벌써 2025년의 1/4 정도가 지났는데 소감이 있으신가요?",
              "hint": "지금까지의 2025년이 만족스러운지, 어떻게 보냈는지 말씀해주세요.",
              "nextSceneId": "808e8e1c-50da-40b1-baa3-8872e6b5bb94"
           }'::json
       );

-- scene6
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '808e8e1c-50da-40b1-baa3-8872e6b5bb94',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'QUESTION',
           '{
              "question": "올해의 마지막 날 후회하지 않기 위해서는 어떻게 보낼 예정인가요?",
              "hint": "2025년이 마무리 되었을 때 후회하지 않도록 어떤 노력을 하고, 어떻게 보낼 것인지 말씀해주세요.",
              "nextSceneId": "d480e9e4-dc3b-414d-a905-08f39265dc1b"
           }'::json
       );

-- scene7 (EPILOGUE)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'd480e9e4-dc3b-414d-a905-08f39265dc1b',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'EPILOGUE',
           (
               '{ ' ||
               '   "question": "아래 문구를 따라 읽어주세요", ' ||
               '   "hint": "\"2025년 3월 30일 오늘은 여기까지\"", ' ||
               '   "nextSceneId": "18dec153-ed04-4671-a07a-ec0b54055a61"' ||
               '}'
           )::json
       );

-- scene8 (END)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '18dec153-ed04-4671-a07a-ec0b54055a61',
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           'scene_title',
           'END',
           '{}'::json
       );

-- 4. 스토리보드의 start_scene_id 설정
UPDATE storyboard
SET start_scene_id = 'f2655dc4-8daa-40c6-853c-f01acf72b4ad'
WHERE id = '9c570f84-16b6-4c5d-85b0-eadf05829056';

-- 5. 새 토픽 생성
INSERT INTO topic (id, name, description, thumbnail_url)
VALUES (
           '63a1f2b5-1039-4f96-93a7-c30e5252bb01',
           '미래는 등 뒤에 있지 않다',
           '과거는 남겨두고 앞으로 나아가야 할 때 뒤돌아보지 않도록 확실히 남겨두는 시간도 필요해요. 지난 해를 돌아보고 정리하면 머릿속이 맑아질 거예요.',
           'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'
       );

-- 6. 스토리보드와 토픽 연결
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES (
           '9c570f84-16b6-4c5d-85b0-eadf05829056',
           '63a1f2b5-1039-4f96-93a7-c30e5252bb01'
       );

COMMIT;
