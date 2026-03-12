package com.group5.ems.service.employee;

import com.group5.ems.dto.response.AttendanceDTO;
import com.group5.ems.dto.response.AttendanceStatsDTO;
import com.group5.ems.entity.Attendance;
import com.group5.ems.repository.AttendanceRepository;
import com.group5.ems.service.employee.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;

    // Giờ bắt đầu làm việc chuẩn
    private static final LocalTime WORK_START = LocalTime.of(9, 0);

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDTO> getAttendanceHistory(Long employeeId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, from, to)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatsDTO getAttendanceStats(Long employeeId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<Attendance> records = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, from, to);

        // Tổng số giờ làm
        long totalMinutes = records.stream()
                .filter(a -> a.getCheckIn() != null && a.getCheckOut() != null)
                .mapToLong(a -> ChronoUnit.MINUTES.between(a.getCheckIn(), a.getCheckOut()))
                .sum();
        String totalHours = (totalMinutes / 60) + "h " + (totalMinutes % 60) + "m";

        // Số ngày đi làm
        long presentDays = records.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()) || "LATE".equals(a.getStatus()))
                .count();

        // Số ngày đúng giờ
        long onTimeDays = records.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()))
                .filter(a -> a.getCheckIn() != null && !a.getCheckIn().isAfter(WORK_START))
                .count();

        double onTimeRate = presentDays > 0
                ? Math.round((onTimeDays * 100.0 / presentDays) * 10.0) / 10.0
                : 0.0;

        // Tổng ngày làm việc trong tháng (trừ weekend)
        int totalWorkDays = countWorkDays(from, to);

        // Kiểm tra hôm nay đã clock in/out chưa
        LocalDate today = LocalDate.now();
        Optional<Attendance> todayRecord = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today);
        boolean clockedInToday = todayRecord.isPresent() && todayRecord.get().getCheckIn() != null;
        boolean clockedOutToday = todayRecord.isPresent() && todayRecord.get().getCheckOut() != null;

        return AttendanceStatsDTO.builder()
                .totalHours(totalHours)
                .onTimeRate(onTimeRate)
                .presentDays((int) presentDays)
                .totalWorkDays(totalWorkDays)
                .clockedInToday(clockedInToday)
                .clockedOutToday(clockedOutToday)
                .build();
    }

    @Override
    @Transactional
    public void clockIn(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today);

        if (existing.isPresent()) {
            Attendance att = existing.get();
            if (att.getCheckIn() != null) {
                throw new RuntimeException("Already clocked in today!");
            }
            att.setCheckIn(now);
            att.setStatus(now.isAfter(WORK_START) ? "LATE" : "PRESENT");
            attendanceRepository.save(att);
        } else {
            Attendance att = new Attendance();
            att.setEmployeeId(employeeId);
            att.setWorkDate(today);
            att.setCheckIn(now);
            att.setStatus(now.isAfter(WORK_START) ? "LATE" : "PRESENT");
            attendanceRepository.save(att);
        }
    }

    @Override
    @Transactional
    public void clockOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Attendance att = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("You haven't clocked in today!"));

        if (att.getCheckOut() != null) {
            throw new RuntimeException("Already clocked out today!");
        }

        att.setCheckOut(now);
        attendanceRepository.save(att);
    }

    @Override
    public byte[] exportReport(Long employeeId, int year, int month) {
        List<AttendanceDTO> records = getAttendanceHistory(employeeId, year, month);

        // Tạo CSV đơn giản
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Check In,Check Out,Duration,Status\n");
        for (AttendanceDTO r : records) {
            csv.append(r.getWorkDate()).append(",")
                    .append(r.getCheckIn() != null ? r.getCheckIn() : "-").append(",")
                    .append(r.getCheckOut() != null ? r.getCheckOut() : "-").append(",")
                    .append(r.getDuration()).append(",")
                    .append(r.getStatus()).append("\n");
        }
        return csv.toString().getBytes();
    }

    // ── Helper ─────────────────────────────────────────────

    private AttendanceDTO mapToDTO(Attendance a) {
        return AttendanceDTO.builder()
                .id(a.getId())
                .workDate(a.getWorkDate())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .status(a.getStatus())
                .note(a.getNote())
                .build();
    }

    private int countWorkDays(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate date = from;
        while (!date.isAfter(to)) {
            int dow = date.getDayOfWeek().getValue();
            if (dow != 6 && dow != 7) count++;
            date = date.plusDays(1);
        }
        return count;
    }
}