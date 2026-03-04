package com.datdt.authenticationsystem.service;

import com.datdt.authenticationsystem.entity.UserEntity;
import com.datdt.authenticationsystem.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    //load user through user detail service
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("username not found with email: " + email));
        return User.builder()
                .username(existingUser.getEmail())
                .password(existingUser.getPassword())
                .roles("")
                .build();
    }
}
