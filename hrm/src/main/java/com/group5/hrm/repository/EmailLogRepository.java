package com.group5.hrm.repository;

import com.group5.hrm.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByRecipientEmail(String recipientEmail);

    List<EmailLog> findByTemplateCode(String templateCode);
}
