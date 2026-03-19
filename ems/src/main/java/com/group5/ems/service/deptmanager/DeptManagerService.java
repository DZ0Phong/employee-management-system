package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Position;
import com.group5.ems.entity.User;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.repository.RequestRepository;
import com.group5.ems.repository.RequestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeptManagerService {

        private final DepartmentRepository departmentRepository;
        private final DeptManagerUtilService utilService;
        private final RequestRepository requestRepository;
        private final RequestTypeRepository requestTypeRepository;
        private final PositionRepository positionRepository;

        public int getTeamSize(Long managerId) {
                return departmentRepository.findByManagerId(managerId).size();
        }

        public Map<String, Object> getDashboardMockData() {
                Map<String, Object> data = new HashMap<>();
                User currentUser = utilService.getCurrentUser();
                data.put("manager", utilService.getManagerMap(currentUser));

                Department dept = utilService.getDepartmentForManager(currentUser);
                int teamSize = 0;
                int activeCount = 0;
                int inactiveCount = 0;
                int suspendedCount = 0;

                int pendingApprovals = utilService.getPendingApprovalsCount(dept);

                data.put("newApprovals", pendingApprovals);
                data.put("pendingApprovals", pendingApprovals);
                data.put("teamAttendance", "N/A"); // Needs Attendance implementation later
                data.put("nextReview", "N/A"); // Needs Performance Review implementation later

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
                                        map.put("title", emp.getPosition() != null ? emp.getPosition().getName() : "Staff");
                                        map.put("avatarUrl", empUser != null && empUser.getAvatarUrl() != null
                                                        ? empUser.getAvatarUrl()
                                                        : "https://lh3.googleusercontent.com/aida-public/AB6AXuC5CGURyOdKZg1nPS5tSotOqt9DZB78eRGH3eg3EC-4AMB4rdDXy9jeQr4qOj7ChckGmlmMGLtDvSGTG-xjpnI3_Nfylvv685wy7ZZt-l-5S7goNULPP3vcdleWBtr01Pep3ZA9mtCX5hkoy6OkrIFGl8u9d9KfmPZQw2_aut4hAoECaycxw2Oz1wyAZA4-eKUk6Jbrk2Maoj0rN7YEVuVvYssm7pQ4monLETA3SqC89B2yDAt2DK9icMHRpTDW18H21JOyHXMqMQA");
                                        map.put("status", emp.getStatus() != null ? emp.getStatus() : "Active");

                                        String statusClass = "ACTIVE".equalsIgnoreCase(emp.getStatus())
                                                        ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                                                        : "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400";
                                        map.put("statusClass", statusClass);

                                        map.put("attendance", "N/A");
                                        map.put("lastReview", "N/A");
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
                User currentUser = utilService.getCurrentUser();
                data.put("manager", utilService.getManagerMap(currentUser));
                Department dept = utilService.getDepartmentForManager(currentUser);
                
                int pendingApprovals = utilService.getPendingApprovalsCount(dept);
                data.put("pendingApprovals", pendingApprovals);
                data.put("newApprovals", pendingApprovals); // Treating all as new for simplicity

                List<Map<String, String>> members = new ArrayList<>();
                if (dept != null && dept.getEmployees() != null) {
                        for (Employee emp : dept.getEmployees()) {
                                User empUser = emp.getUser();
                                Map<String, String> member = new HashMap<>();
                                member.put("id", String.valueOf(emp.getId()));
                                member.put("empCode", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "EMP-" + String.format("%03d", emp.getId()));
                                member.put("name", empUser != null ? empUser.getFullName() : "Employee #" + emp.getId());
                                member.put("email", empUser != null ? empUser.getEmail() : "");
                                member.put("role", emp.getPosition() != null ? emp.getPosition().getName() : "Staff");
                                member.put("rating", "N/A");
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

                // Pass all positions for the modal dropdown
                List<Map<String, String>> positionList = new ArrayList<>();
                List<Position> allPositions = positionRepository.findAll();
                for (Position pos : allPositions) {
                    Map<String, String> p = new HashMap<>();
                    p.put("id", String.valueOf(pos.getId()));
                    p.put("name", pos.getName());
                    positionList.add(p);
                }
                data.put("allPositions", positionList);

                return data;
        }

        public Map<String, Object> getDepartmentMockData() {
                Map<String, Object> data = new HashMap<>();
                User currentUser = utilService.getCurrentUser();
                Map<String, String> managerMap = utilService.getManagerMap(currentUser);
                data.put("manager", managerMap);
                Department dept = utilService.getDepartmentForManager(currentUser);
                
                int pendingApprovals = utilService.getPendingApprovalsCount(dept);
                
                data.put("pendingApprovals", pendingApprovals);
                data.put("newApprovals", pendingApprovals);
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
                        department.put("openPositions", "0"); // requires jobpost integration
                        department.put("budgetUtilization", "N/A"); // Requires salary calculation

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
                                        int count = 0;
                                        if (pos.getEmployees() != null) { // Using standard length
                                                count = pos.getEmployees().size();
                                        }
                                        posMap.put("headcount", String.valueOf(count)); 
                                        posMap.put("status", "Active");
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
                        department.put("budgetUtilization", "N/A");
                }

                data.put("department", department);
                data.put("teams", teams);
                data.put("positions", positions);

                return data;
        }

        @Transactional
        public void createRemovalRequest(Long employeeId, String reason) {
            com.group5.ems.entity.RequestType rt = requestTypeRepository.findByCode("REMOVAL").orElseGet(() -> {
                com.group5.ems.entity.RequestType nrt = new com.group5.ems.entity.RequestType();
                nrt.setCode("REMOVAL");
                nrt.setName("Member Removal");
                nrt.setCategory("HR");
                return requestTypeRepository.save(nrt);
            });

            com.group5.ems.entity.Request req = new com.group5.ems.entity.Request();
            req.setEmployeeId(employeeId);
            req.setRequestTypeId(rt.getId());
            req.setTitle("Request Member Removal");
            req.setContent(reason);
            req.setStatus("PENDING");
            requestRepository.save(req);
        }

        @Transactional
        public boolean createAddMemberRequest(String requestType, String role, String description) {
            if ("TRANSFER".equals(requestType)) {
                // Return false to simulate no internal transfer targets available
                return false;
            }

            // Recruitment logic
            com.group5.ems.entity.RequestType rt = requestTypeRepository.findByCode("RECRUITMENT").orElseGet(() -> {
                com.group5.ems.entity.RequestType nrt = new com.group5.ems.entity.RequestType();
                nrt.setCode("RECRUITMENT");
                nrt.setName("Recruitment");
                nrt.setCategory("HR");
                return requestTypeRepository.save(nrt);
            });

            User currentUser = utilService.getCurrentUser();
            Department dept = utilService.getDepartmentForManager(currentUser);
            // using any employee ID as a placeholder for the requested by
            Long anyEmpId = (dept.getEmployees() != null && !dept.getEmployees().isEmpty()) 
                            ? dept.getEmployees().get(0).getId() : 1L;

            com.group5.ems.entity.Request req = new com.group5.ems.entity.Request();
            req.setEmployeeId(anyEmpId);
            req.setRequestTypeId(rt.getId());
            req.setTitle("Request Recruitment: " + role);
            req.setContent(description);
            req.setStatus("PENDING");
            requestRepository.save(req);

            return true;
        }
}
