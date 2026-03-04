package com.datdt.authenticationsystem.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    public String generateToken(UserDetails userDetails){
        Map<String,Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String email) {
        return Jwts.builder().setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                //tạo token tồn tại 10 tiếng
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();

    }
    // lấy ra tất cả claims
    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
    //trả ra 1 kiểu dữ liệu T
    //viết kiểu này gọn hơn để tái sử dụng nhiều lan hoặc có thể viết ra
    //theo kiểu là gán Claims c = extractAllClaims và dùng c.something
    //để lấy ra kiểu dữ liệu cần
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
            Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
    }
    //apply extractClaim để lây ra email (1 kiểu dữ liệu T mà hàm trên trả ve)
    public String extractEmail(String token){
        return extractClaim(token, Claims::getSubject);
    }
    //apply để lấy ra expiration
    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    private Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
