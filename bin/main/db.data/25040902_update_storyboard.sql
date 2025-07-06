BEGIN;

-- storyboard 테이블 업데이트: start_scene_id와 title 갱신
UPDATE public.storyboard AS s
SET
    start_scene_id = v.start_scene_id::uuid,
    title = v.title
FROM (
    VALUES
    ('e5e9b7dc-efa4-43f9-b428-03769aabdafc', '189d11ae-c9bf-4ed5-8f55-40f004afa098', '여행의 특별한 순간 남기기 (1)'),
    ('c81d9417-5797-4b11-a8ea-c161cacfe9d1', 'c0cf41b6-e6f6-4e40-b3da-b49e83a133d3', '흔들리지 않고 피는 꽃은 없다 (1)'),
    ('cff1c432-b6ac-4b10-89b7-3c9be91a6699', '093bd1a0-ffb7-4d46-87b3-d1e9a5379ec7', '네가 나를 사랑하지 않아도, 너를 사랑하는 나를 사랑한다. (1)'),
    ('8c2746c4-4613-47f8-8799-235fec7f359d', 'b0f68e09-0cc8-4269-bbf0-8a6c74566483', '나도 모르는 나 발견하기 (1)'),
    ('18779df7-a80d-497c-9206-9e61540bb465', '20fbca77-249f-4826-8c30-b1dc3af847c0', '하루의 계획은 아침에 달려있다 (1)'),
    ('8c4359b2-c60a-4972-8327-89677244b12b', '68b101f5-39c4-4c14-b07c-2a2e68870bf5', '세상에서 가장 특별한 날 (1)'),
    ('9c570f84-16b6-4c5d-85b0-eadf05829056', 'f2655dc4-8daa-40c6-853c-f01acf72b4ad', '미래는 등 뒤에 있지 않다 (1)'),
    ('0afecfc8-62a4-4398-85a8-0cff8b8f698f', '420d3f95-037d-4c15-b8bb-4e5c8fcb2f92', '월요병을 물리치는 법 (1)')
    ) AS v(id, start_scene_id, title)
WHERE s.id = v.id::uuid;

-- storyboard_preview 테이블 업데이트: examples 컬럼 갱신 (텍스트 배열 형식)
UPDATE public.storyboard_preview AS sp
SET examples = v.arr
    FROM (
    VALUES
        ('e5e9b7dc-efa4-43f9-b428-03769aabdafc', ARRAY[
            '이렇게 여행을 떠나게 된 계기는 무엇인가요?',
            '이번 여행을 하나의 문장으로 표현한다면 어떤 문장이 될 것 같나요?'
        ]),
        ('c81d9417-5797-4b11-a8ea-c161cacfe9d1', ARRAY[
            '올해가 마무리 되기까지 꼭 이루고 싶은 목표가 있나요?',
            '요즘 부단히 노력하고 있지만 어려움을 느끼는 일이 있나요?'
        ]),
        ('cff1c432-b6ac-4b10-89b7-3c9be91a6699', ARRAY[
            '당신의 이상형은 무엇인가요?',
            '내가 좋아하는 사람이 나를 좋아하지 않는다면 어떻게 하실 건가요?'
        ]),
        ('8c2746c4-4613-47f8-8799-235fec7f359d', ARRAY[
            '다른 사람들은 나를 어떤 사람이라고 느끼나요?',
            '핸드폰 갤러리의 첫번째 사진은 무엇인가요?'
        ]),
        ('18779df7-a80d-497c-9206-9e61540bb465', ARRAY[
            '오늘 중요한 일정이 있다면 3가지만 말해주세요.',
            '하루 일과가 끝나고 누웠을 때 어떤 모습이기를 원하나요?'
        ]),
        ('8c4359b2-c60a-4972-8327-89677244b12b', ARRAY[
            '나에게 생일은 어떤 의미인가요?',
            '어떤 선물이든 받을 수 있다면 나는 무슨 선물을 바랄까요?'
        ]),
        ('9c570f84-16b6-4c5d-85b0-eadf05829056', ARRAY[
            '작년 2024년은 @{name}님에게 어떤 의미였나요? 혹은 어떤 한해로 기억에 남나요?',
            '올해의 나는 어떤 목표를 가지고 있나요?'
        ]),
        ('0afecfc8-62a4-4398-85a8-0cff8b8f698f', ARRAY[
            '월요병이라는 말에 동의하시나요?',
            '돌아오는 주말은 어떻게 시간을 보낼 계획인가요?'
        ])
) AS v(id, arr)
WHERE sp.storyboard_id = v.id::uuid;

COMMIT;
