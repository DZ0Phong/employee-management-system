package com.group5.hrm.service;

import com.group5.hrm.entity.Role;
import com.group5.hrm.entity.User;
import com.group5.hrm.entity.UserRole;
import com.group5.hrm.repository.UserRepository;
import com.group5.hrm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       User existingUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found: " + username));
       List<Role> userRoles =  userRoleRepository.getRolesByUserId(existingUser.getId());

       List<GrantedAuthority> grantedAuthorities = userRoles.stream()
               .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode())).collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(existingUser.getUsername())
                .password(existingUser.getPasswordHash())
                .authorities(grantedAuthorities).build();
    }
}
