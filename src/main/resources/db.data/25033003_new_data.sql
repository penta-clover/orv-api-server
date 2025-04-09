BEGIN;

-- 1. 새로운 스토리보드 생성
INSERT INTO storyboard (id, title, start_scene_id)
VALUES (
           '8c4359b2-c60a-4972-8327-89677244b12b',  -- 세상에서 가장 특별한 날 (1)
           '세상에서 가장 특별한 날 (1)',
           NULL
       );

-- 2. 스토리보드 미리보기(Preview) 생성
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES (
           '8c4359b2-c60a-4972-8327-89677244b12b',
           ARRAY [
               '나에게 생일은 어떤 의미인가요?',
           '어떤 선물이든 받을 수 있다면 나는 무슨 선물을 바랄까요?'
               ]
       );

-- 3. 스토리보드에 속하는 씬들 생성

-- scene1
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '68b101f5-39c4-4c14-b07c-2a2e68870bf5',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "가벼운 인사 한마디 부탁 드립니다.",
              "hint": "오늘 하루의 나, 나이, 이름 등을 함께 표현해주시면 좋아요. 특히 몇 번째 생일인지도 같이 설명해주세요!",
              "nextSceneId": "95da9f31-d3e8-45d9-83fa-09aec02ef79d"
           }'::json
       );

-- scene2
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '95da9f31-d3e8-45d9-83fa-09aec02ef79d',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "오늘이 생일이신데 소감이 어떠신가요?",
              "hint": "생일자로서 오늘의 소감을 말해주세요.",
              "nextSceneId": "f01abb71-5758-4381-b071-1bf5ecc618a0"
           }'::json
       );

-- scene3
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'f01abb71-5758-4381-b071-1bf5ecc618a0',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "나에게 생일은 어떤 의미인가요?",
              "hint": "지난 생일날들을 돌아보며 나에게 생일이란 어떤 의미인지 이야기해주세요.",
              "nextSceneId": "4765f971-f666-49de-b725-07639046499f"
           }'::json
       );

-- scene4
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '4765f971-f666-49de-b725-07639046499f',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "가장 이상적인 생일의 모습은 어떤 모습인가요?",
              "hint": "가장 원하고 바라는 생일의 모습이 있다면 알려주세요.",
              "nextSceneId": "8f12e2a6-4219-48f3-91dc-1b90c5759848"
           }'::json
       );

-- scene5
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '8f12e2a6-4219-48f3-91dc-1b90c5759848',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "어떤 선물이든 받을 수 있다면 나는 무슨 선물을 바랄까요?",
              "hint": "아무런 제한이 없다면 가장 받고 싶은 선물은 무엇인지 자유롭게 말씀해주세요.",
              "nextSceneId": "63c67607-b7fe-4146-8594-5c9c4a105f8b"
           }'::json
       );

-- scene6
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '63c67607-b7fe-4146-8594-5c9c4a105f8b',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "이번 생일에 가장 고마움을 느끼는 상대가 있나요?",
              "hint": "가장 잘 챙겨준 사람, 어떤 이유였고 어떤 사람인지, 그리고 그 사람의 생일에 해주고 싶은 것도 함께 이야기해주세요.",
              "nextSceneId": "8e6efbc7-c765-408c-b427-8d958bd8c755"
           }'::json
       );

-- scene7
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '8e6efbc7-c765-408c-b427-8d958bd8c755',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'QUESTION',
           '{
              "question": "스스로를 위한 생일 축하 노래를 불러주세요",
              "hint": "떠오르는 생일축하곡이 있다면 불러주세요. 없으면 핸드폰으로 틀어서 같이 불러주세요.",
              "nextSceneId": "e263b0fe-1c61-4e65-adfc-85cd19faedd1"
           }'::json
       );

-- scene8 (EPILOGUE)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'e263b0fe-1c61-4e65-adfc-85cd19faedd1',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'EPILOGUE',
           (
                   '{ ' ||
                   '   "question": "아래 문구를 따라 읽어주세요", ' ||
                   '   "hint": "\"2025년 3월 30일 내 생일은 여기까지\"", ' ||
                   '   "nextSceneId": "7cdf1924-5c0b-428c-8930-a89019574af0"' ||
                   '}'
           )::json

       );

-- scene9 (END)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '7cdf1924-5c0b-428c-8930-a89019574af0',
           '8c4359b2-c60a-4972-8327-89677244b12b',
           'scene_title',
           'END',
           '{}'::json
       );

-- 4. 스토리보드의 start_scene_id를 첫 씬으로 설정
UPDATE storyboard
SET start_scene_id = '68b101f5-39c4-4c14-b07c-2a2e68870bf5'
WHERE id = '8c4359b2-c60a-4972-8327-89677244b12b';

-- 5. 새 토픽 생성
INSERT INTO topic (id, name, description, thumbnail_url)
VALUES (
           '9b4aefb3-1468-4b56-a9ec-a41ea0ff4e52',
           '세상에서 가장 특별한 날',
           '이 세상에 태어나줘서 감사합니다. 오늘은 당신의 생일입니다. 다른 이는 지나치더라도 내 생일을 더 특별하게 만들어보세요.',
           'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'
       );

-- 6. 스토리보드와 토픽 연결
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES (
           '8c4359b2-c60a-4972-8327-89677244b12b',
           '9b4aefb3-1468-4b56-a9ec-a41ea0ff4e52'
       );

COMMIT;
