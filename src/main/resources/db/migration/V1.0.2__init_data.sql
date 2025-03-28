BEGIN;

-- storyboard INSERT
INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('E5E9B7DC-EFA4-43F9-B428-03769AABDAFC', '여행의 특별한 순간을 기억하며 (1)', NULL);

INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('C81D9417-5797-4B11-A8EA-C161CACFE9D1', '흔들리지 않고 피는 꽃은 없다 (1)', NULL);

INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('CFF1C432-B6AC-4B10-89B7-3C9BE91A6699', '네가 나를 사랑하지 않아도, 너를 사랑하는 나를 사랑한다. (1)', NULL);

-- storyboard_preview INSERT
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES ('E5E9B7DC-EFA4-43F9-B428-03769AABDAFC', ARRAY [
    '이렇게 여행을 떠나게 된 계기는 무엇인가요?',
    '이번 여행을 하나의 문장으로 표현한다면 어떤 문장이 될 것 같나요?'
    ]);

INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES ('C81D9417-5797-4B11-A8EA-C161CACFE9D1', ARRAY [
    '올해가 마무리 되기까지 꼭 이루고 싶은 목표가 있나요?',
    '요즘 부단히 노력하고 있지만 어려움을 느끼는 일이 있나요?'
    ]);

INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES ('CFF1C432-B6AC-4B10-89B7-3C9BE91A6699', ARRAY [
    '당신의 이상형은 무엇인가요?',
    '내가 좋아하는 사람이 나를 좋아하지 않는다면 어떻게 하실 건가요?'
    ]);

-- storyboard 'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC' 관련 scene INSERT
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('189D11AE-C9BF-4ED5-8F55-40F004AFA098',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "가벼운 인사 한마디 부탁 드립니다.",' ||
                            '   "hint": "가볍게 나이 그리고 이름을 말씀해주세요. 해외에 계시다면 그 나라의 인삿말로 ‘안녕’이라고 해주세요.",' ||
                            '   "nextSceneId": "3A82B5A0-83A7-4C36-B0D1-84F56300E7A7"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('3A82B5A0-83A7-4C36-B0D1-84F56300E7A7',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "지금 있는 여행 장소와 나의 현재 상태를 설명해주세요.",' ||
                            '   "hint": "지금 계시는 구체적인 여행지를 소개하고, 여행 며칠 차인지, 컨디션, 오늘 입은 옷, 먹은 음식 및 상태 등을 설명해주세요.",' ||
                            '   "nextSceneId": "B53375A2-ABE9-4D6C-8994-BE751F294F3B"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('B53375A2-ABE9-4D6C-8994-BE751F294F3B',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "이렇게 여행을 떠나게 된 계기는 무엇인가요?",' ||
                            '   "hint": "이 여행을 떠나야겠다고 마음 먹은 순간은 언제였나요? 언제 이 여행을 계획했고 여행 장소나 기간을 결정하게 된 이유를 알려주세요.",' ||
                            '   "nextSceneId": "14FDD655-8938-4221-8186-5F59B9DA4173"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('14FDD655-8938-4221-8186-5F59B9DA4173',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "지금까지의 여행 일정 중 가장 기억에 남는 순간은 언제였나요?",' ||
                            '   "hint": "가장 밀도가 높은 감정을 느꼈던 순간이나 큰 깨달음을 얻었던 순간을 말해주세요. 꼭 그게 아니어도 이번 여행이 끝났을 때 가장 떠올리게 되는 순간을 구체적으로 말해주세요.",' ||
                            '   "nextSceneId": "7AD359CC-FE4D-49A3-B8C5-8F9256679029"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('7AD359CC-FE4D-49A3-B8C5-8F9256679029',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "이번 여행을 하나의 문장으로 표현한다면 어떤 문장이 될 것 같나요?",' ||
                            '   "hint": "지금의 시기, 상황, 이번 여행에서의 가장 기억에 남는 경험이나 에피소드 등과 함께 이유를 알려주세요.",' ||
                            '   "nextSceneId": "4E5B04BF-D9E0-45AB-B2A1-E8D3766676F5"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('4E5B04BF-D9E0-45AB-B2A1-E8D3766676F5',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "이 장소에 다시오게 된다면 그게 언제일 것 같나요? 그때의 나와 지금의 나는 어떻게 달라져 있을 것 같나요?",' ||
                            '   "hint": "다음에 이 여행지에 오게 될 예상 시기와 그 이유를 알려주세요. 그리고 그 시기의 나는 지금과 무엇이 다를지 생각해보고 알려주세요. 만약 다시 오지 않을 거라면 이유도 함께 설명해주세요.",' ||
                            '   "nextSceneId": "E722D781-866A-4566-97C2-0B8C2F350066"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('E722D781-866A-4566-97C2-0B8C2F350066',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "\"다음에는 OO(으)로 여행갈거야\"를 말해주세요.",' ||
                            '   "hint": "그 이유는 무엇이고 이번 여행과는 어떤 점이 다를 것 같은지 말해주세요. 꼭 그곳으로 새로운 여행을 떠나기를 바랍니다.",' ||
                            '   "nextSceneId": "25FA7E49-5E83-4B8D-A833-1C563114CA9B"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('25FA7E49-5E83-4B8D-A833-1C563114CA9B',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'EPILOGUE',
        (
                            '{ ' ||
                            '   "question": "아래 문구를 따라 읽어주세요",' ||
                            '   "hint": "2025년 3월 28일 오늘은 여기까지",' ||
                            '   "nextSceneId": "9386E4B8-BA17-49EB-8139-1A36131FEF73"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('9386E4B8-BA17-49EB-8139-1A36131FEF73',
        'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC',
        'scene_title',
        'END',
        '{}'::json);

-- storyboard 'C81D9417-5797-4B11-A8EA-C161CACFE9D1' 관련 scene INSERT
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('C0CF41B6-E6F6-4E40-B3DA-B49E83A133D3',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "가벼운 인사 한마디 부탁 드립니다.",' ||
                            E'   "hint": "시간이 지난 후에 이 영상을 다시 보았을 때, \\"나는 이런 사람이었구나\\"라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 추가하는 것을 추천 드려요.",' ||
                            '   "nextSceneId": "7035A40E-276B-4CF8-AAC1-BC1ECCAFA0B1"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('7035A40E-276B-4CF8-AAC1-BC1ECCAFA0B1',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "요즘 어떻게 지내고 있나요? 근황을 알려주세요.",' ||
                            '   "hint": "최근에 주로 하고 있는 일이나 시간을 많이 보내는 장소, 방문했던 장소나 새로한 경험 등 자유롭게 말씀 해주세요.",' ||
                            '   "nextSceneId": "0854CBC0-F04A-4A12-BAFC-51F059614E72"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('0854CBC0-F04A-4A12-BAFC-51F059614E72',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "올해가 마무리 되기까지 꼭 이루고 싶은 목표가 있나요?",' ||
                            '   "hint": "2025년이 끝났을 때 스스로가 생각하는 이상적인 모습을 상상하면서 말해주세요. 그리고 그 모습이 되기 위해 어떤 노력을 하실지도 함께 말씀해주세요.",' ||
                            '   "nextSceneId": "6D0A5602-5000-44CB-B3F8-914C911B451A"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('6D0A5602-5000-44CB-B3F8-914C911B451A',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "요즘 부단히 노력하고 있지만 어려움을 느끼는 일이 있나요?",' ||
                            '   "hint": "어떤 일이었나요? 왜 어려움을 느끼고 있고 어떻게 되기를 바라는지도 함께 이야기 해주세요.",' ||
                            '   "nextSceneId": "89D9E30E-42BD-48B2-913A-C802B25D1105"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('89D9E30E-42BD-48B2-913A-C802B25D1105',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "지금까지 여러 어려움을 견딜 수 있었던 이유는 무엇인가요?",' ||
                            '   "hint": "회피하고 싶고 도망치고 싶은 순간에도 그러지 않도록 해주던 힘은 어디서 온건가요?",' ||
                            '   "nextSceneId": "B834E349-3BC8-4053-8B56-2C291D913B85"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('B834E349-3BC8-4053-8B56-2C291D913B85',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "2025년 어느 날, 지쳐있는 나를 위해 한마디를 해주세요.",' ||
                            '   "hint": "그날의 내가 일어서기 위해선 어떤 말이 필요할까요?",' ||
                            '   "nextSceneId": "DF697C8A-572D-4C5C-8A68-63F66F77B3CE"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('DF697C8A-572D-4C5C-8A68-63F66F77B3CE',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "다음 인터뷰 때의 나는 어떤 모습일까요?",' ||
                            '   "hint": "다음 인터뷰 때 나는 오늘과는 어떻게 다른 모습일지, 혹은 어떤 모습이기를  바라는지 설명해주세요.",' ||
                            '   "nextSceneId": "07564719-63E7-4EC2-8E86-4EB2E5F2F978"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('07564719-63E7-4EC2-8E86-4EB2E5F2F978',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'EPILOGUE',
        (
                            '{ ' ||
                            '   "question": "아래 문구를 따라 읽어주세요",' ||
                            '   "hint": "\"2025년 3월 28일 오늘은 여기까지\"",' ||
                            '   "nextSceneId": "1BB17C6E-58F2-4903-97B1-28091DD66641"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('1BB17C6E-58F2-4903-97B1-28091DD66641',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'END',
        '{}'::json);


-- storyboard 'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699' 관련 scene INSERT
INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('093BD1A0-FFB7-4D46-87B3-D1E9A5379EC7',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "가벼운 인사 한마디 부탁 드립니다.",' ||
                            '   "hint": "시간이 지난 후에 이 영상을 다시 보았을 때, ''나는 이런 사람이었구나''라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 추가하는 것을 추천 드려요.",' ||
                            '   "nextSceneId": "692D2F53-8B9E-44A0-B247-FC54D42766F8"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('692D2F53-8B9E-44A0-B247-FC54D42766F8',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "당신의 이상형은 무엇인가요?",' ||
                            '   "hint": "외적/내적 이상형을 최대한 구체적으로 설명해주세요. 실제 존재하는 사람을 예시로 들거나 비유를 해도 좋아요.",' ||
                            '   "nextSceneId": "1241F56B-4E04-42F0-8772-18223CBF06DA"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('1241F56B-4E04-42F0-8772-18223CBF06DA',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "인생에서 사랑이라는 감정의 비중은 어느 정도인가요?",' ||
                            '   "hint": "누군가를 좋아하고 사랑하는 감정이 나에게 얼마나 큰 영향을 끼치는지, 그리고 중요하다고 생각하는지 알려주세요.",' ||
                            '   "nextSceneId": "F6F41857-0764-4127-A09B-BF685201CF05"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('F6F41857-0764-4127-A09B-BF685201CF05',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "내가 좋아하는 사람이 나를 좋아하지 않는다면 어떻게 하실 건가요?",' ||
                            '   "hint": "내가 좋아하는 누군가가 나를 좋아하지 않는 상황이라면 어떻게 행동하실 건가요? 혹은 그동안 어떻게 행동해오셨나요?",' ||
                            '   "nextSceneId": "FA4848DD-EBE4-4C6B-A323-FD353A2E2FD7"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('FA4848DD-EBE4-4C6B-A323-FD353A2E2FD7',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "이번 인터뷰를 진행하며 나의 머릿속에 떠오른 사람은 누구인가요?",' ||
                            '   "hint": "이번 인터뷰의 주제를 고르고 답변을 고민하면서 꾸준히 생각나는 사람이 누군지 알려주세요. 그리고 그 사람을 언제 처음 좋아하게 되고 왜 좋아했는지도 함께 설명해주세요. 만약 떠오르는 사람이 없다면 왜 없는지에 대한 자신의 이야기를 해주세요.",' ||
                            '   "nextSceneId": "16955530-A858-43D7-9A51-9401C89EA720"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('16955530-A858-43D7-9A51-9401C89EA720',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "누군가를 좋아하는 마음을 정리해야겠다는 결심을 했던 적이 있나요?",' ||
                            '   "hint": "좋아하는 마음을 정리했던 계기나 사건이 있다면 설명해주세요.",' ||
                            '   "nextSceneId": "29A8FDC2-EB1F-4EE1-9A31-BB87514F1649"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('29A8FDC2-EB1F-4EE1-9A31-BB87514F1649',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'QUESTION',
        (
                            '{ ' ||
                            '   "question": "짝사랑을 막 시작하려는 사람들에게 조언을 해준다면?",' ||
                            '   "hint": "어떤 조언을 해주실 것인가요? 그리고 그 조언의 마지막에 나 스스로를 위한 이야기도 추가해주세요.",' ||
                            '   "nextSceneId": "44DBFEBF-F52C-421F-843A-1643A3BBB2F6"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('44DBFEBF-F52C-421F-843A-1643A3BBB2F6',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'EPILOGUE',
        (
                            '{ ' ||
                            '   "question": "아래 문구를 따라 읽어주세요",' ||
                            '   "hint": "\"2025년 3월 28일 오늘은 여기까지\"",' ||
                            '   "nextSceneId": "F07CBB52-22D8-46F5-83A7-8C13558B75EC"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('F07CBB52-22D8-46F5-83A7-8C13558B75EC',
        'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699',
        'scene_title',
        'END',
        ('{}')::json);

-- UPDATE storyboard의 start_scene_id
UPDATE storyboard
SET start_scene_id = '189D11AE-C9BF-4ED5-8F55-40F004AFA098'
WHERE id = 'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC';

UPDATE storyboard
SET start_scene_id = 'C0CF41B6-E6F6-4E40-B3DA-B49E83A133D3'
WHERE id = 'C81D9417-5797-4B11-A8EA-C161CACFE9D1';

UPDATE storyboard
SET start_scene_id = '093BD1A0-FFB7-4D46-87B3-D1E9A5379EC7'
WHERE id = 'CFF1C432-B6AC-4B10-89B7-3C9BE91A6699';

-- topic INSERT
INSERT INTO topic (id, name, description, thumbnail_url)
VALUES ('03240B15-F080-4A6F-BEDA-608ED5429249',
        '여행의 특별한 순간을 기억하며',
        '지금까지 여행을 어떻게 기록해왔나요? 이번에는 여행자로서의 나 스스로를 인터뷰를 하며 새롭게 기록해보세요.',
        'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png');

INSERT INTO topic (id, name, description, thumbnail_url)
VALUES ('0CF8CFBC-23D0-488B-BB8A-C8459F8F5C58',
        '흔들리지 않고 피는 꽃은 없다',
        '세상에 쉬운 일은 없다고 하지만 유독 더 힘든 하루가 가끔 있어요. 이번 기회에 ‘나는 괜찮은지’ ‘어느 정도로 힘들어하고 있는지’를 스스로에게 물어봐주세요.',
        'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png');


INSERT INTO topic (id, name, description, thumbnail_url)
VALUES ('4D7035EA-3A4B-4C06-944C-EEA954C17C1F',
        '흔들리지 않고 피는 꽃은 없다',
        '사랑이라는 감정이 찾아온 나는 어떤 표정을 짓고 있을까요? 지금 이 주제를 읽으며 떠오르는 그 사람에 대한 내 마음을 확인해보세요.',
        'https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-topic-thumbnail.png');

-- storyboard_topic INSERT
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES ('E5E9B7DC-EFA4-43F9-B428-03769AABDAFC', '03240B15-F080-4A6F-BEDA-608ED5429249');

INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES ('C81D9417-5797-4B11-A8EA-C161CACFE9D1', '0CF8CFBC-23D0-488B-BB8A-C8459F8F5C58');

INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES ('CFF1C432-B6AC-4B10-89B7-3C9BE91A6699', '4D7035EA-3A4B-4C06-944C-EEA954C17C1F');

COMMIT;
