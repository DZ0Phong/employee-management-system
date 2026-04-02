package com.group5.ems.controller.auth;

import com.group5.ems.service.auth.AccountActivationService;
import com.group5.ems.service.auth.AccountActivationService.VerifyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Luồng kích hoạt tài khoản INACTIVE qua email OTP.
 *
 * GET  /activate?email=xxx     → Trang kích hoạt (tự gửi OTP nếu chưa có)
 * POST /activate/send-otp      → Gửi lại OTP (AJAX)
 * POST /activate/verify        → Xác minh OTP → kích hoạt (AJAX)
 */
@Controller
@RequestMapping("/activate")
@RequiredArgsConstructor
public class AuthActivationController {

    private final AccountActivationService activationService;

    @GetMapping
    public String showActivatePage(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/login";
        }

        model.addAttribute("email", email);

        // Tự gửi OTP nếu chưa có OTP đang chờ
        long remaining = activationService.getRemainingSeconds(email);
        if (remaining <= 0) {
            activationService.sendOtp(email);
            remaining = activationService.getRemainingSeconds(email);
        }
        model.addAttribute("remainingSeconds", remaining);

        return "auth/activate";
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        boolean sent = activationService.sendOtp(email);
        if (sent) {
            return ResponseEntity.ok("SENT");
        }
        // false có thể do cooldown (60 giây) hoặc email không tồn tại
        return ResponseEntity.status(429).body("COOLDOWN");
    }

    @PostMapping("/verify")
    @ResponseBody
    public ResponseEntity<String> verify(@RequestParam String email,
                                         @RequestParam String code) {
        VerifyResult result = activationService.verifyAndActivate(email, code);
        return switch (result) {
            case OK             -> ResponseEntity.ok("OK");
            case ALREADY_ACTIVE -> ResponseEntity.ok("ALREADY_ACTIVE");
            case EXPIRED        -> ResponseEntity.status(410).body("EXPIRED");
            case NOT_FOUND      -> ResponseEntity.badRequest().body("NOT_FOUND");
            default             -> ResponseEntity.badRequest().body("INVALID");
        };
    }
}
