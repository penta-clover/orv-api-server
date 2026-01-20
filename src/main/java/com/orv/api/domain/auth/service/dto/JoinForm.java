package com.orv.api.domain.auth.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class JoinForm {
    private String nickname;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDay;
    private String phoneNumber;
}
