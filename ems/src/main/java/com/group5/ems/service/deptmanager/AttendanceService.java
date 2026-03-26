package com.group5.ems.service.deptmanager;

import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Request;
import com.group5.ems.entity.User;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private static final LocalTime LATE_AFTER = LocalTime.of(9, 0);
    private static final LocalTime ABSENT_AFTER = LocalTime.of(10, 30);

    private final AttendanceRepository attendanceRepository;
    private final RequestRepository requestRepository;
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
            Map<Long, Set<LocalDate>> approvedLeaveDays = buildApprovedLeaveDays(empIds, monday, sunday);

            // Map grouped by Employee ID
            Map<Long, List<com.group5.ems.entity.Attendance>> employeeAttendanceMap = new HashMap<>();
            for (com.group5.ems.entity.Attendance att : attendances) {
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
                Set<LocalDate> leaveDays = approvedLeaveDays.getOrDefault(emp.getId(), Set.of());

                int eLate = 0;
                int eAbsent = 0;
                int eApprovedLeave = 0;
                int totalCheckins = 0;
                long totalMinutes = 0;

                List<Map<String, String>> dailyStatus = new ArrayList<>();

                for (java.time.LocalDate day : weekDays) {
                    boolean found = false;
                    for (com.group5.ems.entity.Attendance a : ematts) {
                        if (a.getWorkDate().equals(day)) {
                            Map<String, String> dStat = new HashMap<>();
                            boolean approvedLeaveDay = leaveDays.contains(day);
                            String effectiveStatus = resolveAttendanceStatus(a);
                            String displayStatus = resolveDisplayStatus(a, approvedLeaveDay);
                            if ("PRESENT".equalsIgnoreCase(displayStatus)) {
                                dStat.put("icon", "check_circle");
                                dStat.put("color", approvedLeaveDay ? "text-emerald-500" : "text-emerald-500");
                                onTimeCount++;
                            } else if ("LATE".equalsIgnoreCase(displayStatus)) {
                                dStat.put("icon", "schedule");
                                dStat.put("color", "text-amber-500");
                                eLate++;
                                lateCount++;
                                latestViolations.add(0, buildLateViolation(empRowName(emp), a.getCheckIn(), day));
                            } else if ("LEAVE".equalsIgnoreCase(displayStatus)) {
                                dStat.put("icon", "check_circle");
                                dStat.put("color", "text-sky-500");
                                eApprovedLeave++;
                            } else {
                                dStat.put("icon", "cancel");
                                dStat.put("color", "text-rose-500");
                                eAbsent++;
                                absenceCount++;
                                latestViolations.add(0, buildAbsenceViolation(empRowName(emp), day, a.getCheckIn(), "ABSENT".equalsIgnoreCase(effectiveStatus) && a.getCheckIn() != null));
                            }
                            dailyStatus.add(dStat);

                            if (a.getCheckIn() != null && ("PRESENT".equalsIgnoreCase(displayStatus) || "LATE".equalsIgnoreCase(displayStatus))) {
                                totalCheckins++;
                                totalMinutes += a.getCheckIn().getHour() * 60 + a.getCheckIn().getMinute();
                            }

                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Map<String, String> dStat = new HashMap<>();
                        if (leaveDays.contains(day)) {
                            dStat.put("icon", "check_circle");
                            dStat.put("color", "text-sky-500");
                            eApprovedLeave++;
                        } else if (isMissingAttendanceAbsence(day, today)) {
                            dStat.put("icon", "cancel");
                            dStat.put("color", "text-rose-500");
                            eAbsent++;
                            absenceCount++;
                            latestViolations.add(0, buildAbsenceViolation(empRowName(emp), day, null, false));
                        } else {
                            dStat.put("icon", "remove");
                            dStat.put("color", "text-slate-300");
                        }
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
                } else if (eApprovedLeave > 0) {
                    empRow.put("weeklyStatus", "On Leave (x" + eApprovedLeave + ")");
                    empRow.put("statusClass", "bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-400");
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

    private String resolveAttendanceStatus(com.group5.ems.entity.Attendance attendance) {
        if (attendance == null) {
            return "ABSENT";
        }
        if (attendance.getCheckIn() != null) {
            return resolveStatusByCheckIn(attendance.getCheckIn());
        }
        return attendance.getStatus() != null ? attendance.getStatus() : "ABSENT";
    }

    private String resolveStatusByCheckIn(LocalTime checkIn) {
        if (checkIn == null) {
            return "ABSENT";
        }
        if (checkIn.isAfter(ABSENT_AFTER)) {
            return "ABSENT";
        }
        if (checkIn.isAfter(LATE_AFTER)) {
            return "LATE";
        }
        return "PRESENT";
    }

    private String resolveDisplayStatus(com.group5.ems.entity.Attendance attendance, boolean approvedLeaveDay) {
        if (!approvedLeaveDay) {
            return resolveAttendanceStatus(attendance);
        }
        if (attendance != null && attendance.getCheckIn() != null) {
            return "PRESENT";
        }
        return "LEAVE";
    }

    private boolean isMissingAttendanceAbsence(java.time.LocalDate day, java.time.LocalDate today) {
        DayOfWeek dayOfWeek = day.getDayOfWeek();
        boolean workingDay = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        return workingDay && !day.isAfter(today);
    }

    private String empRowName(Employee employee) {
        return employee.getUser() != null ? employee.getUser().getFullName() : "Employee";
    }

    private Map<String, String> buildLateViolation(String employeeName, LocalTime checkIn, LocalDate workDate) {
        Map<String, String> violation = new HashMap<>();
        violation.put("type", "Late Arrival");
        violation.put("icon", "history");
        violation.put("colorClass", "bg-amber-50 border-amber-100 text-amber-500");
        violation.put("bgClass", "bg-amber-500");
        violation.put("title", employeeName + " - Late Arrival");
        violation.put("description", "Checked in at "
                + (checkIn != null ? checkIn : "Unknown")
                + " on "
                + workDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd"))
                + ".");
        return violation;
    }

    private Map<String, String> buildAbsenceViolation(String employeeName, LocalDate workDate, LocalTime checkIn, boolean afterCutoff) {
        Map<String, String> violation = new HashMap<>();
        violation.put("type", "Absence");
        violation.put("icon", "person_off");
        violation.put("colorClass", "bg-rose-50 border-rose-100 text-rose-500");
        violation.put("bgClass", "bg-rose-500");
        violation.put("title", employeeName + " - Absence");
        violation.put("description", afterCutoff && checkIn != null
                ? "Checked in after 10:30 at " + checkIn + " on " + workDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd")) + "."
                : "No valid check-in on " + workDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd")) + ".");
        return violation;
    }

    private Map<Long, Set<LocalDate>> buildApprovedLeaveDays(List<Long> employeeIds, LocalDate rangeStart, LocalDate rangeEnd) {
        Map<Long, Set<LocalDate>> leaveDays = new HashMap<>();
        if (employeeIds.isEmpty()) {
            return leaveDays;
        }

        List<Request> approvedLeaves = requestRepository.findApprovedLeaveRequestsByEmployeeIdsAndDateRange(employeeIds, rangeStart, rangeEnd);
        for (Request leave : approvedLeaves) {
            LocalDate start = leave.getLeaveFrom().isBefore(rangeStart) ? rangeStart : leave.getLeaveFrom();
            LocalDate end = leave.getLeaveTo().isAfter(rangeEnd) ? rangeEnd : leave.getLeaveTo();
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                DayOfWeek dayOfWeek = cursor.getDayOfWeek();
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    leaveDays.computeIfAbsent(leave.getEmployeeId(), key -> new HashSet<>()).add(cursor);
                }
                cursor = cursor.plusDays(1);
            }
        }
        return leaveDays;
    }
}
