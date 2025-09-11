# 아키텍처 개선사항

## 🏛 현재 아키텍처의 문제점

### 1. Repository 패턴 불일치
- **Memory**, **Jdbc**, **S3** 구현체가 통일되지 않음
- 인터페이스와 구현체 분리가 일관성 없음
- 트랜잭션 경계가 불명확함

### 2. Service 계층 미흡
- 일부만 인터페이스 존재 (예: ReservationService)
- 비즈니스 로직이 Controller에 존재
- Service 간 의존성 관리 부재

### 3. 데이터 접근 계층
- JDBC Template 직접 사용으로 보일러플레이트 코드 과다
- N+1 쿼리 문제 해결 어려움
- 복잡한 연관관계 처리 미흡

## 🔧 개선 방안

### 1. Clean Architecture 적용

```
┌─────────────────────────────────────────────────────┐
│                   Presentation Layer                 │
│                  (Controllers, DTOs)                 │
├─────────────────────────────────────────────────────┤
│                   Application Layer                  │
│                  (Use Cases, Services)               │
├─────────────────────────────────────────────────────┤
│                    Domain Layer                      │
│               (Entities, Domain Services)            │
├─────────────────────────────────────────────────────┤
│                 Infrastructure Layer                 │
│            (Repositories, External Services)         │
└─────────────────────────────────────────────────────┘
```

### 2. Repository 패턴 정리

#### 현재 구조 (문제점)
```java
// 일관성 없는 구현
public class MemoryMemberRepository implements MemberRepository { }
public class JdbcMemberRepository implements MemberRepository { }
public class S3VideoRepository implements VideoRepository { } // 왜 S3?
```

#### 개선된 구조
```java
// 도메인 계층 - 순수한 인터페이스
package com.orv.api.domain.member.repository;
public interface MemberRepository {
    Optional<Member> findById(UUID id);
    Member save(Member member);
    void delete(Member member);
}

// 인프라 계층 - JPA 구현체
package com.orv.api.infrastructure.persistence.member;
@Repository
public class JpaMemberRepository implements MemberRepository {
    private final MemberJpaRepository jpaRepository;
    
    @Override
    public Optional<Member> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(MemberEntity::toDomain);
    }
}

// 파일 저장소는 별도 서비스로 분리
package com.orv.api.infrastructure.storage;
@Component
public class S3StorageService implements StorageService {
    public String uploadFile(InputStream file, StorageMetadata metadata);
    public InputStream downloadFile(String fileId);
}
```

### 3. Service 계층 리팩토링

#### 현재 문제
```java
// ArchiveController.java - 비즈니스 로직이 Controller에!
private Double calculateRunningTime(MultipartFile video) {
    // 복잡한 비즈니스 로직...
}
```

#### 개선 방안
```java
// 도메인 서비스 인터페이스
package com.orv.api.domain.archive.service;
public interface VideoService {
    VideoUploadResult uploadVideo(VideoUploadCommand command);
    Video findById(UUID videoId);
    void updateMetadata(UUID videoId, VideoMetadataUpdate update);
}

// 애플리케이션 서비스 구현
package com.orv.api.application.archive;
@Service
@Transactional
public class VideoServiceImpl implements VideoService {
    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoProcessor videoProcessor;
    
    @Override
    public VideoUploadResult uploadVideo(VideoUploadCommand command) {
        // 1. 검증
        videoValidator.validate(command.getFile());
        
        // 2. 비디오 처리
        VideoMetadata metadata = videoProcessor.extractMetadata(command.getFile());
        
        // 3. 저장
        String storageId = storageService.uploadFile(
            command.getFile().getInputStream(), 
            metadata
        );
        
        // 4. 메타데이터 저장
        Video video = Video.create(command, storageId, metadata);
        videoRepository.save(video);
        
        return VideoUploadResult.from(video);
    }
}

// 컨트롤러는 단순 위임만
@RestController
public class ArchiveController {
    private final VideoService videoService;
    
    @PostMapping("/recorded-video")
    public ApiResponse uploadRecordedVideo(
        @RequestParam("video") MultipartFile video,
        @RequestParam("storyboardId") String storyboardId
    ) {
        VideoUploadCommand command = new VideoUploadCommand(video, storyboardId);
        VideoUploadResult result = videoService.uploadVideo(command);
        return ApiResponse.success(result, 201);
    }
}
```

### 4. JPA/Hibernate 도입

#### 현재 JDBC Template 코드
```java
public Optional<Member> findById(UUID memberId) {
    String sql = "SELECT id, nickname, provider, social_id, email, " +
                 "profile_image_url, created_at, phone_number, birthday, " +
                 "gender, name FROM member WHERE id = ?";
    try {
        Member member = jdbcTemplate.queryForObject(sql, 
            new Object[]{memberId}, 
            new BeanPropertyRowMapper<>(Member.class));
        return Optional.of(member);
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty();
    }
}
```

#### JPA 도입 후
```java
@Entity
@Table(name = "member")
@Where(clause = "deleted_at IS NULL") // Soft Delete
public class MemberEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 8)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    private Provider provider;
    
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<VideoEntity> videos;
    
    // 도메인 모델로 변환
    public Member toDomain() {
        return Member.builder()
            .id(this.id)
            .nickname(this.nickname)
            .provider(this.provider)
            .build();
    }
}

@Repository
public interface MemberJpaRepository extends JpaRepository<MemberEntity, UUID> {
    Optional<MemberEntity> findByNickname(String nickname);
    
    @Query("SELECT m FROM MemberEntity m JOIN FETCH m.roles WHERE m.id = :id")
    Optional<MemberEntity> findByIdWithRoles(@Param("id") UUID id);
}
```

### 5. 도메인 모델 개선

#### 현재: Anemic Domain Model
```java
public class Member {
    private UUID id;
    private String nickname;
    // getter/setter만 존재
}
```

#### 개선: Rich Domain Model
```java
@Entity
public class Member {
    @Id
    private UUID id;
    
    @Embedded
    private Nickname nickname;
    
    @Embedded
    private PersonalInfo personalInfo;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberRole> roles = new ArrayList<>();
    
    // 비즈니스 로직 포함
    public void changeNickname(String newNickname) {
        validateNicknameChange(newNickname);
        this.nickname = new Nickname(newNickname);
        Events.raise(new NicknameChangedEvent(this.id, newNickname));
    }
    
    public void assignRole(Role role) {
        if (hasRole(role)) {
            throw new DuplicateRoleException();
        }
        this.roles.add(new MemberRole(this, role));
    }
    
    private void validateNicknameChange(String nickname) {
        if (!Nickname.isValid(nickname)) {
            throw new InvalidNicknameException();
        }
    }
}

// Value Object
@Embeddable
public class Nickname {
    private static final Pattern VALID_PATTERN = 
        Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]{1,8}$");
    
    @Column(name = "nickname")
    private String value;
    
    protected Nickname() {} // JPA용
    
    public Nickname(String value) {
        if (!isValid(value)) {
            throw new InvalidNicknameException();
        }
        this.value = value;
    }
    
    public static boolean isValid(String value) {
        return value != null && VALID_PATTERN.matcher(value).matches();
    }
}
```

### 6. 이벤트 기반 아키텍처

```java
// 도메인 이벤트
public class VideoUploadedEvent extends DomainEvent {
    private final UUID videoId;
    private final UUID memberId;
    private final long fileSize;
    
    // 이벤트 발행
    public VideoUploadedEvent(Video video) {
        this.videoId = video.getId();
        this.memberId = video.getOwnerId();
        this.fileSize = video.getFileSize();
    }
}

// 이벤트 리스너
@Component
@Transactional
public class VideoEventHandler {
    @EventListener
    @Async
    public void handleVideoUploaded(VideoUploadedEvent event) {
        // 썸네일 생성
        thumbnailService.generateThumbnail(event.getVideoId());
        
        // 통계 업데이트
        statisticsService.updateMemberUploadStats(event.getMemberId());
        
        // 알림 발송
        notificationService.notifyVideoProcessingStarted(event.getVideoId());
    }
}
```

## 📋 Action Items

### Phase 1: 기초 정리 (2주)
1. [ ] Repository 인터페이스 통일
2. [ ] Service 인터페이스 정의
3. [ ] Controller에서 비즈니스 로직 제거

### Phase 2: JPA 도입 (3주)
1. [ ] JPA 의존성 추가 및 설정
2. [ ] Entity 클래스 생성
3. [ ] Repository 구현체 JPA로 전환
4. [ ] 기존 JDBC 코드와 병행 운영

### Phase 3: 도메인 모델 강화 (2주)
1. [ ] Value Object 도입
2. [ ] Domain Service 구현
3. [ ] 도메인 이벤트 적용

### Phase 4: 고도화 (2주)
1. [ ] CQRS 패턴 적용 검토
2. [ ] Event Sourcing 도입 검토
3. [ ] 마이크로서비스 전환 준비

## 🎯 기대 효과

1. **유지보수성**: 명확한 계층 분리로 변경 영향 최소화
2. **테스트 용이성**: 각 계층별 독립적 테스트 가능
3. **확장성**: 새로운 기능 추가 시 기존 코드 영향 최소화
4. **성능**: JPA 최적화로 쿼리 성능 향상
5. **개발 생산성**: 보일러플레이트 코드 제거

---

← 이전: [보안 이슈 및 개선방안](./02-security-issues.md) | 다음: [코드 품질 개선](./04-code-quality.md) →
