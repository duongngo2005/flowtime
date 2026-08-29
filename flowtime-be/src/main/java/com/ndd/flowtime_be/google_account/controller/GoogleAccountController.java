package com.ndd.flowtime_be.google_account.controller;

import com.ndd.flowtime_be.google_account.service.GoogleAccountService;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class GoogleAccountController {

    private final GoogleAccountService googleAccountService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("connected", googleAccountService.isConnected(user)));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal User user) {
        googleAccountService.disconnect(user);
        return ResponseEntity.noContent().build();
    }
}
