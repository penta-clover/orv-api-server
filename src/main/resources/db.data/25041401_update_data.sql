BEGIN;

UPDATE topic AS t
SET name          = d.name,
    description   = d.description,
    thumbnail_url = d.thumbnail_url FROM (
    VALUES
        ('4d7035ea-3a4b-4c06-944c-eea954c17c1f'::uuid, '설렘은 그렇게 조용히 찾아왔다', '사랑이라는 감정이 찾아온 나는 어떤 표정을 짓고 있을까요? 지금 이 주제를 읽으며 떠오르는 그 사람에 대한 내 마음을 확인해보세요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('63a1f2b5-1039-4f96-93a7-c30e5252bb01'::uuid, '미래는 등 뒤에 있지 않다', '과거는 뒤로하고 앞으로 나아가야 할 때 가장 필요한 것은 미련을 확실히 정리하는 일이라고 생각합니다. 지난 해를 돌아보고 정리하면 머릿속이 맑아질 거예요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png')
) AS d(id, name, description, thumbnail_url)
WHERE t.id = d.id;


UPDATE public.storyboard AS s
SET start_scene_id = v.start_scene_id::uuid,
    title = v.title
FROM (
    VALUES
    ('cff1c432-b6ac-4b10-89b7-3c9be91a6699', '093bd1a0-ffb7-4d46-87b3-d1e9a5379ec7', '설렘은 그렇게 조용히 찾아왔다. (1)')
    ) AS v(id, start_scene_id, title)
WHERE s.id = v.id::uuid;



UPDATE public.scene AS s
SET name          = d.name,
    storyboard_id = d.storyboard_id::uuid,
    scene_type    = d.scene_type,
    content       =
        CASE
            WHEN d.scene_type = 'END' THEN
                -- 1) END → 빈 JSON 객체
                '{}'::json

            WHEN d.scene_type = 'EPILOGUE' THEN
                -- 2) EPILOGUE → question, hint, nextSceneId 모두 포함
                row_to_json(
                        (SELECT r
                         FROM (SELECT d.question    AS "question",
                                      d.hint        AS "hint",
                                      d.nextSceneId AS "nextSceneId") r)
                )::json

            WHEN d.scene_type = 'QUESTION' THEN
                -- 3) QUESTION → question, hint, nextSceneId, isHiddenQuestion 모두 포함
                row_to_json(
                        (SELECT r
                         FROM (SELECT d.question               AS "question",
                                      d.hint                   AS "hint",
                                      d.nextSceneId            AS "nextSceneId",
                                      d.isHiddenQuestion::bool AS "isHiddenQuestion") r)
                )::json

            ELSE
                -- 추가적인 scene_type 처리 (필요 시 수정)
                '{}'::json
END
FROM (VALUES ('scene_title',
              '6c89db11-5c60-4fac-a22e-815647f8ceb1',
              '37cf3e73-2cba-456b-b3df-5a6abfd7867c',
              '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
              'QUESTION',
              '돌아오는 주말은 어떻게 시간을 보낼 계획인가요?',
              '평일을 열심히 보낸 후 주말에는 어떻게 시간을 보낼지 들려주세요.',
              'TRUE'),
             ('scene_title',
              '964a9dda-dbf3-436e-b3ca-eb90b8ad8df0',
              '432b9dd4-3a66-4239-bdc9-0874c5f38d7c',
              '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
              'QUESTION',
              '기분 좋은 월요일을 만들기 위해 한가지 이벤트를 떠올려보세요.',
              '월요일에 내가 좋아하는 곳을 가거나, 맛있는 것을 사먹거나 등등 다 좋아요. 나를 위한 한가지 약속을 말해주세요.',
              'FALSE'),
              ('scene_title',
              '37cf3e73-2cba-456b-b3df-5a6abfd7867c',
              '964a9dda-dbf3-436e-b3ca-eb90b8ad8df0',
              '0afecfc8-62a4-4398-85a8-0cff8b8f698f',
              'QUESTION',
              '어떤 순간이 오면 더 이상 월요일이 싫지 않을까요?',
              '월요일이 더 이상 오기 싫은 날이 아니게 되는 순간. 그 순간은 어떤 상황이 와야 가능할까요?',
              'FALSE'),
             ('scene_title',
              'f01abb71-5758-4381-b071-1bf5ecc618a0',
              '4765f971-f666-49de-b725-07639046499f',
              '8c4359b2-c60a-4972-8327-89677244b12b',
              'QUESTION',
              '@{name}님에게 생일은 어떤 의미인가요?',
              '지난 생일날들을 돌아보며 나에게 생일이란 어떤 의미인지 이야기해주세요.',
              'FALSE'),
             ('scene_title',
              '8f12e2a6-4219-48f3-91dc-1b90c5759848',
              '63c67607-b7fe-4146-8594-5c9c4a105f8b',
              '8c4359b2-c60a-4972-8327-89677244b12b',
              'QUESTION',
              '어떤 선물이든 받을 수 있다면 @{name}님은 무슨 선물을 바랄까요?',
              '아무런 제한이 없다면 가장 받고 싶은 선물은 무엇인지 자유롭게 말씀해주세요.',
              'FALSE'),
             ('scene_title',
              'fa4848dd-ebe4-4c6b-a323-fd353a2e2fd7',
              '16955530-A858-43D7-9A51-9401C89EA720',
              'cff1c432-b6ac-4b10-89b7-3c9be91a6699',
              'QUESTION',
              '이번 인터뷰를 진행하며 @{name}님의 머릿속에 떠오른 사람은 누구인가요?',
              '이번 인터뷰의 답변을 고민하며 꾸준히 생각나는 사람이 누군지 알려주세요. 언제 그 사람을 처음 좋아했고 왜 좋아하게 됐나요? 만약 떠오르는 사람이 없다면 그 이유를 알려주세요.',
              'TRUE'),
             ('scene_title',
              '16955530-a858-43d7-9a51-9401c89ea720',
              '29A8FDC2-EB1F-4EE1-9A31-BB87514F1649',
              'cff1c432-b6ac-4b10-89b7-3c9be91a6699',
              'QUESTION',
              '그 사람과 당신은 지금 어떤 관계인가요?',
              '그리고 어떤 관계이기를 바라나요? 이를 위해 나는 어떤 노력을 할건가요?',
              'FALSE')) AS d(name, id, nextSceneId, storyboard_id, scene_type, question, hint, isHiddenQuestion)
WHERE s.id = d.id::uuid;

COMMIT;