BEGIN;

-- 1. 새 스토리보드 생성 (새 UUID 사용)
INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('8c2746c4-4613-47f8-8799-235fec7f359d', -- 나도 모르는 나 발견하기 (1)
        '나도 모르는 나 발견하기 (1)',
        NULL);

-- 2. 스토리보드 미리보기(Preview) 생성 (스토리보드 ID = 8c2746c4-4613-47f8-8799-235fec7f359d)
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES ('8c2746c4-4613-47f8-8799-235fec7f359d',
        ARRAY [
            '다른 사람들은 나를 어떤 사람이라고 느끼나요?',
            '핸드폰 갤러리의 첫번째 사진은 무엇인가요?'
            ]);

-- 3. 스토리보드에 속하는 씬들 생성
-- scene1
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('b0f68e09-0cc8-4269-bbf0-8a6c74566483', -- 새 UUID
        '8c2746c4-4613-47f8-8799-235fec7f359d', -- 스토리보드 ID
        'scene_title',
        'QUESTION',
        (
                 '{ ' ||
                 '   "question": "가벼운 인사 한마디 부탁 드립니다.",' ||
                 E'   "hint": "시간이 지난 후에 이 영상을 다시 보았을 때, \'나는 이런 사람이었구나\'라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 추가하는 것을 추천 드려요.", ' ||
                 '   "nextSceneId": "0a473113-6cf5-4a25-8322-a2f0b2665f7a"' ||
        '}'
               )::json
       );

-- scene2
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '0a473113-6cf5-4a25-8322-a2f0b2665f7a',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'QUESTION',
           (
                   '{ ' ||
                   '   "question": "가장 좋아하는 영화를 소개해주세요.", ' ||
                   '   "hint": "어떤 영화이고 가장 좋아하는 이유는 무엇인가요? 언제부터 좋아했는지 등 떠오르는 것을 다 말해주세요.", ' ||
                   '   "nextSceneId": "56a17b77-b82e-4d39-bae0-2b9ca04e08d4"' ||
                   '}'
               )::json
       );

-- scene3
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '56a17b77-b82e-4d39-bae0-2b9ca04e08d4',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'QUESTION',
           (
                   '{ ' ||
                   '   "question": "핸드폰 갤러리의 첫번째 사진은 무엇인가요?", ' ||
                 '   "hint": "핸드폰 갤러리를 열어 가장 첫번째에 있는 사진을 보여주면서 그 사진에 대해서 설명해주세요. 언제, 어디서 찍은 것인지 관련 추억이 있으면 더 좋아요.", ' ||
                   '   "nextSceneId": "7f8df752-845f-4b30-9781-b2bc44df5861"' ||
                   '}'
               )::json
       );

-- scene4
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '7f8df752-845f-4b30-9781-b2bc44df5861',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'QUESTION',
           (
                   '{ ' ||
                   '   "question": "다른 사람들은 나를 어떤 사람이라고 느끼나요?", ' ||
                   '   "hint": "친구, 가족, 지인 5명에게 ‘나는 어떤 사람인 것 같아?’라고 물어보고 알려주세요. 그리고 다른 사람이 보는 나는 내가 생각하는 나와 어떻게 달랐는지도 설명해주세요.", ' ||
                   '   "nextSceneId": "2e48d7f6-a3a5-4851-a0ba-c2ebf0eddfec"' ||
                   '}'
               )::json
       );

-- scene5
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '2e48d7f6-a3a5-4851-a0ba-c2ebf0eddfec',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'QUESTION',
           (
                   '{ ' ||
                   '   "question": "만약 주변 사람들의 시선이나 경제적인 제약이 없다면 당장 무엇을 할 건가요?", ' ||
                   '   "hint": "지금 정말 하고 싶은데 주변사람들의 시선이나 사회적 압박, 혹은 경제적인 여유가 없어 하지 못하고 있는 것은 무엇인지 말해주세요.", ' ||
                   '   "nextSceneId": "fb811764-017e-47e0-b3c3-5458975d2af9"' ||
                   '}'
               )::json
       );

-- scene6
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'fb811764-017e-47e0-b3c3-5458975d2af9',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'QUESTION',
           (
                   '{ ' ||
                   '   "question": "나의 유튜브 알고리즘에는 어떤 영상들이 뜨나요?", ' ||
                   '   "hint": "핸드폰으로 유튜브 앱을 열어 어떤 영상들이 뜨는지 보여주세요. 무엇이 많이 뜨는지, 공통점이 있는지 알려주세요.", ' ||
                   '   "nextSceneId": "8cda4c0c-10c4-4ee4-ab33-7a259a04b9f5"' ||
                   '}'
               )::json
       );

-- scene7
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '8cda4c0c-10c4-4ee4-ab33-7a259a04b9f5',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'QUESTION',
           (
                   '{ ' ||
                   '   "question": "지금까지 이야기한 내용과는 별개로 나는 어떤 사람이 되고 싶나요?", ' ||
                   '   "hint": "나는 나 스스로에게 어떤 사람이 되고 싶나요? 혹은 다른 사람이 보았으면 하는 모습이 있나요? 충분히 고민하고 말씀해주세요.", ' ||
                   '   "nextSceneId": "e2641ed3-bccc-4a6b-8b76-22849d6f71cc"' ||
                   '}'
               )::json
       );

-- scene8 (EPILOGUE)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           'e2641ed3-bccc-4a6b-8b76-22849d6f71cc',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'EPILOGUE',
           (
                   '{ ' ||
                   '   "question": "아래 문구를 따라 읽어주세요", ' ||
                   '   "hint": "\"2025년 3월 30일 오늘은 여기까지\"", ' ||
                   '   "nextSceneId": "41c46238-537d-4df7-8140-b98630876331"' ||
                   '}'
               )::json
       );

-- scene9 (END)
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES (
           '41c46238-537d-4df7-8140-b98630876331',   -- 새 UUID
           '8c2746c4-4613-47f8-8799-235fec7f359d',
           'scene_title',
           'END',
           '{}'::json
       );

-- 4. 스토리보드의 start_scene_id로 첫 씬의 id를 넣어주기
UPDATE storyboard
SET start_scene_id = 'b0f68e09-0cc8-4269-bbf0-8a6c74566483'
WHERE id = '8c2746c4-4613-47f8-8799-235fec7f359d';

-- 5. 토픽 생성 (새 UUID 사용)
INSERT INTO topic (id, name, description, thumbnail_url)
VALUES (
           '0245582e-c29c-4834-a5af-9355e2032af9',  -- 새 UUID
           '나도 모르는 나 발견하기',
           E'나는 나 스스로를 잘 알고 있다고 생각하지만 예상치 못한 순간에 낯선 나를 마주하고는 하죠. 나도 몰랐던 \'진짜 나\'를 찾아가는 특별한 시간을 가져보세요.',
           'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'
       );

-- 6. 토픽과 스토리보드를 연결 (storyboard_topic)
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES (
           '8c2746c4-4613-47f8-8799-235fec7f359d',  -- 스토리보드 ID
           '0245582e-c29c-4834-a5af-9355e2032af9'   -- 토픽 ID
       );

COMMIT;
