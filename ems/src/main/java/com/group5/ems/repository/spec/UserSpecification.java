package com.group5.ems.repository.spec;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Role;
import com.group5.ems.entity.User;
import com.group5.ems.entity.UserRole;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    //filter cho status
    //root là đại diện cho table được truyền vào từ Specification<?>
    public static Specification<User> hasKeyword(String keyword){
        if(keyword==null||keyword.isBlank()){
            return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        //criteriaBuilder là build condition
        //ở đây return là tìm trong table User(root) với điều kiện full name hoặc email
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
        );
    }

    public static Specification<User> hasStatus(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return (r, q, cb) -> cb.conjunction();
        }
        String normalized = statusFilter.trim().toLowerCase();

        // AllLocked = OR query cho cả LOCKED và LOCK5
        if ("alllocked".equals(normalized)) {
            return (r, q, cb) -> r.get("status").in("LOCKED", "LOCK5");
        }

        String dbStatus = switch (normalized) {
            case "active"   -> "ACTIVE";
            case "inactive" -> "INACTIVE";
            case "locked"   -> "LOCKED";
            case "lock5"    -> "LOCK5";
            default         -> null;
        };

        if (dbStatus == null) {
            return (r, q, cb) -> cb.conjunction();
        }
        final String finalStatus = dbStatus;
        return (r, q, cb) -> cb.equal(r.get("status"), finalStatus);
    }

    public static Specification<User> hasRoleCode(String roleCode){
        if(roleCode==null||roleCode.isBlank()){
            return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        return (root, query, cb) -> {
            query.distinct(true); // tránh duplicate khi user có nhiều role
            Join<User, UserRole> userRoleJoin = root.join("userRoles", JoinType.INNER);
            Join<UserRole, Role> roleJoin = userRoleJoin.join("role", JoinType.INNER);
            return cb.equal(roleJoin.get("code"), roleCode.trim());
        };
    }

    public static Specification<User> hasDepartment(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<User, Employee> empJoin = root.join("employee", JoinType.INNER);
            Join<Employee, Department> deptJoin = empJoin.join("department", JoinType.INNER);
            return cb.equal(deptJoin.get("name"), departmentName.trim());
        };
    }

    public static Specification<User> withFilters(String keyword, String statusFilter,
                                                   String roleCode, String departmentName) {
        return Specification.where(hasKeyword(keyword))
                .and(hasStatus(statusFilter))
                .and(hasRoleCode(roleCode))
                .and(hasDepartment(departmentName));
    }

    public static Specification<User> withFilters(String keyword, String statusFilter, String roleCode) {
        return withFilters(keyword, statusFilter, roleCode, null);
    }

}
