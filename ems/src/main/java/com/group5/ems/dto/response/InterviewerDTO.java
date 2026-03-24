package com.group5.ems.dto.response;

/**
 * Slim record representing one employee shown in the Assign Interviewer modal.
 *
 * Schema mapping:
 * id → users.id (FK used in interview_assignments.interviewer_id)
 * fullName → users.full_name
 * initials → derived from users.full_name (first char of each word, max 2)
 * employeeCode → employees.employee_code
 * jobTitle → positions.name (via employees.position_id → positions.id)
 * departmentName → departments.name (via employees.department_id →
 * departments.id)
 * avatarUrl → users.avatar_url (optional, used for avatar fallback in the UI)
 *
 * NOTE: employees.user_id is the join key to users.
 * interview_assignments.interviewer_id references users(id), NOT employees(id).
 * Therefore id = users.id, not employees.id.
 */
public record InterviewerDTO(

                Long id,
                String fullName,
                String initials,
                String employeeCode,
                String jobTitle,
                String departmentName,
                String avatarUrl

) {
}