create table benefit_types
(
    id          bigint auto_increment
        primary key,
    code        varchar(50)          null,
    name        varchar(100)         not null,
    description text                 null,
    is_active   tinyint(1) default 1 null,
    constraint code
        unique (code)
);

create table candidates
(
    id               bigint auto_increment
        primary key,
    full_name        varchar(150)                       null,
    email            varchar(150)                       null,
    phone            varchar(20)                        null,
    years_experience int                                null,
    expected_salary  decimal(15, 2)                     null,
    created_at       datetime default CURRENT_TIMESTAMP null,
    address          varchar(255)                       null,
    date_of_birth    date                               null,
    introduction     text                               null,
    headline         varchar(255)                       null,
    summary          text                               null,
    constraint uk_candidates_email_phone
        unique (email, phone)
);

create table candidate_cvs
(
    id           bigint auto_increment
        primary key,
    candidate_id bigint                             null,
    file_name    varchar(255)                       null,
    uploaded_at  datetime default CURRENT_TIMESTAMP null,
    file_type    varchar(255)                       null,
    file_data    longblob                           null,
    file_path    varchar(255)                       null,
    constraint candidate_cvs_ibfk_1
        foreign key (candidate_id) references candidates (id)
            on delete cascade
);

create index candidate_id
    on candidate_cvs (candidate_id);

create index idx_candidate_name
    on candidates (full_name);

create table departments
(
    id          bigint auto_increment
        primary key,
    code        varchar(50)                        null,
    name        varchar(100)                       not null,
    description text                               null,
    parent_id   bigint                             null,
    manager_id  bigint                             null comment 'Sẽ update sau khi có bảng employees',
    created_at  datetime default CURRENT_TIMESTAMP null,
    updated_at  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint code
        unique (code),
    constraint departments_ibfk_1
        foreign key (parent_id) references departments (id)
);

create index manager_id
    on departments (manager_id);

create index parent_id
    on departments (parent_id);

create table email_logs
(
    id              bigint auto_increment
        primary key,
    recipient_email varchar(150)                       null,
    template_code   varchar(100)                       null,
    status          varchar(30)                        null,
    sent_at         datetime default CURRENT_TIMESTAMP null
);

create table email_templates
(
    id      bigint auto_increment
        primary key,
    code    varchar(100) null,
    subject varchar(255) null,
    body    text         null,
    constraint code
        unique (code)
);

create table pay_components
(
    id                 bigint auto_increment
        primary key,
    code               varchar(50)                       not null,
    name               varchar(100)                      not null,
    type               varchar(30)                       not null,
    is_taxable         tinyint(1)  default 1             null,
    calculation_method varchar(50) default 'FLAT_AMOUNT' null,
    default_value      decimal(15, 2)                    null,
    is_active          tinyint(1)  default 1             null,
    constraint code
        unique (code)
);

create table positions
(
    id            bigint auto_increment
        primary key,
    code          varchar(50)                        null,
    name          varchar(100)                       not null,
    description   text                               null,
    department_id bigint                             null comment 'NULL = vị trí dùng chung',
    created_at    datetime default CURRENT_TIMESTAMP null,
    constraint code
        unique (code),
    constraint positions_ibfk_1
        foreign key (department_id) references departments (id)
);

create index idx_dept_active
    on positions (department_id);

create table request_types
(
    id          bigint auto_increment
        primary key,
    category    varchar(50)  not null,
    code        varchar(50)  not null,
    name        varchar(100) not null,
    description varchar(255) null,
    constraint code
        unique (code)
);

create table roles
(
    id          bigint auto_increment
        primary key,
    code        varchar(50)                        not null,
    name        varchar(100)                       not null,
    description varchar(255)                       null,
    created_at  datetime default CURRENT_TIMESTAMP null,
    constraint code
        unique (code)
);

create table skills
(
    id          bigint auto_increment
        primary key,
    name        varchar(100) not null,
    category    varchar(50)  not null,
    description varchar(255) null,
    constraint name
        unique (name)
);

create table users
(
    id                        bigint auto_increment
        primary key,
    username                  varchar(100)                                                             null,
    email                     varchar(150)                                                             null,
    password_hash             varchar(255)                                                             null,
    full_name                 varchar(150)                                                             null,
    phone                     varchar(20)                                                              null,
    avatar_url                varchar(255)                                                             null,
    status                    enum ('ACTIVE', 'INACTIVE', 'LOCKED', 'LOCK5') default 'ACTIVE'          not null,
    is_verified               tinyint(1)                                     default 0                 null,
    last_login_at             datetime                                                                 null,
    created_at                datetime                                       default CURRENT_TIMESTAMP null,
    updated_at                datetime                                       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    reset_otp                 varchar(6)                                                               null,
    reset_otp_expires_at      datetime(6)                                                              null,
    failed_login_count        int                                            default 0                 not null,
    locked_until              datetime                                                                 null,
    activation_otp            varchar(6)                                                               null,
    activation_otp_expires_at datetime                                                                 null,
    constraint email
        unique (email),
    constraint username
        unique (username)
);

create table audit_logs
(
    id           bigint auto_increment
        primary key,
    entity_name  varchar(100)                       null,
    entity_id    bigint                             null,
    action       varchar(50)                        null,
    performed_by bigint                             null,
    created_at   datetime default CURRENT_TIMESTAMP null,
    constraint FKonjalmr7kf8970g8gu7ymueer
        foreign key (performed_by) references users (id)
);

create table company_info
(
    id         bigint auto_increment
        primary key,
    info_key   varchar(100)                         not null,
    title      varchar(200)                         null,
    content    text                                 null,
    is_public  tinyint(1) default 1                 null,
    updated_at datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    updated_by bigint                               null,
    constraint info_key
        unique (info_key),
    constraint company_info_ibfk_1
        foreign key (updated_by) references users (id)
);

create index updated_by
    on company_info (updated_by);

create table employees
(
    id                   bigint auto_increment
        primary key,
    user_id              bigint                                null comment 'NULL khi mới tạo, map sau khi duyệt cấp tài khoản',
    employee_code        varchar(50)                           null,
    department_id        bigint                                null,
    position_id          bigint                                null,
    previous_position_id bigint                                null comment 'Position trước khi promotion (for future use)',
    promotion_date       date                                  null comment 'Ngày promotion gần nhất (for future use)',
    line_manager_id      bigint                                null comment 'Quản lý trực tiếp của nhân viên',
    hire_date            date                                  null,
    status               varchar(30) default 'ACTIVE'          null comment 'ACTIVE, ON_LEAVE, TERMINATED',
    termination_date     date                                  null comment 'Ngày nghỉ việc chính thức',
    termination_reason   varchar(255)                          null comment 'Lý do nghỉ việc',
    created_at           datetime    default CURRENT_TIMESTAMP null,
    updated_at           datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint employee_code
        unique (employee_code),
    constraint user_id
        unique (user_id),
    constraint employees_ibfk_1
        foreign key (user_id) references users (id),
    constraint employees_ibfk_2
        foreign key (department_id) references departments (id),
    constraint employees_ibfk_3
        foreign key (position_id) references positions (id),
    constraint employees_ibfk_4
        foreign key (line_manager_id) references employees (id)
);

create table account_requests
(
    id               bigint auto_increment
        primary key,
    employee_id      bigint                                not null,
    proposed_role_id bigint                                not null,
    requested_by     bigint                                not null,
    approved_by      bigint                                null,
    request_reason   text                                  null,
    status           varchar(30) default 'PENDING'         null comment 'PENDING, APPROVED, REJECTED',
    rejected_reason  text                                  null,
    created_at       datetime    default CURRENT_TIMESTAMP null,
    updated_at       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint account_requests_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint account_requests_ibfk_2
        foreign key (proposed_role_id) references roles (id),
    constraint account_requests_ibfk_3
        foreign key (requested_by) references users (id),
    constraint account_requests_ibfk_4
        foreign key (approved_by) references users (id)
);

create index approved_by
    on account_requests (approved_by);

create index employee_id
    on account_requests (employee_id);

create index proposed_role_id
    on account_requests (proposed_role_id);

create index requested_by
    on account_requests (requested_by);

create table attendance
(
    id          bigint auto_increment
        primary key,
    employee_id bigint                                not null,
    work_date   date                                  not null,
    check_in    time                                  null,
    check_out   time                                  null,
    status      varchar(30) default 'PRESENT'         null,
    note        text                                  null,
    created_at  datetime    default CURRENT_TIMESTAMP null,
    updated_at  datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    created_by  bigint                                null,
    constraint uk_attendance_employee_date
        unique (employee_id, work_date),
    constraint attendance_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint attendance_ibfk_2
        foreign key (created_by) references users (id)
);

create index created_by
    on attendance (created_by);

create index idx_attendance_date
    on attendance (work_date);

create index idx_attendance_status
    on attendance (status);

create table bonuses
(
    id                 bigint auto_increment
        primary key,
    employee_id        bigint                                not null,
    bonus_type         varchar(50)                           not null,
    amount             decimal(15, 2)                        not null,
    bonus_date         date                                  null,
    period_description varchar(100)                          null,
    status             varchar(30) default 'PENDING'         null,
    note               text                                  null,
    created_at         datetime    default CURRENT_TIMESTAMP null,
    created_by         bigint                                null,
    approved_by        bigint                                null,
    approved_at        datetime                              null,
    constraint bonuses_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint bonuses_ibfk_2
        foreign key (created_by) references users (id),
    constraint bonuses_ibfk_3
        foreign key (approved_by) references users (id)
);

create index approved_by
    on bonuses (approved_by);

create index created_by
    on bonuses (created_by);

create index employee_id
    on bonuses (employee_id);

create table contracts
(
    id            bigint auto_increment
        primary key,
    employee_id   bigint                                not null,
    contract_type varchar(50)                           not null,
    start_date    date                                  not null,
    end_date      date                                  null,
    document_url  varchar(500)                          null,
    status        varchar(30) default 'ACTIVE'          null,
    note          text                                  null,
    created_at    datetime    default CURRENT_TIMESTAMP null,
    created_by    bigint                                null,
    approved_by   bigint                                null,
    approved_at   datetime                              null,
    constraint contracts_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint contracts_ibfk_2
        foreign key (created_by) references users (id),
    constraint contracts_ibfk_3
        foreign key (approved_by) references users (id)
);

create index approved_by
    on contracts (approved_by);

create index created_by
    on contracts (created_by);

create index employee_id
    on contracts (employee_id);

alter table departments
    add constraint departments_ibfk_2
        foreign key (manager_id) references employees (id);

create table employee_bank_details
(
    id             bigint auto_increment
        primary key,
    employee_id    bigint                               not null,
    bank_name      varchar(150)                         not null,
    branch_name    varchar(150)                         null,
    account_name   varchar(150)                         not null,
    account_number varchar(255)                         not null,
    is_primary     tinyint(1) default 1                 null,
    created_at     datetime   default CURRENT_TIMESTAMP null,
    updated_at     datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint employee_bank_details_ibfk_1
        foreign key (employee_id) references employees (id)
            on delete cascade
);

create index employee_id
    on employee_bank_details (employee_id);

create table employee_benefits
(
    id                bigint auto_increment
        primary key,
    employee_id       bigint                             not null,
    benefit_type_id   bigint                             not null,
    value_amount      decimal(15, 2)                     null,
    value_description varchar(255)                       null,
    effective_from    date                               not null,
    effective_to      date                               null,
    created_at        datetime default CURRENT_TIMESTAMP null,
    created_by        bigint                             null,
    constraint employee_benefits_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint employee_benefits_ibfk_2
        foreign key (benefit_type_id) references benefit_types (id),
    constraint employee_benefits_ibfk_3
        foreign key (created_by) references users (id)
);

create index benefit_type_id
    on employee_benefits (benefit_type_id);

create index created_by
    on employee_benefits (created_by);

create index employee_id
    on employee_benefits (employee_id);

create table employee_kpis
(
    id              bigint auto_increment
        primary key,
    employee_id     bigint                                   not null,
    period varchar (30) not null,
    kpi_name        varchar(200)                             not null,
    weight          decimal(5, 2)  default 0.00              null,
    target_value    decimal(15, 2)                           not null,
    achieved_value  decimal(15, 2) default 0.00              null,
    completion_rate decimal(5, 2) as ((case
                                           when (`target_value` > 0) then ((`achieved_value` / `target_value`) * 100)
                                           else 0 end)) stored,
    status          varchar(30)    default 'IN_PROGRESS'     null,
    created_at      datetime       default CURRENT_TIMESTAMP null,
    updated_at      datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint employee_kpis_ibfk_1
        foreign key (employee_id) references employees (id)
);

create index employee_id
    on employee_kpis (employee_id);

create table employee_leave_balances
(
    id             bigint auto_increment
        primary key,
    employee_id    bigint                                  not null,
    year           int                                     not null,
    total_days     decimal(5, 2) default 12.00             not null,
    used_days      decimal(5, 2) default 0.00              not null,
    pending_days   decimal(5, 2) default 0.00              not null,
    remaining_days decimal(5, 2) as (((`total_days` - `used_days`) - `pending_days`)) stored,
    updated_at     datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_leave_emp_year
        unique (employee_id, year),
    constraint employee_leave_balances_ibfk_1
        foreign key (employee_id) references employees (id)
);

create index idx_leave_balance_year
    on employee_leave_balances (year);

create table employee_skills
(
    employee_id bigint                             not null,
    skill_id    bigint                             not null,
    proficiency int                                not null,
    verified_by bigint                             null,
    updated_at  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    primary key (employee_id, skill_id),
    constraint employee_skills_ibfk_1
        foreign key (employee_id) references employees (id)
            on delete cascade,
    constraint employee_skills_ibfk_2
        foreign key (skill_id) references skills (id)
            on delete cascade,
    constraint employee_skills_ibfk_3
        foreign key (verified_by) references users (id)
);

create index skill_id
    on employee_skills (skill_id);

create index verified_by
    on employee_skills (verified_by);

create index department_id
    on employees (department_id);

create index idx_emp_code
    on employees (employee_code);

create index idx_emp_status
    on employees (status);

create index line_manager_id
    on employees (line_manager_id);

create index position_id
    on employees (position_id);

create table events
(
    id            bigint auto_increment
        primary key,
    title         varchar(200)                         not null,
    description   text                                 null,
    start_date    date                                 not null,
    end_date      date                                 null,
    start_time    time                                 null,
    end_time      time                                 null,
    type          varchar(50)                          null,
    color         varchar(20)                          null,
    is_all_day    tinyint(1) default 0                 null,
    created_by    bigint                               null,
    department_id bigint                               null,
    created_at    datetime   default CURRENT_TIMESTAMP null,
    updated_at    datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    status        varchar(30)                          null,
    constraint events_ibfk_1
        foreign key (created_by) references users (id),
    constraint events_ibfk_2
        foreign key (department_id) references departments (id)
);

create index created_by
    on events (created_by);

create index department_id
    on events (department_id);

create table hr_reports
(
    id              bigint auto_increment
        primary key,
    file_path       varchar(255) not null,
    format          varchar(10)  not null,
    generated_at    datetime(6)  null,
    generated_by_id bigint       null,
    is_published    bit          null,
    published_at    datetime(6)  null,
    remarks         text         null,
    report_type     varchar(50)  not null,
    status          varchar(30)  not null,
    title           varchar(100) not null,
    constraint FKfe8jt772fdutgd8bv2n3u8loe
        foreign key (generated_by_id) references employees (id)
);

create table otp_verifications
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                               null,
    email      varchar(150)                         null,
    code       varchar(10)                          not null,
    type       varchar(30)                          not null comment 'REGISTER, RESET_PASSWORD',
    expired_at datetime                             not null,
    is_used    tinyint(1) default 0                 null,
    created_at datetime   default CURRENT_TIMESTAMP null,
    constraint otp_verifications_ibfk_1
        foreign key (user_id) references users (id)
);

create index user_id
    on otp_verifications (user_id);

create table performance_reviews
(
    id                bigint auto_increment
        primary key,
    employee_id       bigint                                not null,
    reviewer_id       bigint                                not null,
    review_period     varchar(50)                           not null,
    performance_score decimal(3, 2)                         not null,
    potential_score   decimal(3, 2)                         not null,
    talent_matrix     varchar(50) as ((case
                                           when ((`performance_score` >= 4.0) and (`potential_score` >= 4.0))
                                               then _utf8mb4'Star'
                                           when ((`performance_score` >= 4.0) and (`potential_score` >= 2.5))
                                               then _utf8mb4'High Performer'
                                           when ((`performance_score` >= 4.0) and (`potential_score` < 2.5))
                                               then _utf8mb4'Workhorse'
                                           when ((`performance_score` >= 2.5) and (`potential_score` >= 4.0))
                                               then _utf8mb4'High Potential'
                                           when ((`performance_score` >= 2.5) and (`potential_score` >= 2.5))
                                               then _utf8mb4'Core Employee'
                                           when ((`performance_score` >= 2.5) and (`potential_score` < 2.5))
                                               then _utf8mb4'Effective'
                                           when ((`performance_score` < 2.5) and (`potential_score` >= 4.0))
                                               then _utf8mb4'Problem Child'
                                           when ((`performance_score` < 2.5) and (`potential_score` >= 2.5))
                                               then _utf8mb4'Inconsistent'
                                           else _utf8mb4'Underperformer' end)) stored,
    strengths         text                                  null,
    areas_to_improve  text                                  null,
    status            varchar(30) default 'DRAFT'           null,
    created_at        datetime    default CURRENT_TIMESTAMP null,
    updated_at        datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_review_emp_period
        unique (employee_id, review_period),
    constraint performance_reviews_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint performance_reviews_ibfk_2
        foreign key (reviewer_id) references employees (id)
);

create index reviewer_id
    on performance_reviews (reviewer_id);

create table requests
(
    id                  bigint auto_increment
        primary key,
    employee_id         bigint                                not null,
    request_type_id     bigint                                not null,
    title               varchar(200)                          null,
    content             text                                  null,
    other_detail        varchar(255)                          null,
    start_date          datetime                              null,
    end_date            datetime                              null,
    is_urgent           tinyint(1)  default 0                 null comment 'TRUE if the request needs immediate attention',
    status              varchar(30) default 'PENDING'         null,
    step                varchar(50) default 'WAITING_DM'      null,
    rejected_reason     text                                  null,
    current_approver_id bigint                                null comment 'Người đang cần xử lý request hiện tại',
    dm_approver_id      bigint                                null comment 'Department Manager Approver',
    hrm_approver_id     bigint                                null comment 'HR Manager Approver',
    hr_processor_id     bigint                                null comment 'HR Processor/Executor',
    approved_by         bigint                                null,
    approved_at         datetime                              null,
    created_at          datetime    default CURRENT_TIMESTAMP null,
    updated_at          datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    leave_from          date                                  null,
    leave_to            date                                  null,
    leave_type          varchar(50)                           null,
    priority            varchar(20) default 'NORMAL'          null,
    priority_score      int         default 0                 null,
    constraint fk_req_dm
        foreign key (dm_approver_id) references users (id),
    constraint fk_req_hr
        foreign key (hr_processor_id) references users (id),
    constraint fk_req_hrm
        foreign key (hrm_approver_id) references users (id),
    constraint requests_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint requests_ibfk_2
        foreign key (request_type_id) references request_types (id),
    constraint requests_ibfk_3
        foreign key (current_approver_id) references users (id),
    constraint requests_ibfk_4
        foreign key (approved_by) references users (id)
);

create table job_posts
(
    id            bigint auto_increment
        primary key,
    request_id    bigint                                null comment 'Linked to HR_RECRUIT request',
    title         varchar(150)                          null,
    department_id bigint                                null,
    position_id   bigint                                null,
    description   text                                  null,
    requirements  text                                  null,
    benefits      text                                  null,
    salary_min    decimal(15, 2)                        null,
    salary_max    decimal(15, 2)                        null,
    status        varchar(30) default 'OPEN'            null,
    open_date     date                                  null,
    close_date    date                                  null,
    created_by    bigint                                null,
    created_at    datetime    default CURRENT_TIMESTAMP null,
    constraint fk_job_request
        foreign key (request_id) references requests (id),
    constraint job_posts_ibfk_1
        foreign key (department_id) references departments (id),
    constraint job_posts_ibfk_2
        foreign key (position_id) references positions (id),
    constraint job_posts_ibfk_3
        foreign key (created_by) references users (id)
);

create table applications
(
    id             bigint auto_increment
        primary key,
    job_post_id    bigint                                null,
    candidate_id   bigint                                null,
    cv_id          bigint                                null,
    status         varchar(30) default 'APPLIED'         null,
    applied_at     datetime    default CURRENT_TIMESTAMP null,
    tracking_token varchar(120)                          null,
    constraint UKjsup1i6thk7mov3fxf0upplx4
        unique (tracking_token),
    constraint uk_job_candidate
        unique (job_post_id, candidate_id),
    constraint applications_ibfk_1
        foreign key (job_post_id) references job_posts (id),
    constraint applications_ibfk_2
        foreign key (candidate_id) references candidates (id),
    constraint applications_ibfk_3
        foreign key (cv_id) references candidate_cvs (id)
);

create table application_stages
(
    id             bigint auto_increment
        primary key,
    application_id bigint                             null,
    stage_name     varchar(50)                        null,
    changed_at     datetime default CURRENT_TIMESTAMP null,
    note           text                               null,
    constraint application_stages_ibfk_1
        foreign key (application_id) references applications (id)
);

create index application_id
    on application_stages (application_id);

create index candidate_id
    on applications (candidate_id);

create index cv_id
    on applications (cv_id);

create table interview_assignments
(
    id             bigint auto_increment
        primary key,
    application_id bigint      not null,
    assigned_at    datetime(6) null,
    assigned_by    bigint      null,
    interviewer_id bigint      not null,
    constraint UK6xy8avir5bhdsdmqj0s96dac0
        unique (application_id, interviewer_id),
    constraint FK9d0lav3nis2pijhcbq0qo15dk
        foreign key (interviewer_id) references users (id),
    constraint FKkotqwo79smdgbplr4vyfiucny
        foreign key (application_id) references applications (id)
);

create table interviews
(
    id             bigint auto_increment
        primary key,
    application_id bigint       null,
    scheduled_at   datetime     null,
    location       varchar(255) null,
    interviewer_id bigint       null,
    status         varchar(30)  null,
    feedback       text         null,
    constraint interviews_ibfk_1
        foreign key (application_id) references applications (id),
    constraint interviews_ibfk_2
        foreign key (interviewer_id) references users (id)
);

create index application_id
    on interviews (application_id);

create index interviewer_id
    on interviews (interviewer_id);

create index created_by
    on job_posts (created_by);

create index department_id
    on job_posts (department_id);

create index position_id
    on job_posts (position_id);

create table offers
(
    id             bigint auto_increment
        primary key,
    application_id bigint                             null,
    salary_offered decimal(15, 2)                     null,
    start_date     date                               null,
    status         varchar(30)                        null,
    created_at     datetime default CURRENT_TIMESTAMP null,
    constraint offers_ibfk_1
        foreign key (application_id) references applications (id)
);

create index application_id
    on offers (application_id);

create table request_approval_history
(
    id          bigint auto_increment
        primary key,
    request_id  bigint                             not null,
    approver_id bigint                             not null,
    action      varchar(30)                        not null comment 'APPROVED, REJECTED, FORWARDED, CANCELLED',
    comment     text                               null,
    action_at   datetime default CURRENT_TIMESTAMP null,
    constraint request_approval_history_ibfk_1
        foreign key (request_id) references requests (id)
            on delete cascade,
    constraint request_approval_history_ibfk_2
        foreign key (approver_id) references users (id)
);

create index approver_id
    on request_approval_history (approver_id);

create index request_id
    on request_approval_history (request_id);

create index approved_by
    on requests (approved_by);

create index current_approver_id
    on requests (current_approver_id);

create index idx_requests_created_at
    on requests (created_at desc);

create index idx_requests_employee_id
    on requests (employee_id);

create index idx_requests_leave_dates
    on requests (leave_from, leave_to);

create index idx_requests_priority
    on requests (priority);

create index idx_requests_status
    on requests (status);

create index idx_requests_status_priority
    on requests (status, priority);

create index idx_requests_step
    on requests (step);

create index idx_requests_type_status
    on requests (request_type_id, status);

create index idx_requests_updated_at
    on requests (updated_at desc);

create index idx_requests_urgent
    on requests (is_urgent);

create table reward_disciplines
(
    id            bigint auto_increment
        primary key,
    employee_id   bigint                                   not null,
    record_type   varchar(30)                              not null,
    title         varchar(200)                             not null,
    description   text                                     null,
    decision_date date                                     not null,
    amount        decimal(15, 2) default 0.00              null,
    decided_by    bigint                                   null,
    created_at    datetime       default CURRENT_TIMESTAMP null,
    constraint reward_disciplines_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint reward_disciplines_ibfk_2
        foreign key (decided_by) references users (id)
);

create index decided_by
    on reward_disciplines (decided_by);

create index employee_id
    on reward_disciplines (employee_id);

create table salaries
(
    id               bigint auto_increment
        primary key,
    employee_id      bigint                                   not null,
    base_amount      decimal(15, 2)                           not null,
    allowance_amount decimal(15, 2) default 0.00              null,
    effective_from   date                                     not null,
    effective_to     date                                     null,
    salary_type      varchar(30)    default 'MONTHLY'         null,
    note             text                                     null,
    created_at       datetime       default CURRENT_TIMESTAMP null,
    created_by       bigint                                   null,
    constraint salaries_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint salaries_ibfk_2
        foreign key (created_by) references users (id)
);

create index created_by
    on salaries (created_by);

create index employee_id
    on salaries (employee_id);

create table staffing_requests
(
    id                       bigint auto_increment
        primary key,
    assigned_employee_id     bigint       null,
    created_at               datetime(6)  null,
    department_id            bigint       not null,
    description              text         null,
    processed_at             datetime(6)  null,
    processed_by_user_id     bigint       null,
    request_type             varchar(50)  not null,
    requested_by_employee_id bigint       not null,
    role_requested           varchar(200) not null,
    status                   varchar(30)  not null,
    updated_at               datetime(6)  null,
    constraint FK5xtddodjq1lxxvn7qp6jsh2xi
        foreign key (assigned_employee_id) references employees (id),
    constraint FK6co4476kgkwyoiitpwca8mkcc
        foreign key (department_id) references departments (id),
    constraint FKiq3eg9o7f4uxtbafny8u4r2nw
        foreign key (requested_by_employee_id) references employees (id),
    constraint FKq9f8vasw2dy74e4fhlwu304lw
        foreign key (processed_by_user_id) references users (id)
);

create table system_settings
(
    id            bigint auto_increment
        primary key,
    setting_key   varchar(100)                       not null,
    setting_value text                               null,
    description   varchar(255)                       null,
    updated_at    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    updated_by    bigint                             null,
    constraint setting_key
        unique (setting_key),
    constraint system_settings_ibfk_1
        foreign key (updated_by) references users (id)
);

create index updated_by
    on system_settings (updated_by);

create table tasks
(
    id           bigint auto_increment
        primary key,
    title        varchar(200)                          not null,
    description  text                                  null,
    assigned_to  bigint                                not null,
    assigned_by  bigint                                not null,
    due_date     date                                  null,
    status       varchar(30) default 'PENDING'         null,
    is_urgent    tinyint(1)  default 0                 null,
    completed_at datetime                              null,
    created_at   datetime    default CURRENT_TIMESTAMP null,
    updated_at   datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    priority     varchar(20)                           null,
    constraint tasks_ibfk_1
        foreign key (assigned_to) references users (id),
    constraint tasks_ibfk_2
        foreign key (assigned_by) references users (id)
);

create index assigned_by
    on tasks (assigned_by);

create index assigned_to
    on tasks (assigned_to);

create table timesheet_periods
(
    id          bigint auto_increment
        primary key,
    period_name varchar(100)         null,
    start_date  date                 not null,
    end_date    date                 not null,
    is_locked   tinyint(1) default 0 null,
    locked_at   datetime             null,
    locked_by   bigint               null,
    constraint timesheet_periods_ibfk_1
        foreign key (locked_by) references users (id)
);

create table payslips
(
    id                 bigint auto_increment
        primary key,
    employee_id        bigint                        not null,
    period_id          bigint                        not null,
    total_gross_salary decimal(15, 2)                null,
    actual_base_salary decimal(15, 2)                null,
    total_ot_amount    decimal(15, 2)                null,
    total_bonus        decimal(15, 2)                null,
    total_deduction    decimal(15, 2)                null,
    net_salary         decimal(15, 2)                null,
    status             varchar(30) default 'PENDING' null,
    approved_by        bigint                        null,
    payment_date       date                          null,
    payment_reference  varchar(100)                  null,
    constraint payslips_ibfk_1
        foreign key (employee_id) references employees (id),
    constraint payslips_ibfk_2
        foreign key (period_id) references timesheet_periods (id),
    constraint payslips_ibfk_3
        foreign key (approved_by) references users (id)
);

create table payslip_line_items
(
    id               bigint auto_increment
        primary key,
    payslip_id       bigint         not null,
    pay_component_id bigint         not null,
    description      varchar(255)   null,
    amount           decimal(15, 2) not null,
    type             varchar(30)    not null,
    constraint payslip_line_items_ibfk_1
        foreign key (payslip_id) references payslips (id)
            on delete cascade,
    constraint payslip_line_items_ibfk_2
        foreign key (pay_component_id) references pay_components (id)
);

create index pay_component_id
    on payslip_line_items (pay_component_id);

create index payslip_id
    on payslip_line_items (payslip_id);

create index approved_by
    on payslips (approved_by);

create index employee_id
    on payslips (employee_id);

create index period_id
    on payslips (period_id);

create index idx_period_dates
    on timesheet_periods (start_date, end_date);

create index locked_by
    on timesheet_periods (locked_by);

create table user_roles
(
    user_id    bigint                             not null,
    role_id    bigint                             not null,
    granted_at datetime default CURRENT_TIMESTAMP null,
    granted_by bigint                             null,
    primary key (user_id, role_id),
    constraint user_roles_ibfk_1
        foreign key (user_id) references users (id)
            on delete cascade,
    constraint user_roles_ibfk_2
        foreign key (role_id) references roles (id),
    constraint user_roles_ibfk_3
        foreign key (granted_by) references users (id)
);

create index granted_by
    on user_roles (granted_by);

create index role_id
    on user_roles (role_id);

create index idx_users_status
    on users (status);

create procedure ConvertToRealNames()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_id BIGINT;
    DECLARE v_last VARCHAR(50);
    DECLARE v_mid VARCHAR(50);
    DECLARE v_first VARCHAR(50);
    DECLARE v_full_name VARCHAR(150);
    DECLARE v_username VARCHAR(100);
    DECLARE v_email VARCHAR(150);
    
    -- ULTRA SAFE: Only target auto-generated users AND strictly ignore your first 18 original users!
    DECLARE cur CURSOR FOR SELECT id FROM users WHERE username LIKE 'user_%' AND id > 18;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

OPEN cur;

read_loop: LOOP
        FETCH cur INTO v_id;
        IF done THEN
            LEAVE read_loop;
END IF;

        SET v_last = ELT(FLOOR(RAND() * 10) + 1, 'Nguyen', 'Tran', 'Le', 'Pham', 'Hoang', 'Phan', 'Vu', 'Vo', 'Dang', 'Bui');
        SET v_mid = ELT(FLOOR(RAND() * 10) + 1, 'Van', 'Thi', 'Huu', 'Ngoc', 'Minh', 'Xuan', 'Thanh', 'Quang', 'Bao', 'Gia');
        SET v_first = ELT(FLOOR(RAND() * 15) + 1, 'Anh', 'Khoa', 'Dat', 'Dung', 'Huy', 'Linh', 'Trang', 'Hoa', 'Tuan', 'Phong', 'Thao', 'Ngan', 'Kien', 'Hieu', 'Nhi');
        
        SET v_full_name = CONCAT(v_last, ' ', v_mid, ' ', v_first);
        SET v_username = LOWER(CONCAT(v_first, '.', SUBSTRING(v_last, 1, 1), '.', v_id));
        SET v_email = CONCAT(v_username, '@company.vn');

UPDATE users
SET full_name = v_full_name,
    username = v_username,
    email = v_email
WHERE id = v_id;

END LOOP;

CLOSE cur;

SELECT '✅ Data successfully updated! Your original 18 users were NOT touched.' AS Result;
END;

create procedure GenerateFullSystemData(IN num_employees int, IN num_candidates int)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE k INT DEFAULT 1;
    DECLARE req_idx INT;
    DECLARE att_idx INT;
    
    DECLARE v_user_id, v_emp_id, v_req_id, v_candidate_id, v_app_id BIGINT;
    DECLARE v_dept_id, v_pos_id BIGINT;
    DECLARE v_email VARCHAR(150);
    DECLARE v_salary DECIMAL(15,2);
    
    -- Dynamic ID fetchers to prevent Foreign Key crashes
    DECLARE v_emp_role_id BIGINT;
    DECLARE v_admin_id BIGINT;
    
    -- Get the actual ID of the 'EMPLOYEE' role and 'admin' user from your database
SELECT id INTO v_emp_role_id FROM roles WHERE code = 'EMPLOYEE' LIMIT 1;
SELECT id INTO v_admin_id FROM users WHERE username = 'admin' LIMIT 1;

-- Fallback safety if not found
IF v_emp_role_id IS NULL THEN SET v_emp_role_id = 1; END IF;
    IF v_admin_id IS NULL THEN SET v_admin_id = 1; END IF;

    SET autocommit = 0; 

    -- ==========================================================
    -- A. LOOP: GENERATE CANDIDATES & RECRUITMENT FLOW
    -- ==========================================================
    WHILE k <= num_candidates DO
        SET v_email = CONCAT('candidate_', UNIX_TIMESTAMP(), '_', k, '@gmail.com');

INSERT INTO candidates (full_name, email, phone, headline, summary, years_experience, expected_salary)
VALUES (CONCAT('Candidate ', k), v_email, CONCAT('08', LPAD(k, 8, '0')), 'Software Engineer', 'Experienced in web development', FLOOR(RAND()*5)+1, 15000000 + (RAND()*10000000));
SET v_candidate_id = LAST_INSERT_ID();

INSERT INTO candidate_cvs (candidate_id, file_name, file_path)
VALUES (v_candidate_id, CONCAT('CV_', v_candidate_id, '.pdf'), CONCAT('/uploads/cv/', v_candidate_id, '.pdf'));

INSERT INTO applications (job_post_id, candidate_id, cv_id, status)
VALUES ((k % 2) + 1, v_candidate_id, LAST_INSERT_ID(), CASE WHEN k%4=0 THEN 'HIRED' WHEN k%3=0 THEN 'INTERVIEWING' ELSE 'APPLIED' END);
SET v_app_id = LAST_INSERT_ID();

INSERT INTO application_stages (application_id, stage_name, note) VALUES (v_app_id, 'CV Review', 'CV checked, passed initial screening');

IF (k % 3 = 0) THEN
            INSERT INTO application_stages (application_id, stage_name, note) VALUES (v_app_id, 'Technical Interview', 'First round interview');
INSERT INTO interviews (application_id, scheduled_at, location, interviewer_id, status, feedback)
VALUES (v_app_id, DATE_ADD(NOW(), INTERVAL 2 DAY), 'Meeting Room 1', v_admin_id, 'COMPLETED', 'Candidate performed well');
END IF;

        IF (k % 4 = 0) THEN
            INSERT INTO offers (application_id, salary_offered, start_date, status)
            VALUES (v_app_id, 20000000, '2026-04-01', 'ACCEPTED');
END IF;

        SET k = k + 1;
END WHILE;

    -- ==========================================================
    -- B. LOOP: GENERATE EMPLOYEES & HR OPERATIONS
    -- ==========================================================
    WHILE i <= num_employees DO
        SET v_email = CONCAT('emp_', UNIX_TIMESTAMP(), '_', i, '@hrm.local');
        SET v_dept_id = (i % 5) + 1;
        SET v_pos_id = (i % 10) + 1;
        SET v_salary = FLOOR(10000000 + RAND() * 20000000);

INSERT INTO users (username, email, password_hash, full_name, phone, status, is_verified)
VALUES (CONCAT('user_', UNIX_TIMESTAMP(), '_', i), v_email, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', CONCAT('Employee ', i), CONCAT('09', LPAD(i, 8, '0')), 'ACTIVE', 1);
SET v_user_id = LAST_INSERT_ID();
        
        -- DYNAMICALLY assign the Employee role
INSERT INTO user_roles (user_id, role_id) VALUES (v_user_id, v_emp_role_id);

INSERT INTO otp_verifications (user_id, email, code, type, expired_at, is_used)
VALUES (v_user_id, v_email, '123456', 'REGISTER', DATE_ADD(NOW(), INTERVAL 10 MINUTE), 1);

INSERT INTO audit_logs (entity_name, entity_id, action, performed_by)
VALUES ('users', v_user_id, 'CREATE_ACCOUNT', v_admin_id);

INSERT INTO employees (user_id, employee_code, department_id, position_id, line_manager_id, hire_date, status)
VALUES (v_user_id, CONCAT('EMP', LPAD(i, 6, '0')), v_dept_id, v_pos_id, NULL, DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 500) DAY), 'ACTIVE');
SET v_emp_id = LAST_INSERT_ID();

INSERT INTO account_requests (employee_id, proposed_role_id, requested_by, approved_by, status)
VALUES (v_emp_id, v_emp_role_id, v_admin_id, v_admin_id, 'APPROVED');

INSERT INTO contracts (employee_id, contract_type, start_date, status, approved_by)
VALUES (v_emp_id, 'INDEFINITE', '2025-01-01', 'ACTIVE', v_admin_id);

INSERT INTO salaries (employee_id, base_amount, allowance_amount, effective_from)
VALUES (v_emp_id, v_salary, 1000000, '2025-01-01');

INSERT INTO bonuses (employee_id, bonus_type, amount, bonus_date, status, approved_by)
VALUES (v_emp_id, 'PERFORMANCE_BONUS', 5000000, '2026-01-25', 'APPROVED', v_admin_id);

INSERT INTO employee_benefits (employee_id, benefit_type_id, value_amount, effective_from)
VALUES (v_emp_id, 1, 800000, '2025-01-01');

INSERT INTO employee_skills (employee_id, skill_id, proficiency, verified_by)
VALUES (v_emp_id, (i % 4) + 1, FLOOR(RAND() * 5) + 1, v_admin_id);

INSERT INTO employee_kpis (employee_id, period, kpi_name, weight, target_value, achieved_value, status)
VALUES (v_emp_id, 'Q1-2026', 'Deliver project on time', 50, 100, FLOOR(RAND() * 50) + 50, 'IN_PROGRESS');

INSERT INTO performance_reviews (employee_id, reviewer_id, review_period, performance_score, potential_score, status)
VALUES (v_emp_id, v_emp_id, 'YEAR_2025', 2.0 + (RAND() * 3.0), 2.0 + (RAND() * 3.0), 'PUBLISHED');

IF (i % 10 = 0) THEN
            INSERT INTO reward_disciplines (employee_id, record_type, title, decision_date, amount, decided_by)
            VALUES (v_emp_id, 'REWARD', 'Employee of the Month', '2026-02-15', 2000000, v_admin_id);
END IF;

INSERT INTO tasks (title, assigned_to, assigned_by, due_date, status)
VALUES (CONCAT('Complete Module ', i), v_user_id, v_admin_id, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'IN_PROGRESS');

SET req_idx = 1;
        WHILE req_idx <= 3 DO
            INSERT INTO requests (employee_id, request_type_id, title, content, start_date, end_date, status, current_approver_id)
            VALUES (v_emp_id, (req_idx % 10) + 1, CONCAT('Request Ticket #', req_idx), 'Auto-generated request content...', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 DAY), CASE WHEN req_idx=1 THEN 'APPROVED' ELSE 'PENDING' END, CASE WHEN req_idx=1 THEN NULL ELSE v_admin_id END);
            SET v_req_id = LAST_INSERT_ID();

INSERT INTO request_approval_history (request_id, approver_id, action, comment)
VALUES (v_req_id, v_admin_id, CASE WHEN req_idx=1 THEN 'APPROVED' ELSE 'FORWARDED' END, 'Checked and verified');

SET req_idx = req_idx + 1;
END WHILE;

        SET att_idx = 1;
        WHILE att_idx <= 10 DO
            INSERT INTO attendance (employee_id, work_date, check_in, check_out, status)
            VALUES (v_emp_id, DATE_SUB(CURDATE(), INTERVAL att_idx DAY), '08:00:00', '17:30:00', 'PRESENT');
            SET att_idx = att_idx + 1;
END WHILE;

INSERT INTO payslips (employee_id, period_id, actual_base_salary, total_ot_amount, total_bonus, total_deduction, net_salary, status)
VALUES (v_emp_id, 1, v_salary, 0, 0, 500000, v_salary - 500000, 'PAID');

INSERT INTO email_logs (recipient_email, template_code, status)
VALUES (v_email, 'WELCOME_EMAIL', 'SENT');

IF i % 200 = 0 THEN
            COMMIT;
END IF;

        SET i = i + 1;
END WHILE;

COMMIT;
SET autocommit = 1;

SELECT CONCAT('✅ SUCCESS! Generated data for ', num_employees, ' employees and ', num_candidates, ' candidates.') AS Result;
END;

