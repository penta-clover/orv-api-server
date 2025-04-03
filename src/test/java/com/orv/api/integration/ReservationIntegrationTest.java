package com.orv.api.integration;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orv.api.domain.auth.MemberRepository;
import com.orv.api.domain.auth.dto.Member;
import com.orv.api.domain.reservation.RecapRepository;
import com.orv.api.domain.reservation.ReservationNotificationService;
import com.orv.api.domain.reservation.ReservationService;
import com.orv.api.domain.reservation.dto.InterviewReservation;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Testcontainers;


@SpringBootTest
@Testcontainers
@Transactional
public class ReservationServiceRepositoryTest {

    @Autowired
    private ReservationService reservationService; // 테스트 대상 서비스

    @Autowired
    private MemberRepository memberRepository; // 실제 DB 연동을 위한 repository

    @Autowired
    private JdbcTemplate jdbcTemplate; // DB 직접 접근을 위한 JdbcTemplate

    // 예약 알림은 실제 전송하지 않도록 @MockBean 처리
    @MockitoBean
    private ReservationNotificationService notificationService;

    // Recap 관련 기능은 이 테스트에서는 단순 동작 확인을 위해 모킹
    @MockitoBean
    private RecapRepository recapRepository;

    private UUID testMemberId;
    private UUID testStoryboardId;

    @BeforeEach
    public void setUp() {
        // 테스트용 member 생성 (필요한 필드만 채워줍니다)
        testMemberId = UUID.randomUUID();
        Member member = new Member();
        member.setId(testMemberId);
        member.setNickname("TestUser");
        member.setProvider("test");
        member.setSocialId("testSocialId" + testMemberId);
        member.setPhoneNumber("01012345678");
        memberRepository.save(member);

        // storyboard는 외래키 제약조건 때문에 미리 삽입 (MemberRepository처럼 엔티티가 없으면 JdbcTemplate 사용)
        testStoryboardId = UUID.randomUUID();
        String insertStoryboardSql = "INSERT INTO storyboard (id, title) VALUES (?, ?)";
        jdbcTemplate.update(insertStoryboardSql, testStoryboardId, "Test Storyboard");
    }

    @Test
    public void testReserveInterview_success() throws Exception {
        // given
        ZonedDateTime scheduledAt = ZonedDateTime.now().plusDays(1);

        // when
        Optional<UUID> reservationIdOpt = reservationService.reserveInterview(testMemberId, testStoryboardId, scheduledAt);

        // then
        assertThat(reservationIdOpt)
                .as("예약 성공 시 생성된 예약 id가 반환되어야 함")
                .isPresent();
        UUID reservationId = reservationIdOpt.get();

        // DB에 interview_reservation 레코드가 실제 생성되었는지 확인
        String query = "SELECT count(*) FROM interview_reservation WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, reservationId);
        assertThat(count).isEqualTo(1);

        // member의 전화번호가 있을 경우 알림 전송을 시도하므로, notificationService가 호출되었는지 검증
        verify(notificationService, times(1))
                .notifyInterviewReservationConfirmed(eq("01012345678"), any(OffsetDateTime.class));
    }

    @Test
    public void testGetForwardInterviews() throws Exception {
        // given: 예약을 먼저 등록
        ZonedDateTime scheduledAt = ZonedDateTime.now().plusDays(1);
        Optional<UUID> reservationIdOpt = reservationService.reserveInterview(testMemberId, testStoryboardId, scheduledAt);
        assertThat(reservationIdOpt).isPresent();

        OffsetDateTime from = OffsetDateTime.now();

        // when
        Optional<List<InterviewReservation>> interviewsOpt = reservationService.getForwardInterviews(testMemberId, from);

        // then
        assertThat(interviewsOpt)
                .as("예약 조회에 성공하면 예약 목록이 반환되어야 함")
                .isPresent();
        List<InterviewReservation> interviews = interviewsOpt.get();
        assertThat(interviews).isNotEmpty();
        boolean found = interviews.stream().anyMatch(ir -> ir.getId().equals(reservationIdOpt.get()));
        assertThat(found).isTrue();
    }

    @Test
    public void testMarkInterviewAsDone() throws Exception {
        // given: 예약 생성
        ZonedDateTime scheduledAt = ZonedDateTime.now().plusDays(1);
        Optional<UUID> reservationIdOpt = reservationService.reserveInterview(testMemberId, testStoryboardId, scheduledAt);
        assertThat(reservationIdOpt).isPresent();
        UUID reservationId = reservationIdOpt.get();

        // when: 예약 상태를 'done'으로 변경
        boolean result = reservationService.markInterviewAsDone(reservationId);

        // then
        assertThat(result).as("상태 업데이트 성공 시 true를 반환해야 함").isTrue();

        // DB에서 실제로 상태가 변경되었는지 확인
        String query = "SELECT reservation_status FROM interview_reservation WHERE id = ?";
        String status = jdbcTemplate.queryForObject(query, String.class, reservationId);
        assertThat(status).isEqualTo("done");
    }

    @Test
    public void testReserveRecap() {
        // given: Recap 기능은 모킹 처리하고, 미리 동작 정의
        UUID videoId = UUID.randomUUID();
        ZonedDateTime scheduledAt = ZonedDateTime.now().plusDays(1);
        UUID expectedRecapId = UUID.randomUUID();
        when(recapRepository.reserveRecap(eq(testMemberId), eq(videoId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(expectedRecapId));

        // when
        Optional<UUID> result = reservationService.reserveRecap(testMemberId, videoId, scheduledAt);

        // then
        assertThat(result)
                .as("Recap 예약 성공 시 생성된 recap id를 반환해야 함")
                .isPresent()
                .hasValue(expectedRecapId);
    }
}