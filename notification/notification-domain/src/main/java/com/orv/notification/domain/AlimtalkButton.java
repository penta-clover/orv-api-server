package com.orv.notification.domain;

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
