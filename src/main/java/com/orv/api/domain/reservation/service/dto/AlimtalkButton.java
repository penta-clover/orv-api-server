package com.orv.api.domain.reservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlimtalkButton {
    private String name;
    private String type;
    private String urlPc;
    private String urlMobile;
}
