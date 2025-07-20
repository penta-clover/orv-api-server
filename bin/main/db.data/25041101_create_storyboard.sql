BEGIN;

-- 1) 스토리보드 생성
INSERT INTO public.storyboard (id, title, start_scene_id)
VALUES ('B1F399C7-293F-4215-A717-E0C9BECD6D9B',
        '창업을 꿈꾸는 당신에게',
        'B33DBF34-7F5D-47DB-84F6-0C846EEB0B6A' -- 첫 번째 질문 scene ID
       );

-- 2) 스토리보드 미리보기 예시 삽입
INSERT INTO public.storyboard_preview (storyboard_id, examples)
VALUES ('B1F399C7-293F-4215-A717-E0C9BECD6D9B',
        ARRAY [
            '처음 창업에 관심을 갖게 된 계기가 무엇인가요?',
            '이번 HySpark 3기가 끝날 때 @{name}님은 어떤 모습일 것 같나요?'
            ]);

-- 3) 9개의 QUESTION, 1개의 EPILOGUE, 1개의 END 씬 삽입
INSERT INTO public.scene (id, storyboard_id, name, scene_type, content)
VALUES
    -- 1
    ('B33DBF34-7F5D-47DB-84F6-0C846EEB0B6A',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '가벼운 인사 한마디 부탁 드립니다.',
             'hint',
             '시간이 지난 후에 이 영상을 다시 보았을 때, ''나는 이런 사람이었구나''라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 함께 말해주셔도 좋아요.',
             'nextSceneId', 'B7CA99D7-55D6-4EB1-9102-8957C1275EE5',
             'isHiddenQuestion', false
     )),
    -- 2
    ('B7CA99D7-55D6-4EB1-9102-8957C1275EE5',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '@{name}님은 왜 HySpark에 들어 오려고 했나요?',
             'hint', '들어오기 전 무엇을 기대했고 어떤 마음으로 지원했었나요?',
             'nextSceneId', 'C247A878-A3C0-4788-9AF4-212E60795253',
             'isHiddenQuestion', false
     )),
    -- 3
    ('C247A878-A3C0-4788-9AF4-212E60795253',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '처음 창업에 관심을 갖게 된 계기가 무엇인가요?',
             'hint', '@{name}님이 생각하시는 창업이 무엇이고 관심을 가지게 된 이유를 알려주세요.',
             'nextSceneId', 'BF5AE112-BC05-4359-88FA-8052508A9347',
             'isHiddenQuestion', false
     )),
    -- 4
    ('BF5AE112-BC05-4359-88FA-8052508A9347',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '지금까지의 HySpark 활동 중 가장 기억에 남는 활동은 무엇인가요?',
             'hint', '가장 기억에 남는 활동과 그 이유를 구체적으로 말해주세요.',
             'nextSceneId', 'B5D247A7-0B90-49BB-BEF5-379CD0AE8A22',
             'isHiddenQuestion', false
     )),
    -- 5
    ('B5D247A7-0B90-49BB-BEF5-379CD0AE8A22',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '4기 학회원들에게 10만원톤에 대한 조언을 해준다면 어떤 이야기를 하실 건가요?',
             'hint', '10만원톤을 했던 기억 속에서 어떤 점을 조언할 것 같은지 말해주세요. 만약 다시 10만원톤을 한다면 어떻게 하실 것인지 생각해보고 알려주셔도 좋아요.',
             'nextSceneId', 'B7A7DF57-F03D-4FBB-8C33-868140FAE739',
             'isHiddenQuestion', false
     )),
    -- 6
    ('B7A7DF57-F03D-4FBB-8C33-868140FAE739',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '3기 학회원 1명과 함께 창업을 해야 한다면 누구랑 함께 할 건가요?',
             'hint', '바로 떠오르는 그 학회원이 누구인지와 그 이유를 알려주세요.',
             'nextSceneId', 'B9FBDF32-4C15-4FB9-9624-22B178612C0E',
             'isHiddenQuestion', true
     )),
    -- 7
    ('B9FBDF32-4C15-4FB9-9624-22B178612C0E',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '이번 HySpark 3기가 끝날 때 @{name}은 어떤 모습일 것 같나요?',
             'hint', '이번 학회가 끝날 무렵 @{name}님은 어떤 상태일지 상상해서 말씀해주세요. 하이스파크를 시작하기 전과 지금의 모습은 어떻게 달라졌고,  앞으로는 또 어떻게 변화할지를 함께 말해주시면 좋아요. ',
             'nextSceneId', 'B8785D13-A99F-453D-9ECE-84065CE7CE95',
             'isHiddenQuestion', false
     )),
    -- 8
    ('B8785D13-A99F-453D-9ECE-84065CE7CE95',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '@{name}님은 본인이 창업과 잘 맞는다고 생각하시나요?',
             'hint', '@{name}님이 창업과 본인이 잘 맞는다고 생각한 이유는 무엇인가요? 만약 그렇지 않다면 어떤 점 때문에 창업과 잘 맞지 않다고 생각하신 건가요?',
             'nextSceneId', 'B41BE536-AD00-4F96-8600-54CAFFA28401',
             'isHiddenQuestion', false
     )),
    -- 9 (히든)
    ('B41BE536-AD00-4F96-8600-54CAFFA28401',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', 'HySpark에서 어떤 사람으로 기억되고 싶나요?',
             'hint', '함께 하는 학회원들에게 혹은 운영진에게 @{name}님은 어떤 사람으로 기억에 남고 싶은지 알려주세요.',
             'nextSceneId', 'B7AAB9D7-BDFE-4FAC-9757-55A87A7E0CCE',
             'isHiddenQuestion', true
     )),
    -- EPILOGUE
    ('B7AAB9D7-BDFE-4FAC-9757-55A87A7E0CCE',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'EPILOGUE',
     json_build_object(
             'question', '아래 문구를 따라 읽어주세요',
             'hint', '2025년 3월 20일 오늘은 여기까지',
             'nextSceneId', 'B9DC9E42-BE65-4B9F-A2D3-1AA0243C06EB'
     )),
    -- END
    ('B9DC9E42-BE65-4B9F-A2D3-1AA0243C06EB',
     'B1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'END',
     '{}'::json)
;

INSERT INTO public.topic (id, name, description, thumbnail_url)
VALUES ('B121be73-e9be-48dd-96fb-43f4b8c100c6',
        '창업을 꿈꾸는 당신에게',
        '하이스파크 3기로 함께 한지 2달이 지났습니다. 처음 하이스파크를 지원했던 나와 지금의 나는 얼마나 달라졌나요? 그 여정 속에서 내 안의 변화를 마주해보세요.',
        'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png');


INSERT INTO public.storyboard_topic (storyboard_id, topic_id)
VALUES ('B1f399c7-293f-4215-a717-e0c9becd6d9b',
        'B121be73-e9be-48dd-96fb-43f4b8c100c6');


INSERT INTO public.category (id, code)
VALUES ('B113a440-1b76-4ac0-a265-31763ff2f08c', 'HIDDEN_3fEMQ2');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('B113a440-1b76-4ac0-a265-31763ff2f08c', 'B121be73-e9be-48dd-96fb-43f4b8c100c6');

COMMIT;