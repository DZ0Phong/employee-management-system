package com.group5.ems.controller;

import com.group5.ems.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/")
    public String index() {
        return "redirect:/home";  // landing page = guest portal
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam(value = "email", required = false) String email,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (email != null && !email.isBlank()) {
            long remainingSeconds = authService.getRemainingOtpSeconds(email);
            if (remainingSeconds <= 0) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Your verification code has expired. Please request a new code."
                );
                return "redirect:/forgot-password";
            }

            model.addAttribute("email", email);
            model.addAttribute("remainingSeconds", remainingSeconds);
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean sent = authService.sendPasswordResetEmail(email);
            if (sent) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "We have sent a verification code to your email. Please check your inbox."
                );
                // Only redirect with email param when a code has actually been generated
                return "redirect:/forgot-password?email=" + email;
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "No account found with the specified email address."
                );
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to send password reset email. Please try again later."
            );
        }

        // For invalid email / general failure, return to plain form without OTP state
        return "redirect:/forgot-password";
    }

    @PostMapping("/forgot-password/verify-otp")
    @ResponseBody
    public ResponseEntity<String> verifyOtp(
            @RequestParam("email") String email,
            @RequestParam("code") String code
    ) {
        AuthService.OtpStatus status = authService.verifyResetOtp(email, code);
        if (status == AuthService.OtpStatus.VALID) {
            return ResponseEntity.ok("OK");
        } else if (status == AuthService.OtpStatus.EXPIRED) {
            return ResponseEntity.status(HttpStatus.GONE).body("EXPIRED");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID");
        }
    }

    @PostMapping("/forgot-password/resend-otp")
    @ResponseBody
    public ResponseEntity<String> resendOtp(@RequestParam("email") String email) {
        try {
            boolean sent = authService.resendOtp(email);
            if (sent) {
                return ResponseEntity.ok("SENT");
            }
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Please wait before requesting a new code.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not resend verification code. Please try again later.");
        }
    }

    @PostMapping("/forgot-password/reset-password")
    @ResponseBody
    public ResponseEntity<String> resetPassword(
            @RequestParam("email") String email,
            @RequestParam("password") String password
    ) {
        AuthService.PasswordStatus status = authService.validatePassword(password);
        if (status != AuthService.PasswordStatus.OK) {
            String message = switch (status) {
                case TOO_SHORT -> "Password must be at least 8 characters long.";
                case NO_UPPERCASE -> "Password must contain at least one uppercase letter.";
                case NO_NUMBER -> "Password must contain at least one number.";
                case NO_SPECIAL -> "Password must contain at least one special character.";
                default -> "Invalid password.";
            };
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
        }

        boolean updated = authService.resetPassword(email, password);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Unable to reset password. Please make sure your verification code is still valid and try again.");
        }

        return ResponseEntity.ok("OK");
    }
}
