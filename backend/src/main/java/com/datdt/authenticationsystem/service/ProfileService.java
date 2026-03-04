package com.datdt.authenticationsystem.service;

import com.datdt.authenticationsystem.io.ProfileRequest;
import com.datdt.authenticationsystem.io.ProfileResponse;
import org.springframework.context.annotation.Profile;

public interface ProfileService {
    ProfileResponse createProfile(ProfileRequest profileRequest);

    ProfileResponse getProfile(String email);

    void sendResetOtp(String email);

    void sendResetPassword(String email,String otp,String newPassword);

    void sendOtp(String email);

    void verifyOtp(String email,String otp);

}
