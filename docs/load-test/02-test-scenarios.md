# 2. 부하테스트 시나리오

> **[← 이전: 개요](01-overview.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 부하 분석 및 목표 설정 →](03-performance-targets.md)**

## 2.1 유저 시나리오 A (컨텐츠 생성형)

- **세션 지속 시간**: 8-11분 (Scene 개수별 차별화)
  - 6개 Scene: 약 8분
  - 8개 Scene: 약 10분  
  - 9개 Scene: 약 11분
- **유저 비율**: 50%
- **목표**: 인터뷰 영상 녹화 → 업로드 → 리캡 생성 → 영상 다운로드 전체 플로우 테스트

### 시나리오 상세 플로우

| 단계 | HTTP Method | Endpoint | 설명 | 대기시간 | 예상 응답시간 |
|------|-------------|----------|------|---------|--------------| 
| 1 | GET | `/api/v0/auth/login/test` | 테스트 전용 로그인 URL 조회 | - | < 200ms |
| 2 | GET | `/api/v0/auth/callback/test?code={testUserId}` | 테스트 전용 로그인 콜백 (test_user_{agent}_{process}_{thread} 형식) | - | < 200ms |
| 3 | GET | `/api/v0/topic/list` | 토픽 목록 조회 | 1초 | < 300ms |
| 4 | GET | `/api/v0/archive/videos/my` | 내 영상 목록 조회 | 0.5초 | < 500ms |
| 5 | GET | `/api/v0/storyboard/{id}/preview` | 스토리보드 미리보기 (4회 반복) | 각 2초 | < 400ms |
| 6 | GET | `/api/v0/storyboard/{id}` | 스토리보드 상세 정보 조회 | 1초 | < 400ms |
| 7 | GET | `/api/v0/storyboard/scene/{sceneId}` | Scene 상세 조회 (6-9회, 스토리보드별 다름) | 각 45-55초 (랜덤) | < 300ms |
| 8 | POST | `/api/v0/archive/recorded-video` | 7분 영상 업로드 (480p, ~5MB) | - | 3-10초 |
| 9 | POST | `/api/v0/reservation/recap/video` | 리캡 예약 생성 | 2초 | < 500ms |
| 10 | GET | `/api/v0/archive/video/{videoId}` | 영상 다운로드 | - | 5-20초 |

### 주요 특징

- **총 API 호출 횟수**: 19-26회 (Scene 개수에 따라 가변)
  - 6개 Scene: 19회 (로그인 2 + 조회 13 + 생성 2 + 다운로드 1)
  - 8개 Scene: 22회 (로그인 2 + 조회 16 + 생성 2 + 다운로드 1)  
  - 9개 Scene: 23회 (로그인 2 + 조회 17 + 생성 2 + 다운로드 1)
- **예상 총 소요시간**: 8-11분 (Scene 개수별 차별화)
- **파일 처리**: 영상 업로드(480p, ~5MB) 및 다운로드
- **병목 예상 지점**: 영상 업로드, 영상 다운로드, Scene 장시간 세션 유지

## 2.2 유저 시나리오 B (컨텐츠 소비형)

- **세션 지속 시간**: 5분
- **유저 비율**: 50%
- **목표**: 기존 리캡 결과 조회 및 오디오 청취

### 시나리오 상세 플로우

| 단계 | HTTP Method | Endpoint | 설명 | 대기시간 | 예상 응답시간 |
|------|-------------|----------|------|---------|--------------| 
| 1 | GET | `/api/v0/auth/callback/test` | 테스트 전용 로그인 | - | < 200ms |
| 2 | GET | `/api/v0/reservation/recap/{id}/result` | 리캡 결과 조회 | 2초 | < 500ms |
| 3 | GET | `/api/v0/reservation/recap/{id}/audio` | 오디오 정보 조회 | 1초 | < 300ms |
| 4 | GET | `{audioUrl}` | S3 오디오 스트리밍 (7분) | - | Progressive |

### 주요 특징

- **총 API 호출 횟수**: 3회 (ORV 서버) + 1회 (S3 직접)
- **예상 총 소요시간**: 3-5분 (오디오 청취 시간에 따라 가변)
- **스트리밍 특성**: HTTP Range Request를 통한 Progressive Download
- **병목 예상 지점**: 동시 오디오 스트리밍 시 S3 대역폭

## 2.3 시나리오 실행 패턴

### 사용자 행동 모델

```
시나리오 A 사용자:
- 4개의 다른 스토리보드 미리보기를 탐색
- Scene을 순차적으로 진행 (인터뷰 시뮬레이션, nextSceneId 체인 따라감)
- Scene 간 45-55초의 답변/사고 시간 (실제 인터뷰 패턴, 랜덤)
- 10분 세션 내 최대 11개 Scene 진행 가능
- 영상 업로드 후 즉시 리캡 요청
- 고유 사용자 ID: test_user_{agent}_{process}_{thread} 형식

시나리오 B 사용자:
- 이전에 생성된 리캡 결과 확인
- 오디오를 처음부터 끝까지 청취
- 중간에 일시정지/재개 가능 (Range Request)
```

### 스토리보드 선택 전략 (결정론적 분배)

**사용자 ID 기반 해시 분배**:
- **6개 Scene (10%)**: 연말정산 스토리보드
- **8개 Scene (70%)**: 월요병, 오늘하루, 생일, 회고, 여행 
- **9개 Scene (20%)**: 자기소개, 짝사랑

```groovy
// TestDataProvider.groovy 구현 예시
static def getStoryboardByUserId(int userId) {
    def hash = Math.abs(userId.hashCode()) % 100
    
    if (hash < 10) {
        return "9c570f84-16b6-4c5d-85b0-eadf05829056" // 연말정산 (6개)
    } else if (hash < 80) {
        def eightSceneIds = [
            "0afecfc8-62a4-4398-85a8-0cff8b8f698f", // 월요병
            "18779df7-a80d-497c-9206-9e61540bb465", // 오늘하루
            "8c4359b2-c60a-4972-8327-89677244b12b", // 생일
            "c81d9417-5797-4b11-a8ea-c161cacfe9d1", // 회고
            "e5e9b7dc-efa4-43f9-b428-03769aabdafc"  // 여행
        ]
        return eightSceneIds[(hash - 10) % eightSceneIds.size()]
    } else {
        def nineSceneIds = [
            "8c2746c4-4613-47f8-8799-235fec7f359d", // 자기소개
            "cff1c432-b6ac-4b10-89b7-3c9be91a6699"  // 짝사랑
        ]
        return nineSceneIds[(hash - 80) % nineSceneIds.size()]
    }
}
```

## 2.4 부하 시나리오 구성

### API 호출량 재계산 (6,000명 사용자 기준 1시간당)

**시나리오 A (3,000명, 50%)**:
- 6개 Scene: 300명 × 6 = 1,800회
- 8개 Scene: 2,100명 × 8 = 16,800회  
- 9개 Scene: 600명 × 9 = 5,400회
- **Scene 조회 총합**: 24,000회

**기타 API 호출 (시나리오 A)**:
- 스토리보드 미리보기: 3,000명 × 4회 = 12,000회
- 영상 업로드: 3,000회
- 리캡 예약: 3,000회
- 영상 다운로드: 3,000회

**시나리오 B (3,000명, 50%)**:
- 리캡 결과 조회: 3,000회
- 오디오 메타데이터: 3,000회
- 오디오 스트리밍: 3,000회

| API Endpoint | 시나리오 A | 시나리오 B | 총 호출수 |
|--------------|------------|------------|-----------| 
| Scene 조회 | 24,000회 | - | 24,000회 |
| 스토리보드 미리보기 | 12,000회 | - | 12,000회 |
| 영상 업로드 | 3,000회 | - | 3,000회 |
| 영상 다운로드 | 3,000회 | - | 3,000회 |
| 리캡 예약 | 3,000회 | - | 3,000회 |
| 리캡 결과 조회 | - | 3,000회 | 3,000회 |
| 오디오 메타데이터 | - | 3,000회 | 3,000회 |
| 오디오 스트리밍 | - | 3,000회 | 3,000회 |

### 테스트 데이터 요구사항

- **테스트 사용자**: 6,000명 (Provider: 'test', ID 형식: test_user_{agent}_{process}_{thread})
- **스토리보드**: 기존 8개 스토리보드 활용 (월요병, 오늘하루, 자기소개, 생일, 연말정산, 회고, 짝사랑, 여행)
  - 현재 구현된 미리보기 ID:
    - 189d11ae-c9bf-4ed5-8f55-40f004afa098
    - f2655dc4-8daa-40c6-853c-f01acf72b4ad
    - c0cf41b6-e6f6-4e40-b3da-b49e83a133d3
    - 8c2746c4-4613-47f8-8799-235fec7f359d (자기소개)
- **Scene**: 스토리보드별 6-9개 (실제 구조 반영), nextSceneId로 연결된 체인 구조
- **Scene 데이터 형태 (타입별 다름)**:
  - **QUESTION**: `{"question": "질문내용", "hint": "힌트", "nextSceneId": "UUID", "isHiddenQuestion": true/false}`
  - **EPILOGUE**: `{"question": "아래 문구를 따라 읽어주세요", "hint": "2025년 4월 1일 오늘은 여기까지", "nextSceneId": "UUID"}`
  - **END**: `{}` (빈 객체)
- **기존 리캡 데이터**: 사용자당 5-10개
- **테스트 영상**: 7분 분량 480p 영상 (~5MB)

### 에러 처리 시나리오

1. **영상 업로드 실패**: 재시도 3회, 실패 시 다음 단계 진행
2. **리캡 생성 타임아웃**: 60초 대기 후 실패 처리
3. **오디오 스트리밍 중단**: 재연결 시도 1회
4. **JWT 토큰 만료**: 자동 재로그인

## 📋 관련 문서

- **이전 단계**: [개요](01-overview.md)에서 테스트 목표 확인
- **다음 단계**: [부하 분석 및 목표 설정](03-performance-targets.md)에서 성능 목표 확인
- **구현 참조**: [테스트 데이터 생성 가이드](appendix-test-data-guide.md)에서 데이터 준비 방법 확인

---

**[← 이전: 개요](01-overview.md)** | **[메인으로 돌아가기](README.md)** | **[다음: 부하 분석 및 목표 설정 →](03-performance-targets.md)**
