CREATE DATABASE IF NOT EXISTS ems_system;
USE ems_system;

-- =========================================================================
-- MODULE 1: HỆ THỐNG TÀI KHOẢN & PHÂN QUYỀN (RBAC)
-- =========================================================================
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE,
    email VARCHAR(150) UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(150),
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    status VARCHAR(30) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, LOCKED',
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE TABLE otp_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    email VARCHAR(150),
    code VARCHAR(10) NOT NULL,
    type VARCHAR(30) NOT NULL COMMENT 'REGISTER, RESET_PASSWORD',
    expired_at DATETIME NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =========================================================================
-- MODULE 2: CẤU TRÚC TỔ CHỨC
-- =========================================================================
CREATE TABLE departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    manager_id BIGINT NULL COMMENT 'Sẽ update sau khi có bảng employees',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES departments(id)
);

CREATE TABLE positions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    department_id BIGINT COMMENT 'NULL = vị trí dùng chung',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- =========================================================================
-- MODULE 3: HỒ SƠ NHÂN SỰ & CẤP TÀI KHOẢN
-- =========================================================================
CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NULL COMMENT 'NULL khi mới tạo, map sau khi duyệt cấp tài khoản',
    employee_code VARCHAR(50) UNIQUE,
    department_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    line_manager_id BIGINT NULL COMMENT 'Quản lý trực tiếp của nhân viên',
    hire_date DATE,
    status VARCHAR(30) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ON_LEAVE, TERMINATED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (department_id) REFERENCES departments(id),
    FOREIGN KEY (position_id) REFERENCES positions(id),
    FOREIGN KEY (line_manager_id) REFERENCES employees(id)
);

-- Update manager_id cho phòng ban sau khi đã có bảng employees
ALTER TABLE departments ADD FOREIGN KEY (manager_id) REFERENCES employees(id);

CREATE TABLE account_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    proposed_role_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    approved_by BIGINT,
    request_reason TEXT,
    status VARCHAR(30) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    contract_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    document_url VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    check_in TIME,
    check_out TIME,
    status VARCHAR(30) DEFAULT 'PRESENT',
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_attendance_employee_date (employee_id, work_date)
);

CREATE TABLE salaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE employee_benefits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
-- MODULE 5: HIỆU SUẤT & ĐÁNH GIÁ (Năng lực, KPI, Performance Review)
-- =========================================================================
CREATE TABLE skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
-- MODULE 6: CÔNG VIỆC, YÊU CẦU & TUYỂN DỤNG
-- =========================================================================
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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

CREATE TABLE request_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    request_type_id BIGINT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    leave_from DATE,
    leave_to DATE,
    leave_type VARCHAR(50),
    status VARCHAR(30) DEFAULT 'PENDING',
    rejected_reason TEXT,
    current_approver_id BIGINT COMMENT 'Người đang cần xử lý request hiện tại',
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL COMMENT 'APPROVED, REJECTED, FORWARDED, CANCELLED',
    comment TEXT,
    action_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
    FOREIGN KEY (approver_id) REFERENCES users(id)
);

CREATE TABLE candidates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_id BIGINT,
    file_name VARCHAR(255),
    file_path VARCHAR(255),
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

-- =========================
-- JOB POSTS
-- =========================
CREATE TABLE job_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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

-- =========================
-- APPLICATIONS
-- =========================
CREATE TABLE applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
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
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT,
    stage_name VARCHAR(50),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);

-- =========================
-- INTERVIEWS
-- =========================
CREATE TABLE interviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT,
    scheduled_at DATETIME,
    location VARCHAR(255),
    interviewer_id BIGINT,
    status VARCHAR(30),
    feedback TEXT,
    FOREIGN KEY (application_id) REFERENCES applications(id),
    FOREIGN KEY (interviewer_id) REFERENCES users(id)
);

-- =========================
-- OFFERS
-- =========================
CREATE TABLE offers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT,
    salary_offered DECIMAL(15,2),
    start_date DATE,
    status VARCHAR(30),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);
-- =========================
-- EMAIL
-- =========================
CREATE TABLE email_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) UNIQUE,
    subject VARCHAR(255),
    body TEXT
);

CREATE TABLE email_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_email VARCHAR(150),
    template_code VARCHAR(100),
    status VARCHAR(30),
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- AUDIT
-- =========================
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    action VARCHAR(50),
    performed_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- =========================
-- COMPANY INFO
-- =========================

CREATE TABLE company_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    info_key VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200),
    content TEXT,
    is_public BOOLEAN DEFAULT TRUE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- 1. Quản lý kỳ chốt công (Dành cho EMS Lock/Unlock)
CREATE TABLE timesheet_periods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_name VARCHAR(100), -- Ví dụ: "Tháng 03/2026"
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_at DATETIME,
    locked_by BIGINT,
    FOREIGN KEY (locked_by) REFERENCES users(id)
);

-- 2. Phiếu lương thực tế (Kết quả của Payroll Engine mà EMS sẽ duyệt)
CREATE TABLE payslips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    actual_base_salary DECIMAL(15,2),
    total_ot_amount DECIMAL(15,2),
    total_bonus DECIMAL(15,2),
    total_deduction DECIMAL(15,2), -- Các khoản trừ
    net_salary DECIMAL(15,2),
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, APPROVED, PAID
    approved_by BIGINT,
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (period_id) REFERENCES timesheet_periods(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- 3. Cập nhật bảng contracts để EMS phê duyệt
ALTER TABLE contracts ADD COLUMN approved_by BIGINT;
ALTER TABLE contracts ADD COLUMN approved_at DATETIME;
ALTER TABLE contracts ADD FOREIGN KEY (approved_by) REFERENCES users(id);

-- CHÈN DATA MẪU BẮT BUỘC
INSERT INTO roles (code, name, description) VALUES
('ADMIN', 'Admin', 'Quản trị hệ thống'),
('HR_MANAGER', 'HR Manager', 'Trưởng phòng nhân sự'),
('HR', 'HR', 'Nhân viên nhân sự'),
('DEPT_MANAGER', 'Trưởng phòng', 'Trưởng phòng ban'),
('EMPLOYEE', 'Nhân viên', 'Nhân viên công ty'),
('GUEST', 'Guest', 'Khách truy cập/Ứng viên');

INSERT INTO request_types (code, name, description) VALUES
('LEAVE', 'Đơn xin nghỉ', 'Nghỉ phép, nghỉ ốm, nghỉ việc riêng...'),
('PERSONAL', 'Đơn yêu cầu cá nhân', 'Yêu cầu cá nhân khác');