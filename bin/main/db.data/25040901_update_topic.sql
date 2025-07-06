BEGIN;

UPDATE topic AS t
SET name          = d.name,
    description   = d.description,
    thumbnail_url = d.thumbnail_url FROM (
    VALUES
        ('0245582e-c29c-4834-a5af-9355e2032af9'::uuid, '나도 모르는 나 발견하기', '나는 나 스스로를 잘 알고 있다고 생각하지만 예상치 못한 순간에 낯선 나를 마주하고는 하죠. 나도 몰랐던 ''진짜 나''를 찾아가는 특별한 시간을 가져보세요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('4d7035ea-3a4b-4c06-944c-eea954c17c1f'::uuid, '네가 나를 사랑하지 않아도, 너를 사랑하는 나를 사랑한다.', '사랑이라는 감정이 찾아온 나는 어떤 표정을 짓고 있을까요? 지금 이 주제를 읽으며 떠오르는 그 사람에 대한 내 마음을 확인해보세요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('03240b15-f080-4a6f-beda-608ed5429249'::uuid, '여행의 특별한 순간 남기기', '지금까지 여행을 어떻게 기록해왔나요? 이번에는 여행자로서의 나 스스로를 인터뷰를 하며 새롭게 기록해보세요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('0cf8cfbc-23d0-488b-bb8a-c8459f8f5c58'::uuid, '흔들리지 않고 피는 꽃은 없다', '세상에 쉬운 일은 없다고 하지만 유독 더 힘든 하루가 가끔 있어요. 이번 기회에 ‘나는 괜찮은지’ ‘어느 정도로 힘들어하고 있는지’를 스스로에게 물어봐주세요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('63a1f2b5-1039-4f96-93a7-c30e5252bb01'::uuid, '미래는 등 뒤에 있지 않다', '과거는 남겨두고 앞으로 나아가야 할 때 뒤돌아보지 않도록 확실히 남겨두는 시간도 필요해요. 지난 해를 돌아보고 정리하면 머릿속이 맑아질 거예요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('9b4aefb3-1468-4b56-a9ec-a41ea0ff4e52'::uuid, '세상에서 가장 특별한 날', '이 세상에 태어나줘서 감사합니다. 오늘은 당신의 생일입니다. 다른 이는 지나치더라도 내 생일을 더 특별하게 만들어보세요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('eb64bdf7-c270-4bea-b84c-195d6a6e81d6'::uuid, '하루의 계획은 아침에 달려있다', '하루를 잘 보내고 싶은 당신에게 추천드려요. 위대한 사람들 중 많은 분들이 하루가 아침에 달려있다고 말합니다. 오늘 아침은 오브와 함께 시작해봐요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png'),
        ('f70d7ebf-9058-4c6b-9afd-03bd23ea10d9'::uuid, '월요병을 물리치는 법', '유독 월요일에는 몸이 축축 늘어지고 더 피곤한 것 같아요. 이번 기회에 월요병에 대해서 함께 탐구해봐요. 어쩌면 기대가 되는 날로 바뀔 수도 있어요.', 'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png')
) AS d(id, name, description, thumbnail_url)
WHERE t.id = d.id;


COMMIT;