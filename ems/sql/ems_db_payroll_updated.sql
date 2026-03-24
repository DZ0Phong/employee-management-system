USE erm_system;

-- =========================================================================
-- UPGRADE 1: BANKING & DISBURSEMENT DETAILS
-- =========================================================================
CREATE TABLE employee_bank_details (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       employee_id BIGINT NOT NULL,
                                       bank_name VARCHAR(150) NOT NULL,
                                       branch_name VARCHAR(150),
                                       account_name VARCHAR(150) NOT NULL,
                                       account_number VARCHAR(255) NOT NULL, -- Should be encrypted at application layer
                                       is_primary BOOLEAN DEFAULT TRUE,
                                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- =========================================================================
-- UPGRADE 2: PAYROLL RULES ENGINE (Taxes, Standard Deductions, Overtime Rules)
-- =========================================================================
-- This replaces hard-coding deductions in your application
CREATE TABLE pay_components (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                code VARCHAR(50) UNIQUE NOT NULL, -- e.g., 'TAX_INCOME', 'SOCIAL_SEC', 'OT_150'
                                name VARCHAR(100) NOT NULL,
                                type VARCHAR(30) NOT NULL, -- 'EARNING', 'DEDUCTION', 'TAX', 'EMPLOYER_CONTRIBUTION'
                                is_taxable BOOLEAN DEFAULT TRUE,
                                calculation_method VARCHAR(50) DEFAULT 'FLAT_AMOUNT', -- 'FLAT_AMOUNT', 'PERCENTAGE', 'FORMULA'
                                default_value DECIMAL(15,2),
                                is_active BOOLEAN DEFAULT TRUE
);

-- =========================================================================
-- UPGRADE 3: PAYSLIP LINE ITEMS (For Auditability and Pay Transparency)
-- =========================================================================
-- Your current `payslips` table is just a summary header.
-- This table tracks the exact math for every single cent on the payslip.
CREATE TABLE payslip_line_items (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    payslip_id BIGINT NOT NULL,
                                    pay_component_id BIGINT NOT NULL, -- Links to 'Base Pay', 'Overtime', 'Income Tax', etc.
                                    description VARCHAR(255), -- E.g., "Overtime 1.5x (10 hours)"
                                    amount DECIMAL(15,2) NOT NULL,
                                    type VARCHAR(30) NOT NULL, -- 'EARNING', 'DEDUCTION' (copied for easy querying)
                                    FOREIGN KEY (payslip_id) REFERENCES payslips(id) ON DELETE CASCADE,
                                    FOREIGN KEY (pay_component_id) REFERENCES pay_components(id)
);

-- =========================================================================
-- UPGRADE 4: MODIFY EXISTING PAYSLIPS TABLE
-- =========================================================================
-- We add 'gross_salary' which is standard accounting practice,
-- and tracking for who executed the actual bank payment.
ALTER TABLE payslips
    ADD COLUMN total_gross_salary DECIMAL(15,2) AFTER period_id,
ADD COLUMN payment_date DATE AFTER approved_by,
ADD COLUMN payment_reference VARCHAR(100) AFTER payment_date;