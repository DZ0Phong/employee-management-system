package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.User;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeptManagerService {

        private final DepartmentRepository departmentRepository;
        private final EmployeeRepository employeeRepository;
        private final UserRepository userRepository;
        private final com.group5.ems.repository.RequestRepository requestRepository;

        private User getCurrentUser() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated())
                        return null;
                return userRepository.findByUsername(auth.getName()).orElse(null);
        }

        private Map<String, String> getManagerMap(User user) {
                Map<String, String> manager = new HashMap<>();
                if (user != null) {
                        manager.put("name", user.getFullName());
                        // Simplistic role derivation
                        manager.put("role", "Department Manager");
                        manager.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl()
                                        : "https://lh3.googleusercontent.com/aida-public/AB6AXuDdPhGMiAuUVMrhgqJVFn7WwuSwOLn9a730wH2usyu4spUNv9xdN6YBAMA1bABIftIBnWGbN4ZOta3fvUqdmQRPhCb4JMMxyVITyF3CXo6Srgkl9jI21MbXolPsVUwxgExfg-52F2HuTfQ6J4o8bqtfvprk6ikOWkdiJjTsXS_uWawAu1l8WErbThrS0pmx91Dh6uDOoITamBraDKhrQ9er2LfexZBrhboZh3DcLncMTsqT9CZHa9SHfcD8lEFBXDC8zmwPLNHkISk");
                }
                return manager;
        }

        private Department getDepartmentForManager(User user) {
                if (user == null)
                        return null;
                Employee managerEmp = employeeRepository.findByUserId(user.getId()).orElse(null);
                if (managerEmp == null)
                        return null;

                List<Department> managedDepts = departmentRepository.findByManagerId(managerEmp.getId());
                if (!managedDepts.isEmpty()) {
                        return managedDepts.get(0);
                }
                return managerEmp.getDepartment();
        }

        public int getTeamSize(Long managerId) {
                return departmentRepository.findByManagerId(managerId).size();
        }

        public Map<String, Object> getDashboardMockData() {
                Map<String, Object> data = new HashMap<>();
                User currentUser = getCurrentUser();
                data.put("manager", getManagerMap(currentUser));

                Department dept = getDepartmentForManager(currentUser);
                int teamSize = 0;
                int activeCount = 0;
                int inactiveCount = 0;
                int suspendedCount = 0;

                data.put("newApprovals", 3); // Metrics will need actual calculation logic later
                data.put("pendingApprovals", 8);
                data.put("teamAttendance", "92%");
                data.put("nextReview", "Oct 15");

                List<Map<String, String>> activities = new ArrayList<>();
                if (dept != null && dept.getEmployees() != null) {
                        teamSize = dept.getEmployees().size();
                        int count = 0;
                        for (Employee emp : dept.getEmployees()) {
                                String status = emp.getStatus();
                                if ("ACTIVE".equalsIgnoreCase(status)) {
                                        activeCount++;
                                } else if ("ON_LEAVE".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
                                        inactiveCount++;
                                } else {
                                        suspendedCount++;
                                }

                                if (count < 5) {
                                        User empUser = emp.getUser();
                                Map<String, String> map = new HashMap<>();
                                map.put("name", empUser != null ? empUser.getFullName() : "Employee #" + emp.getId());
                                map.put("title", "Staff");
                                map.put("avatarUrl", empUser != null && empUser.getAvatarUrl() != null
                                                ? empUser.getAvatarUrl()
                                                : "https://lh3.googleusercontent.com/aida-public/AB6AXuC5CGURyOdKZg1nPS5tSotOqt9DZB78eRGH3eg3EC-4AMB4rdDXy9jeQr4qOj7ChckGmlmMGLtDvSGTG-xjpnI3_Nfylvv685wy7ZZt-l-5S7goNULPP3vcdleWBtr01Pep3ZA9mtCX5hkoy6OkrIFGl8u9d9KfmPZQw2_aut4hAoECaycxw2Oz1wyAZA4-eKUk6Jbrk2Maoj0rN7YEVuVvYssm7pQ4monLETA3SqC89B2yDAt2DK9icMHRpTDW18H21JOyHXMqMQA");
                                map.put("status", emp.getStatus() != null ? emp.getStatus() : "Active");

                                String statusClass = "ACTIVE".equalsIgnoreCase(emp.getStatus())
                                                ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                                                : "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400";
                                map.put("statusClass", statusClass);

                                map.put("attendance", "98%");
                                map.put("lastReview", "Aug 12, 2023");
                                activities.add(map);
                                        count++;
                                }
                        }
                }

                data.put("teamSize", teamSize);
                data.put("activeCount", activeCount);
                data.put("inactiveCount", inactiveCount);
                data.put("suspendedCount", suspendedCount);
                data.put("recentTeamActivities", activities);

                return data;
        }

        public Map<String, Object> getTeamMockData() {
                Map<String, Object> data = new HashMap<>();
                User currentUser = getCurrentUser();
                data.put("manager", getManagerMap(currentUser));
                data.put("pendingApprovals", 8);
                data.put("newApprovals", 3);

                List<Map<String, String>> members = new ArrayList<>();
                Department dept = getDepartmentForManager(currentUser);

                if (dept != null && dept.getEmployees() != null) {
                        for (Employee emp : dept.getEmployees()) {
                                User empUser = emp.getUser();
                                Map<String, String> member = new HashMap<>();
                                member.put("name",
                                                empUser != null ? empUser.getFullName() : "Employee #" + emp.getId());
                                member.put("email", empUser != null ? empUser.getEmail() : "");
                                member.put("role", "Staff");
                                member.put("rating", "Meets Expectations");
                                member.put("ratingClass",
                                                "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400");
                                member.put("status", emp.getStatus() != null ? emp.getStatus() : "Active");
                                member.put("statusDot", "ACTIVE".equalsIgnoreCase(emp.getStatus()) ? "bg-green-500"
                                                : "bg-amber-500");
                                member.put("avatarUrl", empUser != null && empUser.getAvatarUrl() != null
                                                ? empUser.getAvatarUrl()
                                                : "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA");
                                members.add(member);
                        }
                }

                data.put("teamMembers", members);
                return data;
        }

        public Map<String, Object> getDepartmentMockData() {
                Map<String, Object> data = new HashMap<>();
                User currentUser = getCurrentUser();
                Map<String, String> managerMap = getManagerMap(currentUser);
                data.put("manager", managerMap);
                data.put("pendingApprovals", 8);
                data.put("newApprovals", 3);

                Department dept = getDepartmentForManager(currentUser);
                Map<String, String> department = new HashMap<>();
                List<Map<String, String>> teams = new ArrayList<>();
                List<Map<String, String>> positions = new ArrayList<>();

                if (dept != null) {
                        department.put("name", dept.getName() != null ? dept.getName() : "Unnamed Dept");
                        department.put("code", dept.getCode() != null ? dept.getCode() : "N/A");
                        department.put("description", dept.getDescription() != null ? dept.getDescription()
                                        : "Department operations run here.");
                        department.put("manager", managerMap.get("name"));
                        department.put("totalEmployees",
                                        dept.getEmployees() != null ? String.valueOf(dept.getEmployees().size()) : "0");
                        department.put("openPositions", "0");
                        department.put("budgetUtilization", "85%");

                        if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                                for (Department child : dept.getChildren()) {
                                        Map<String, String> team = new HashMap<>();
                                        team.put("name", child.getName());
                                        team.put("headcount",
                                                        child.getEmployees() != null
                                                                        ? String.valueOf(child.getEmployees().size())
                                                                        : "0");

                                        String leadName = "None";
                                        if (child.getManager() != null && child.getManager().getUser() != null) {
                                                leadName = child.getManager().getUser().getFullName();
                                        }
                                        team.put("lead", leadName);
                                        teams.add(team);
                                }
                        }

                        if (dept.getPositions() != null && !dept.getPositions().isEmpty()) {
                                for (Position pos : dept.getPositions()) {
                                        Map<String, String> posMap = new HashMap<>();
                                        posMap.put("title", pos.getName());
                                        posMap.put("headcount", "1"); // Placeholder
                                        posMap.put("status", "Filled"); // Placeholder
                                        posMap.put("statusClass", "bg-green-100 text-green-700");
                                        positions.add(posMap);
                                }
                        }
                } else {
                        // Provide empty fallback data if no department is found
                        department.put("name", "No Department Assigned");
                        department.put("code", "N/A");
                        department.put("description", "You are not currently managing any departments.");
                        department.put("manager", managerMap.get("name"));
                        department.put("totalEmployees", "0");
                        department.put("openPositions", "0");
                        department.put("budgetUtilization", "0%");
                }

                data.put("department", department);
                data.put("teams", teams);
                data.put("positions", positions);

                return data;
        }
        public Map<String, Object> getLeaveApprovalData() {
                Map<String, Object> data = new HashMap<>();
                User currentUser = getCurrentUser();
                Map<String, String> managerMap = getManagerMap(currentUser);
                data.put("manager", managerMap);

                Department dept = getDepartmentForManager(currentUser);
                List<com.group5.ems.entity.Request> requests = new ArrayList<>();
                if (dept != null) {
                        requests = requestRepository.findByEmployeeDepartmentIdAndLeaveTypeIsNotNullOrderByCreatedAtDesc(dept.getId());
                }

                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.YearMonth currentMonth = java.time.YearMonth.now();

                int pendingApprovals = 0;
                int approvedThisMonth = 0;
                int awayToday = 0;

                List<Map<String, Object>> mappedRequests = new ArrayList<>();

                for (com.group5.ems.entity.Request req : requests) {
                        // Metrics
                        if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                                pendingApprovals++;
                        } else if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                                if (req.getApprovedAt() != null && java.time.YearMonth.from(req.getApprovedAt()).equals(currentMonth)) {
                                        approvedThisMonth++;
                                }
                                if (req.getLeaveFrom() != null && req.getLeaveTo() != null) {
                                        if (!today.isBefore(req.getLeaveFrom()) && !today.isAfter(req.getLeaveTo())) {
                                                awayToday++;
                                        }
                                }
                        }

                        // Map to view data
                        Map<String, Object> reqMap = new HashMap<>();
                        reqMap.put("id", req.getId());

                        Map<String, String> empMap = new HashMap<>();
                        if (req.getEmployee() != null) {
                                empMap.put("name", req.getEmployee().getUser() != null ? req.getEmployee().getUser().getFullName() : "Unknown");
                                empMap.put("role", req.getEmployee().getPosition() != null ? req.getEmployee().getPosition().getName() : "Employee");
                                empMap.put("avatarUrl", req.getEmployee().getUser() != null && req.getEmployee().getUser().getAvatarUrl() != null 
                                        ? req.getEmployee().getUser().getAvatarUrl() 
                                        : "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA");
                        }
                        reqMap.put("employee", empMap);

                        // Friendly formatting
                        String typeStr = req.getLeaveType().replace("_", " ").toLowerCase();
                        if (typeStr.contains("annual")) {
                            reqMap.put("typeDisplay", "Annual Leave");
                            reqMap.put("typeColorClass", "bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-300 border-blue-100");
                            reqMap.put("typeDotClass", "bg-blue-500");
                        } else if (typeStr.contains("sick")) {
                            reqMap.put("typeDisplay", "Sick Leave");
                            reqMap.put("typeColorClass", "bg-rose-50 dark:bg-rose-900/30 text-rose-600 dark:text-rose-300 border-rose-100");
                            reqMap.put("typeDotClass", "bg-rose-500");
                        } else {
                            reqMap.put("typeDisplay", req.getLeaveType());
                            reqMap.put("typeColorClass", "bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 border-purple-100");
                            reqMap.put("typeDotClass", "bg-purple-500");
                        }

                        long durationDays = 0;
                        if (req.getLeaveFrom() != null && req.getLeaveTo() != null) {
                                durationDays = java.time.temporal.ChronoUnit.DAYS.between(req.getLeaveFrom(), req.getLeaveTo()) + 1;
                        }
                        reqMap.put("durationDays", durationDays);

                        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd");
                        String dates = "";
                        if (req.getLeaveFrom() != null) {
                                dates += req.getLeaveFrom().format(dtf);
                                if (req.getLeaveTo() != null && !req.getLeaveTo().equals(req.getLeaveFrom())) {
                                        dates += " - " + req.getLeaveTo().format(dtf);
                                }
                        }
                        reqMap.put("dates", dates);
                        reqMap.put("appliedOn", req.getCreatedAt() != null ? req.getCreatedAt().format(dtf) : "N/A");

                        reqMap.put("status", req.getStatus());
                        if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                                reqMap.put("statusClass", "bg-amber-50 dark:bg-amber-900/20 text-amber-600 border-amber-100");
                                reqMap.put("statusDisplay", "Pending Review");
                        } else if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                                reqMap.put("statusClass", "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 border-emerald-100");
                                reqMap.put("statusDisplay", "Approved");
                        } else {
                                reqMap.put("statusClass", "bg-rose-50 dark:bg-rose-900/20 text-rose-600 border-rose-100");
                                reqMap.put("statusDisplay", "Rejected");
                        }

                        mappedRequests.add(reqMap);
                }

                data.put("pendingApprovals", pendingApprovals);
                data.put("approvedThisMonth", approvedThisMonth);
                data.put("awayToday", awayToday);
                data.put("requests", mappedRequests);

                return data;
        }
}
