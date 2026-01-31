package com.orv.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimtalkContent {
    private String to;
    private String templateCode;
    private String title;
    private String text;
    private List<AlimtalkButton> buttons;
    private String msgType;
}
