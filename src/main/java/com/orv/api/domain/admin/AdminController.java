package com.orv.api.domain.admin;

import com.orv.api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v0/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    @GetMapping("/")
    public ApiResponse checkAdmin() {
        log.info("Admin check endpoint hit");
        return ApiResponse.success("You are an admin!", 200);
    }
}
