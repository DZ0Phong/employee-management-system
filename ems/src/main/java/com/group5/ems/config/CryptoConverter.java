package com.group5.ems.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA Attribute Converter for AES Encryption of sensitive PII (Bank Accounts).
 * Format: [16-byte IV][Ciphertext] -> Base64 encoded.
 */
@Component
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static String secretKey;

    @Value("${crypto.secret:YXNkZmFzZGZhc2RmYXNkZg==}") 
    public void setSecretKey(String secret) {
        CryptoConverter.secretKey = secret;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            
            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            
            // Extract IV
            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Extract Ciphertext
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
            
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);

            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting field", e);
        }
    }
}
