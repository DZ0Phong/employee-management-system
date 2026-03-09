-- =========================================================================
-- KHỞI TẠO DATABASE
-- =========================================================================
DROP DATABASE IF EXISTS ems_system;
CREATE DATABASE ems_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ems_system;

-- =========================================================================
-- MODULE 1: HỆ THỐNG TÀI KHOẢN & PHÂN QUYỀN (RBAC)
-- =========================================================================
CREATE TABLE roles (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       code VARCHAR(50) NOT NULL UNIQUE,
                       name VARCHAR(100) NOT NULL,
                       description VARCHAR(255),
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(100) UNIQUE,
                       email VARCHAR(150) UNIQUE,
                       password_hash VARCHAR(255),
                       full_name VARCHAR(150),
                       phone VARCHAR(20),
                       avatar_url VARCHAR(255),
                       status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, LOCKED
                       is_verified BOOLEAN DEFAULT FALSE,
                       last_login_at DATETIME,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            granted_by BIGINT,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id),
                            FOREIGN KEY (granted_by) REFERENCES users(id)
);

CREATE TABLE system_settings (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 setting_key VARCHAR(100) NOT NULL UNIQUE,
                                 setting_value TEXT,
                                 description VARCHAR(255),
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 updated_by BIGINT,
                                 FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE TABLE otp_verifications (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   user_id BIGINT,
                                   email VARCHAR(150),
                                   code VARCHAR(10) NOT NULL,
                                   type VARCHAR(30) NOT NULL, -- REGISTER, RESET_PASSWORD
                                   expired_at DATETIME NOT NULL,
                                   is_used BOOLEAN DEFAULT FALSE,
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =========================================================================
-- MODULE 2: CẤU TRÚC TỔ CHỨC
-- =========================================================================
CREATE TABLE departments (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             code VARCHAR(50) UNIQUE,
                             name VARCHAR(100) NOT NULL,
                             description TEXT,
                             parent_id BIGINT,
                             manager_id BIGINT NULL, -- Sẽ update sau khi có bảng employees
                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             FOREIGN KEY (parent_id) REFERENCES departments(id)
);

CREATE TABLE positions (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           code VARCHAR(50) UNIQUE,
                           name VARCHAR(100) NOT NULL,
                           description TEXT,
                           department_id BIGINT NULL, -- NULL = vị trí dùng chung
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- =========================================================================
-- MODULE 3: HỒ SƠ NHÂN SỰ & CẤP TÀI KHOẢN
-- =========================================================================
CREATE TABLE employees (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT UNIQUE NULL, -- NULL khi mới tạo, map sau khi duyệt cấp tài khoản
                           employee_code VARCHAR(50) UNIQUE,
                           department_id BIGINT NOT NULL,
                           position_id BIGINT NOT NULL,
                           line_manager_id BIGINT NULL, -- Quản lý trực tiếp của nhân viên
                           hire_date DATE,
                           status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, ON_LEAVE, TERMINATED
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           FOREIGN KEY (user_id) REFERENCES users(id),
                           FOREIGN KEY (department_id) REFERENCES departments(id),
                           FOREIGN KEY (position_id) REFERENCES positions(id),
                           FOREIGN KEY (line_manager_id) REFERENCES employees(id)
);

-- Update manager_id cho phòng ban sau khi đã có bảng employees
ALTER TABLE departments ADD CONSTRAINT fk_dept_manager FOREIGN KEY (manager_id) REFERENCES employees(id);

CREATE TABLE account_requests (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  employee_id BIGINT NOT NULL,
                                  proposed_role_id BIGINT NOT NULL,
                                  requested_by BIGINT NOT NULL,
                                  approved_by BIGINT,
                                  request_reason TEXT,
                                  status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
                                  rejected_reason TEXT,
                                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  FOREIGN KEY (employee_id) REFERENCES employees(id),
                                  FOREIGN KEY (proposed_role_id) REFERENCES roles(id),
                                  FOREIGN KEY (requested_by) REFERENCES users(id),
                                  FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- =========================================================================
-- MODULE 4: VẬN HÀNH NHÂN SỰ (Hợp đồng, Chấm công, Tiền lương, Phúc lợi)
-- =========================================================================
CREATE TABLE contracts (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           employee_id BIGINT NOT NULL,
                           contract_type VARCHAR(50) NOT NULL,
                           start_date DATE NOT NULL,
                           end_date DATE,
                           document_url VARCHAR(500),
                           status VARCHAR(30) DEFAULT 'ACTIVE',
                           note TEXT,
                           approved_by BIGINT,
                           approved_at DATETIME,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           created_by BIGINT,
                           FOREIGN KEY (employee_id) REFERENCES employees(id),
                           FOREIGN KEY (created_by) REFERENCES users(id),
                           FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE TABLE attendance (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            employee_id BIGINT NOT NULL,
                            work_date DATE NOT NULL,
                            check_in TIME,
                            check_out TIME,
                            status VARCHAR(30) DEFAULT 'PRESENT',
                            note TEXT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            created_by BIGINT,
                            UNIQUE KEY uk_attendance_employee_date (employee_id, work_date),
                            FOREIGN KEY (employee_id) REFERENCES employees(id),
                            FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE salaries (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          employee_id BIGINT NOT NULL,
                          base_amount DECIMAL(15,2) NOT NULL,
                          allowance_amount DECIMAL(15,2) DEFAULT 0,
                          effective_from DATE NOT NULL,
                          effective_to DATE,
                          salary_type VARCHAR(30) DEFAULT 'MONTHLY',
                          note TEXT,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_by BIGINT,
                          FOREIGN KEY (employee_id) REFERENCES employees(id),
                          FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE bonuses (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         employee_id BIGINT NOT NULL,
                         bonus_type VARCHAR(50) NOT NULL,
                         amount DECIMAL(15,2) NOT NULL,
                         bonus_date DATE,
                         period_description VARCHAR(100),
                         status VARCHAR(30) DEFAULT 'PENDING',
                         note TEXT,
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         created_by BIGINT,
                         approved_by BIGINT,
                         approved_at DATETIME,
                         FOREIGN KEY (employee_id) REFERENCES employees(id),
                         FOREIGN KEY (created_by) REFERENCES users(id),
                         FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE TABLE benefit_types (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               code VARCHAR(50) UNIQUE,
                               name VARCHAR(100) NOT NULL,
                               description TEXT,
                               is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE employee_benefits (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   employee_id BIGINT NOT NULL,
                                   benefit_type_id BIGINT NOT NULL,
                                   value_amount DECIMAL(15,2),
                                   value_description VARCHAR(255),
                                   effective_from DATE NOT NULL,
                                   effective_to DATE,
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   created_by BIGINT,
                                   FOREIGN KEY (employee_id) REFERENCES employees(id),
                                   FOREIGN KEY (benefit_type_id) REFERENCES benefit_types(id),
                                   FOREIGN KEY (created_by) REFERENCES users(id)
);

-- =========================================================================
-- MODULE 5: HIỆU SUẤT & ĐÁNH GIÁ
-- =========================================================================
CREATE TABLE skills (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        category VARCHAR(50) NOT NULL,
                        description VARCHAR(255)
);

CREATE TABLE employee_skills (
                                 employee_id BIGINT NOT NULL,
                                 skill_id BIGINT NOT NULL,
                                 proficiency INT NOT NULL,
                                 verified_by BIGINT,
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 PRIMARY KEY (employee_id, skill_id),
                                 FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                                 FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
                                 FOREIGN KEY (verified_by) REFERENCES users(id)
);

CREATE TABLE employee_kpis (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               employee_id BIGINT NOT NULL,
                               period VARCHAR(30) NOT NULL,
                               kpi_name VARCHAR(200) NOT NULL,
                               weight DECIMAL(5,2) DEFAULT 0,
                               target_value DECIMAL(15,2) NOT NULL,
                               achieved_value DECIMAL(15,2) DEFAULT 0,
                               completion_rate DECIMAL(5,2) GENERATED ALWAYS AS (
                                   CASE WHEN target_value > 0 THEN (achieved_value / target_value) * 100 ELSE 0 END
                                   ) STORED,
                               status VARCHAR(30) DEFAULT 'IN_PROGRESS',
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE performance_reviews (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     employee_id BIGINT NOT NULL,
                                     reviewer_id BIGINT NOT NULL,
                                     review_period VARCHAR(50) NOT NULL,
                                     performance_score DECIMAL(3,2) NOT NULL,
                                     potential_score DECIMAL(3,2) NOT NULL,
                                     talent_matrix VARCHAR(50) GENERATED ALWAYS AS (
                                         CASE
                                             WHEN performance_score >= 4.0 AND potential_score >= 4.0 THEN 'Star'
                                             WHEN performance_score >= 4.0 AND potential_score >= 2.5 THEN 'High Performer'
                                             WHEN performance_score >= 4.0 AND potential_score < 2.5  THEN 'Workhorse'
                                             WHEN performance_score >= 2.5 AND potential_score >= 4.0 THEN 'High Potential'
                                             WHEN performance_score >= 2.5 AND potential_score >= 2.5 THEN 'Core Employee'
                                             WHEN performance_score >= 2.5 AND potential_score < 2.5  THEN 'Effective'
                                             WHEN performance_score < 2.5  AND potential_score >= 4.0 THEN 'Problem Child'
                                             WHEN performance_score < 2.5  AND potential_score >= 2.5 THEN 'Inconsistent'
                                             ELSE 'Underperformer'
                                             END
                                         ) STORED,
                                     strengths TEXT,
                                     areas_to_improve TEXT,
                                     status VARCHAR(30) DEFAULT 'DRAFT',
                                     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     UNIQUE KEY uk_review_emp_period (employee_id, review_period),
                                     FOREIGN KEY (employee_id) REFERENCES employees(id),
                                     FOREIGN KEY (reviewer_id) REFERENCES employees(id)
);

CREATE TABLE reward_disciplines (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    employee_id BIGINT NOT NULL,
                                    record_type VARCHAR(30) NOT NULL,
                                    title VARCHAR(200) NOT NULL,
                                    description TEXT,
                                    decision_date DATE NOT NULL,
                                    amount DECIMAL(15,2) DEFAULT 0,
                                    decided_by BIGINT,
                                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (employee_id) REFERENCES employees(id),
                                    FOREIGN KEY (decided_by) REFERENCES users(id)
);

-- =========================================================================
-- MODULE 6: CÔNG VIỆC, YÊU CẦU & TUYỂN DỤNG (ĐÃ ÁP DỤNG PHƯƠNG ÁN LAI)
-- =========================================================================
CREATE TABLE tasks (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       title VARCHAR(200) NOT NULL,
                       description TEXT,
                       assigned_to BIGINT NOT NULL,
                       assigned_by BIGINT NOT NULL,
                       due_date DATE,
                       status VARCHAR(30) DEFAULT 'PENDING',
                       priority VARCHAR(20) DEFAULT 'NORMAL',
                       completed_at DATETIME,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       FOREIGN KEY (assigned_to) REFERENCES users(id),
                       FOREIGN KEY (assigned_by) REFERENCES users(id)
);

-- [PHƯƠNG ÁN LAI] CHỈ CẦN 1 BẢNG REQUEST_TYPES CÓ THÊM CỘT CATEGORY
CREATE TABLE request_types (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               category VARCHAR(50) NOT NULL,     -- VD: 'ATTENDANCE', 'PAYROLL', 'ADMIN'
                               code VARCHAR(50) NOT NULL UNIQUE,  -- VD: 'LEAVE_ANNUAL'
                               name VARCHAR(100) NOT NULL,
                               description VARCHAR(255),
                               is_active BOOLEAN DEFAULT TRUE
);

-- [PHƯƠNG ÁN LAI] BẢNG REQUESTS TỐI ƯU CÓ OTHER_DETAIL, START_DATE, END_DATE
CREATE TABLE requests (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          employee_id BIGINT NOT NULL,
                          request_type_id BIGINT NOT NULL,

                          title VARCHAR(200),
                          content TEXT,
                          other_detail VARCHAR(255), -- Dùng khi user chọn loại đơn có đuôi _OTHER

                          start_date DATETIME,
                          end_date DATETIME,

                          status VARCHAR(30) DEFAULT 'PENDING',
                          rejected_reason TEXT,
                          current_approver_id BIGINT,
                          approved_by BIGINT,
                          approved_at DATETIME,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          FOREIGN KEY (employee_id) REFERENCES employees(id),
                          FOREIGN KEY (request_type_id) REFERENCES request_types(id),
                          FOREIGN KEY (current_approver_id) REFERENCES users(id),
                          FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE TABLE request_approval_history (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          request_id BIGINT NOT NULL,
                                          approver_id BIGINT NOT NULL,
                                          action VARCHAR(30) NOT NULL, -- APPROVED, REJECTED, FORWARDED, CANCELLED
                                          comment TEXT,
                                          action_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                          FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
                                          FOREIGN KEY (approver_id) REFERENCES users(id)
);

CREATE TABLE candidates (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            full_name VARCHAR(150),
                            email VARCHAR(150),
                            phone VARCHAR(20),
                            headline VARCHAR(255),
                            summary TEXT,
                            years_experience INT,
                            expected_salary DECIMAL(15,2),
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uk_candidates_email_phone (email, phone)
);

CREATE TABLE candidate_cvs (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               candidate_id BIGINT,
                               file_name VARCHAR(255),
                               file_path VARCHAR(255),
                               uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

-- =========================
-- JOB POSTS & APPLICATIONS
-- =========================
CREATE TABLE job_posts (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           title VARCHAR(150),
                           department_id BIGINT,
                           position_id BIGINT,
                           description TEXT,
                           requirements TEXT,
                           benefits TEXT,
                           salary_min DECIMAL(15,2),
                           salary_max DECIMAL(15,2),
                           status VARCHAR(30) DEFAULT 'OPEN',
                           open_date DATE,
                           close_date DATE,
                           created_by BIGINT,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (department_id) REFERENCES departments(id),
                           FOREIGN KEY (position_id) REFERENCES positions(id),
                           FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE applications (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              job_post_id BIGINT,
                              candidate_id BIGINT,
                              cv_id BIGINT,
                              status VARCHAR(30) DEFAULT 'APPLIED',
                              applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_job_candidate (job_post_id, candidate_id),
                              FOREIGN KEY (job_post_id) REFERENCES job_posts(id),
                              FOREIGN KEY (candidate_id) REFERENCES candidates(id),
                              FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id)
);

CREATE TABLE application_stages (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    application_id BIGINT,
                                    stage_name VARCHAR(50),
                                    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    note TEXT,
                                    FOREIGN KEY (application_id) REFERENCES applications(id)
);

CREATE TABLE interviews (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            application_id BIGINT,
                            scheduled_at DATETIME,
                            location VARCHAR(255),
                            interviewer_id BIGINT,
                            status VARCHAR(30),
                            feedback TEXT,
                            FOREIGN KEY (application_id) REFERENCES applications(id),
                            FOREIGN KEY (interviewer_id) REFERENCES users(id)
);

CREATE TABLE offers (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        application_id BIGINT,
                        salary_offered DECIMAL(15,2),
                        start_date DATE,
                        status VARCHAR(30),
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (application_id) REFERENCES applications(id)
);

-- =========================
-- EMAIL, AUDIT, COMPANY INFO & PAYROLL
-- =========================
CREATE TABLE email_templates (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 code VARCHAR(100) UNIQUE,
                                 subject VARCHAR(255),
                                 body TEXT
);

CREATE TABLE email_logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            recipient_email VARCHAR(150),
                            template_code VARCHAR(100),
                            status VARCHAR(30),
                            sent_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            entity_name VARCHAR(100),
                            entity_id BIGINT,
                            action VARCHAR(50),
                            performed_by BIGINT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_info (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              info_key VARCHAR(100) NOT NULL UNIQUE,
                              title VARCHAR(200),
                              content TEXT,
                              is_public BOOLEAN DEFAULT TRUE,
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              updated_by BIGINT,
                              FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE TABLE timesheet_periods (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   period_name VARCHAR(100), -- Ví dụ: "Tháng 03/2026"
                                   start_date DATE NOT NULL,
                                   end_date DATE NOT NULL,
                                   is_locked BOOLEAN DEFAULT FALSE,
                                   locked_at DATETIME,
                                   locked_by BIGINT,
                                   FOREIGN KEY (locked_by) REFERENCES users(id)
);

CREATE TABLE payslips (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          employee_id BIGINT NOT NULL,
                          period_id BIGINT NOT NULL,
                          actual_base_salary DECIMAL(15,2),
                          total_ot_amount DECIMAL(15,2),
                          total_bonus DECIMAL(15,2),
                          total_deduction DECIMAL(15,2),
                          net_salary DECIMAL(15,2),
                          status VARCHAR(30) DEFAULT 'PENDING',
                          approved_by BIGINT,
                          FOREIGN KEY (employee_id) REFERENCES employees(id),
                          FOREIGN KEY (period_id) REFERENCES timesheet_periods(id),
                          FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- =========================================================================
-- CHÈN DATA MẪU CƠ BẢN
-- =========================================================================
INSERT INTO roles (code, name, description) VALUES
                                                ('ADMIN', 'Admin', 'Quản trị hệ thống'),
                                                ('HR_MANAGER', 'HR Manager', 'Trưởng phòng nhân sự'),
                                                ('HR', 'HR', 'Nhân viên nhân sự'),
                                                ('DEPT_MANAGER', 'Trưởng phòng', 'Trưởng phòng ban'),
                                                ('EMPLOYEE', 'Nhân viên', 'Nhân viên công ty'),
                                                ('GUEST', 'Guest', 'Khách truy cập/Ứng viên');

-- CHÈN DỮ LIỆU ĐƠN TỪ (PHƯƠNG ÁN LAI CÓ CỘT CATEGORY & ĐƠN OTHER)
INSERT INTO request_types (category, code, name, description) VALUES
-- Nhóm 1: Chấm công & Thời gian
('ATTENDANCE', 'LEAVE_ANNUAL', 'Nghỉ phép năm', 'Sử dụng quỹ phép năm'),
('ATTENDANCE', 'LEAVE_SICK', 'Nghỉ ốm', 'Cần giấy chứng nhận bác sĩ'),
('ATTENDANCE', 'LEAVE_UNPAID', 'Nghỉ không lương', 'Nghỉ việc riêng không hưởng lương'),
('ATTENDANCE', 'ATT_OVERTIME', 'Xin làm thêm giờ (OT)', 'Đăng ký OT để tính lương'),
('ATTENDANCE', 'ATT_OTHER', 'Đơn thời gian khác', 'Dùng cho các trường hợp khác liên quan đến giờ giấc'),

-- Nhóm 2: Lương & Phúc lợi
('PAYROLL', 'PAY_ADVANCE', 'Tạm ứng tiền', 'Xin ứng trước lương / công tác phí'),
('PAYROLL', 'PAY_CLAIM', 'Thanh toán hoàn ứng (Claim)', 'Thanh toán lại chi phí đã bỏ ra cho công ty'),
('PAYROLL', 'PAY_OTHER', 'Đơn Lương/Phúc lợi khác', 'Ghi rõ lý do vào ô chi tiết'),

-- Nhóm 3: Hành chính & Hỗ trợ
('ADMIN', 'ADM_EQUIPMENT', 'Xin cấp/đổi tài sản', 'Xin cấp laptop, màn hình, văn phòng phẩm...'),
('ADMIN', 'ADM_CONFIRM', 'Xin xác nhận công tác/thu nhập', 'Dùng để vay vốn, làm visa...'),
('ADMIN', 'ADM_OTHER', 'Yêu cầu Hành chính khác', 'Các hỗ trợ hành chính khác'),

-- Nhóm 4: Trạng thái Nhân sự
('HR_STATUS', 'HR_RESIGN', 'Đơn xin thôi việc', 'Thông báo chấm dứt HĐLĐ'),
('HR_STATUS', 'HR_OTHER', 'Đơn trạng thái NS khác', 'Ví dụ: xin gia hạn hợp đồng, đổi chức danh...');