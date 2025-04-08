package com.orv.api.domain.reservation.jobs;

import com.orv.api.domain.reservation.dto.AlimtalkButton;
import com.orv.api.domain.reservation.dto.AlimtalkContent;
import com.orv.api.global.bizgo.AlimtalkService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InterviewReservationPreviewAlimtalkJob implements Job {
    @Autowired
    private AlimtalkService alimtalkService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        try {
            AlimtalkContent alimtalkContent = new AlimtalkContent();
            alimtalkContent.setMsgType("AI");
            alimtalkContent.setTo(jobDataMap.getString("phoneNumber"));
            alimtalkContent.setTemplateCode("orv-request-preview-v1.2");
            alimtalkContent.setText("[오브] 예정된 인터뷰에서 " + jobDataMap.getString("name") + "님이 받을 질문을 미리 안내 드려요.\n" +
                    "\n" +
                    "◼ " + jobDataMap.getString("date") + "\n" +
                    "◼ " + jobDataMap.getString("title") + "\n" +
                    "◼ 질문 " + jobDataMap.getInt("questionCount") + "개");
            alimtalkContent.setButtons(List.of(
                    new AlimtalkButton(
                            "질문 미리보기",
                            "WL",
                            "https://www.orv.im/interview/preview/" + jobDataMap.getString("reservationId"),
                            "https://www.orv.im/interview/preview/" + jobDataMap.getString("reservationId")
                    )
            ));

            String msgKey = alimtalkService.sendAlimtalk(alimtalkContent);
            log.info("Sent alimtalk with msgKey: {}", msgKey);
        } catch (Exception e) {
            log.error("Failed to send alimtalk", e);
            return;
        }

    }
}
