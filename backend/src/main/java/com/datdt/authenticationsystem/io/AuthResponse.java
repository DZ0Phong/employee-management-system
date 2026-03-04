package com.datdt.authenticationsystem.io;

import lombok.*;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String email;
    private String token;

}
