BEGIN;

-- storyboard INSERT
INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('E5E9B7DC-EFA4-43F9-B428-03769AABDAFC', '여행의 특별한 순간을 기억하며 (1)', NULL);

INSERT INTO storyboard (id, title, start_scene_id)
VALUES ('C81D9417-5797-4B11-A8EA-C161CACFE9D1', '흔들리지 않고 피는 꽃은 없다 (1)', NULL);

-- storyboard_preview INSERT
INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES ('E5E9B7DC-EFA4-43F9-B428-03769AABDAFC', ARRAY [
    '지금까지의 여행 일정 중 가장 기억에 남는 순간은 언제였나요?',
    '이번 여행을 하나의 문장으로 표현한다면 어떤 문장이 될 것 같나요?'
    ]);

INSERT INTO storyboard_preview (storyboard_id, examples)
VALUES ('C81D9417-5797-4B11-A8EA-C161CACFE9D1', ARRAY [
    '올해가 마무리 되었을 때, 나는 어떤 모습이기를 바라나요?',
    '2025년 어느 날, 지쳐있는 나를 위해 한마디를 해주세요.'
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
                            '   "hint": "이 여행을 떠나야겠다고 마음 먹은 순간과, 여행 계획, 장소 및 기간을 결정하게 된 이유를 알려주세요.",' ||
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
                            '   "hint": "가장 강렬한 감정이나 큰 깨달음을 얻은 순간, 또는 여행이 끝났을 때 가장 떠올릴 수 있는 순간을 구체적으로 말씀해주세요.",' ||
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
                            '   "hint": "현재 시기, 상황, 그리고 이번 여행에서의 기억에 남는 경험이나 에피소드와 그 이유를 함께 설명해주세요.",' ||
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
                            '   "hint": "다음 방문 예상 시기와 그 이유, 그리고 그 시기의 자신과 현재 자신이 어떻게 달라질지 혹은 다시 오지 않을 이유를 설명해주세요.",' ||
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
                            '   "hint": "여행 이유와 이번 여행과의 차이점, 그리고 그곳으로 떠나고 싶은 이유를 말씀해주세요.",' ||
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
                            '   "hint": "2025년 3월 20일 오늘은 여기까지",' ||
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
                            '   "hint": "시간이 지난 후에 이 영상을 다시 보았을 때, &#39;나는 이런 사람이었구나&#39;라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 추가하는 것을 추천 드려요.",' ||
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
                            '   "question": "올해가 마무리 되었을 때, 나는 어떤 모습이기를 바라나요?",' ||
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
                            '   "question": "나 스스로에게 칭찬 한마디 해주세요.",' ||
                            '   "hint": "사소한 내용이어도 혹은 특별한 내용이어도 좋아요. 지난 시간의 나를 돌아보며 마구마구 칭찬해주세요.",' ||
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
                            '   "question": "아래 문구를 따라 읽어주세요<br/>&#34;{date} 오늘은 여기까지&#34;",' ||
                            '   "hint": "가장 예쁜 꽃은 우여곡절 끝에 피는 꽃이라고 합니다. OO님도 예쁜 꽃을 피우기를 바라며 오늘 인터뷰를 마치겠습니다.",' ||
                            '   "nextSceneId": "1BB17C6E-58F2-4903-97B1-28091DD66641"' ||
                            '}'
            )::json);

INSERT INTO scene (id, storyboard_id, name, scene_type, content)
VALUES ('1BB17C6E-58F2-4903-97B1-28091DD66641',
        'C81D9417-5797-4B11-A8EA-C161CACFE9D1',
        'scene_title',
        'END',
        '{}'::json);

-- UPDATE storyboard의 start_scene_id
UPDATE storyboard
SET start_scene_id = '189D11AE-C9BF-4ED5-8F55-40F004AFA098'
WHERE id = 'E5E9B7DC-EFA4-43F9-B428-03769AABDAFC';

UPDATE storyboard
SET start_scene_id = 'C0CF41B6-E6F6-4E40-B3DA-B49E83A133D3'
WHERE id = 'C81D9417-5797-4B11-A8EA-C161CACFE9D1';

-- topic INSERT
INSERT INTO topic (id, name, description)
VALUES ('03240B15-F080-4A6F-BEDA-608ED5429249',
        '여행의 특별한 순간을 기억하며',
        '주제설명주세설명주제설명주세설명주제설명주세설명');

INSERT INTO topic (id, name, description)
VALUES ('0CF8CFBC-23D0-488B-BB8A-C8459F8F5C58',
        '흔들리지 않고 피는 꽃은 없다',
        '주제설명주세설명주제설명주세설명주제설명주세설명');

-- storyboard_topic INSERT
INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES ('E5E9B7DC-EFA4-43F9-B428-03769AABDAFC', '03240B15-F080-4A6F-BEDA-608ED5429249');

INSERT INTO storyboard_topic (storyboard_id, topic_id)
VALUES ('C81D9417-5797-4B11-A8EA-C161CACFE9D1', '0CF8CFBC-23D0-488B-BB8A-C8459F8F5C58');

COMMIT;
