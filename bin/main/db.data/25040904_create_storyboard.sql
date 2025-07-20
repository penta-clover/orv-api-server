BEGIN;

-- 1) 스토리보드 생성
INSERT INTO public.storyboard (id, title, start_scene_id)
VALUES ('E1F399C7-293F-4215-A717-E0C9BECD6D9B',
        '유튜버와 나 그 사이 어딘가',
        '233DBF34-7F5D-47DB-84F6-0C846EEB0B6A' -- 첫 번째 질문 scene ID
       );

-- 2) 스토리보드 미리보기 예시 삽입
INSERT INTO public.storyboard_preview (storyboard_id, examples)
VALUES ('E1F399C7-293F-4215-A717-E0C9BECD6D9B',
        ARRAY [
            '유튜브를 하고 나서 부터 가장 달라진 점은 무엇인가요?',
            '유튜브를 운영하면서 가장 힘들었던 순간은 언제였나요?'
            ]);

-- 3) 9개의 QUESTION, 1개의 EPILOGUE, 1개의 END 씬 삽입
INSERT INTO public.scene (id, storyboard_id, name, scene_type, content)
VALUES
    -- 1
    ('233DBF34-7F5D-47DB-84F6-0C846EEB0B6A',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '가벼운 인사 한마디 부탁 드립니다.',
             'hint',
             '시간이 지난 후에 이 영상을 다시 보았을 때, ''나는 이런 사람이었구나''라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 함께 말해주셔도 좋아요.',
             'nextSceneId', '57CA99D7-55D6-4EB1-9102-8957C1275EE5',
             'isHiddenQuestion', false
     )),
    -- 2
    ('57CA99D7-55D6-4EB1-9102-8957C1275EE5',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '유튜브를 처음 시작하게 된 계기를 알려주세요?',
             'hint', '처음 유튜브 계정을 만들고 영상을 올려야겠다고 마음을 먹은 이유는 무엇인가요?',
             'nextSceneId', 'B247A878-A3C0-4788-9AF4-212E60795253',
             'isHiddenQuestion', false
     )),
    -- 3
    ('B247A878-A3C0-4788-9AF4-212E60795253',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '유튜브를 시작한 뒤 가장 달라진 점은 무엇인가요?',
             'hint', '유튜브를 시작한 후에 어떤 점이 달라졌는지 궁금해요. 가장 달라졌다고 생각하는 부분은 무엇인가요?',
             'nextSceneId', '4F5AE112-BC05-4359-88FA-8052508A9347',
             'isHiddenQuestion', false
     )),
    -- 4
    ('4F5AE112-BC05-4359-88FA-8052508A9347',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '@{name}님이 업로드한 영상 중 가장 아끼는 영상 하나를 소개해주세요.',
             'hint', '유튜브에 업로드한 영상 중 가장 아끼는 영상을 핸드폰으로 보여주세요. 그리고 어떤 내용인지 왜 가장 좋아하는 지 등을 함께 말해주세요.',
             'nextSceneId', '35D247A7-0B90-49BB-BEF5-379CD0AE8A22',
             'isHiddenQuestion', false
     )),
    -- 5
    ('35D247A7-0B90-49BB-BEF5-379CD0AE8A22',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '유튜버 @{name}님과 일반인으로서 @{name}님은 어떤 점이 다른가요?',
             'hint', '유튜브에서의 모습과 평소의 모습은 어떻게 다른지 알려주세요. 만약 차이가 별로 없다면 그 이유는 무엇일까요?',
             'nextSceneId', 'F7A7DF57-F03D-4FBB-8C33-868140FAE739',
             'isHiddenQuestion', false
     )),
    -- 6
    ('F7A7DF57-F03D-4FBB-8C33-868140FAE739',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '유튜브를 운영하면서 가장 힘들었던 순간은 언제였나요?',
             'hint', '유튜브를 운영하면서 가장 힘들었던 순간은 언제인지 알려주세요. 그리고 그때의 @{name}님은 어떻게 행동하셨나요?',
             'nextSceneId', 'D9FBDF32-4C15-4FB9-9624-22B178612C0E',
             'isHiddenQuestion', false
     )),
    -- 7
    ('D9FBDF32-4C15-4FB9-9624-22B178612C0E',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '유튜브를 운영하면서 가장 즐거웠던 순간도 알려주세요.',
             'hint', '반대로 유튜브를 운영하면서 가장 즐거웠던 순간은 언제였나요? 그 순간의 이유를 알려주세요.',
             'nextSceneId', '28785D13-A99F-453D-9ECE-84065CE7CE95',
             'isHiddenQuestion', false
     )),
    -- 8
    ('28785D13-A99F-453D-9ECE-84065CE7CE95',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '지금의 @{name}님은 셀프 인터뷰를 찍었던 시기와 비교해서 얼마나 달라졌나요?',
             'hint', '셀프 인터뷰를 찍었던 시기와 그때 당시 셀프 인터뷰를 하게 된 이유, 하면서 말했던 내용 등을 함께 말해주세요. 미리 그때의 영상을 보고 이야기해주시면 더 좋아요.',
             'nextSceneId', '741BE536-AD00-4F96-8600-54CAFFA28401',
             'isHiddenQuestion', false
     )),
    -- 9 (히든)
    ('741BE536-AD00-4F96-8600-54CAFFA28401',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'QUESTION',
     json_build_object(
             'question', '친구가 유튜브를 시작하겠다고 한다면 어떤 말을 해주실 건가요?',
             'hint', '가까운 지인이 유튜브를 시작하겠다고 말하는 상황이라면 @{name}님은 어떤 이야기를 해주실 건가요?',
             'nextSceneId', 'F7AAB9D7-BDFE-4FAC-9757-55A87A7E0CCE',
             'isHiddenQuestion', true
     )),
    -- EPILOGUE
    ('F7AAB9D7-BDFE-4FAC-9757-55A87A7E0CCE',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'EPILOGUE',
     json_build_object(
             'question', '아래 문구를 따라 읽어주세요',
             'hint', '2025년 3월 20일 오늘은 여기까지',
             'nextSceneId', '19DC9E42-BE65-4B9F-A2D3-1AA0243C06EB'
     )),
    -- END
    ('19DC9E42-BE65-4B9F-A2D3-1AA0243C06EB',
     'E1F399C7-293F-4215-A717-E0C9BECD6D9B',
     'scene_title', 'END',
     '{}'::json)
;

INSERT INTO public.topic (id, name, description, thumbnail_url)
VALUES ('f121be73-e9be-48dd-96fb-43f4b8c100c6',
        '유튜버와 나 그 사이 어딘가',
        '유튜브를 시작하며 겪은 변화와 그 속에서 발견한 진짜 모습을 돌아봐요. 기록을 통해 성장해온 당신의 이야기, 지금 그 시간 속으로 들어가볼게요.',
        'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png');


INSERT INTO public.storyboard_topic (storyboard_id, topic_id)
VALUES ('e1f399c7-293f-4215-a717-e0c9becd6d9b',
        'f121be73-e9be-48dd-96fb-43f4b8c100c6');


INSERT INTO public.category (id, code)
VALUES ('a113a440-1b76-4ac0-a265-31763ff2f08c', 'HIDDEN_3fEMQ1');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('a113a440-1b76-4ac0-a265-31763ff2f08c', 'f121be73-e9be-48dd-96fb-43f4b8c100c6');


INSERT INTO public.category (id, code)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', 'DEFAULT');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', '03240b15-f080-4a6f-beda-608ed5429249');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', '0cf8cfbc-23d0-488b-bb8a-c8459f8f5c58');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', '4d7035ea-3a4b-4c06-944c-eea954c17c1f');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', '0245582e-c29c-4834-a5af-9355e2032af9');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', 'eb64bdf7-c270-4bea-b84c-195d6a6e81d6');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', '9b4aefb3-1468-4b56-a9ec-a41ea0ff4e52');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', '63a1f2b5-1039-4f96-93a7-c30e5252bb01');

INSERT INTO public.category_topic (category_id, topic_id)
VALUES ('3d91d72d-f9b8-4725-b056-85a2a813c93f', 'f70d7ebf-9058-4c6b-9afd-03bd23ea10d9');

COMMIT;