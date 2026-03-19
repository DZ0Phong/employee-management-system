package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.AttendanceRepository;
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
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final DeptManagerUtilService utilService;

    public Map<String, Object> getAttendanceReviewData(int weekOffset) {
        Map<String, Object> data = new HashMap<>();
        User currentUser = utilService.getCurrentUser();
        Map<String, String> managerMap = utilService.getManagerMap(currentUser);
        data.put("manager", managerMap);

        Department dept = utilService.getDepartmentForManager(currentUser);
        Map<String, String> deptMap = new HashMap<>();
        if (dept != null) {
            deptMap.put("name", dept.getName());
        } else {
            deptMap.put("name", "Engineering Department");
        }
        data.put("department", deptMap);

        int pendingApprovals = utilService.getPendingApprovalsCount(dept);
        data.put("pendingApprovals", pendingApprovals);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate targetDate = today.plusWeeks(weekOffset);
        java.time.LocalDate monday = targetDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate sunday = monday.plusDays(6);

        int onTimeCount = 0;
        int lateCount = 0;
        int absenceCount = 0;

        List<Map<String, Object>> attendanceTable = new ArrayList<>();
        List<Map<String, String>> latestViolations = new ArrayList<>();

        if (dept != null && dept.getEmployees() != null && !dept.getEmployees().isEmpty()) {
            List<Long> empIds = new ArrayList<>();
            for (Employee emp : dept.getEmployees()) {
                empIds.add(emp.getId());
            }

            List<com.group5.ems.entity.Attendance> attendances = attendanceRepository.findByEmployeeIdInAndWorkDateBetweenOrderByWorkDateAsc(empIds, monday, sunday);

            // Map grouped by Employee ID
            Map<Long, List<com.group5.ems.entity.Attendance>> employeeAttendanceMap = new HashMap<>();
            for (com.group5.ems.entity.Attendance att : attendances) {
                if ("PRESENT".equalsIgnoreCase(att.getStatus())) onTimeCount++;
                else if ("LATE".equalsIgnoreCase(att.getStatus())) lateCount++;
                else if ("ABSENT".equalsIgnoreCase(att.getStatus())) absenceCount++;

                // Tracking violations
                if ("LATE".equalsIgnoreCase(att.getStatus()) || "ABSENT".equalsIgnoreCase(att.getStatus())) {
                    Map<String, String> violation = new HashMap<>();
                    String empName = att.getEmployee() != null && att.getEmployee().getUser() != null ? att.getEmployee().getUser().getFullName() : "Employee";

                    if ("ABSENT".equalsIgnoreCase(att.getStatus())) {
                        violation.put("type", "Absence");
                        violation.put("icon", "person_off");
                        violation.put("colorClass", "bg-rose-50 border-rose-100 text-rose-500");
                        violation.put("bgClass", "bg-rose-500");
                        violation.put("title", empName + " - Absence");
                        violation.put("description", "Absence on " + att.getWorkDate().format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd")) + ".");
                    } else {
                        violation.put("type", "Late Arrival");
                        violation.put("icon", "history");
                        violation.put("colorClass", "bg-amber-50 border-amber-100 text-amber-500");
                        violation.put("bgClass", "bg-amber-500");
                        violation.put("title", empName + " - Late Arrival");
                        violation.put("description", "Checked in at " + (att.getCheckIn() != null ? att.getCheckIn() : "Unknown") + " on " + att.getWorkDate().format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd")) + ".");
                    }
                    latestViolations.add(0, violation); // Add to newest
                }

                employeeAttendanceMap.computeIfAbsent(att.getEmployeeId(), k -> new ArrayList<>()).add(att);
            }

            // Generate dates for the headers (Mon-Sun)
            List<java.time.LocalDate> weekDays = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                weekDays.add(monday.plusDays(i));
            }

            List<String> dateHeaders = new ArrayList<>();
            for (java.time.LocalDate d : weekDays) {
                dateHeaders.add(d.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd")));
            }
            data.put("dateHeaders", dateHeaders);
            data.put("weekOffset", weekOffset);


            for (Employee emp : dept.getEmployees()) {
                Map<String, Object> empRow = new HashMap<>();
                empRow.put("empCode", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : ("EMP-" + emp.getId()));
                empRow.put("name", emp.getUser() != null ? emp.getUser().getFullName() : "Employee");
                empRow.put("avatarUrl", emp.getUser() != null && emp.getUser().getAvatarUrl() != null ? emp.getUser().getAvatarUrl() : "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA");

                List<com.group5.ems.entity.Attendance> ematts = employeeAttendanceMap.getOrDefault(emp.getId(), new ArrayList<>());

                int eLate = 0;
                int eAbsent = 0;
                int totalCheckins = 0;
                long totalMinutes = 0;

                List<Map<String, String>> dailyStatus = new ArrayList<>();

                for (java.time.LocalDate day : weekDays) {
                    boolean found = false;
                    for (com.group5.ems.entity.Attendance a : ematts) {
                        if (a.getWorkDate().equals(day)) {
                            Map<String, String> dStat = new HashMap<>();
                            if ("PRESENT".equalsIgnoreCase(a.getStatus())) {
                                dStat.put("icon", "check_circle");
                                dStat.put("color", "text-emerald-500");
                            } else if ("LATE".equalsIgnoreCase(a.getStatus())) {
                                dStat.put("icon", "schedule");
                                dStat.put("color", "text-amber-500");
                                eLate++;
                            } else {
                                dStat.put("icon", "cancel");
                                dStat.put("color", "text-rose-500");
                                eAbsent++;
                            }
                            dailyStatus.add(dStat);

                            if (a.getCheckIn() != null && ("PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))) {
                                totalCheckins++;
                                totalMinutes += a.getCheckIn().getHour() * 60 + a.getCheckIn().getMinute();
                            }

                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // If no record, assume weekend or not recorded
                        Map<String, String> dStat = new HashMap<>();
                        dStat.put("icon", "remove");
                        dStat.put("color", "text-slate-300");
                        dailyStatus.add(dStat);
                    }
                }

                empRow.put("dailyStatus", dailyStatus);

                String avgCheckin = "N/A";
                if (totalCheckins > 0) {
                    long avgMins = totalMinutes / totalCheckins;
                    long hr = avgMins / 60;
                    long mn = avgMins % 60;
                    String ampm = hr >= 12 ? "PM" : "AM";
                    long dhr = hr > 12 ? hr - 12 : (hr == 0 ? 12 : hr);
                    avgCheckin = String.format("%02d:%02d %s", dhr, mn, ampm);
                }
                empRow.put("avgCheckIn", avgCheckin);

                if (eAbsent > 0) {
                    empRow.put("weeklyStatus", "Absence");
                    empRow.put("statusClass", "bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-400");
                } else if (eLate > 0) {
                    empRow.put("weeklyStatus", "Late (x" + eLate + ")");
                    empRow.put("statusClass", "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400");
                } else {
                    empRow.put("weeklyStatus", "On Track");
                    empRow.put("statusClass", "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400");
                }

                attendanceTable.add(empRow);
            }
        }

        // If no violations, we might want to trim or show a message. Here we limit to 5
        if (latestViolations.size() > 5) {
            latestViolations = latestViolations.subList(0, 5);
        }

        data.put("onTimeCount", onTimeCount);
        data.put("lateCount", lateCount);
        data.put("absenceCount", absenceCount);
        data.put("attendanceTable", attendanceTable);
        data.put("latestViolations", latestViolations);

        return data;
    }
}
