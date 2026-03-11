package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrAttendanceDTO;
import com.group5.ems.entity.Attendance;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HrAttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private HrAttendanceService hrAttendanceService;

    private Attendance attendance1;

    @BeforeEach
    void setUp() {
        User user1 = new User();
        user1.setFullName("Charlie Chaplin");

        Department dept1 = new Department();
        dept1.setName("Entertainment");

        Employee employee1 = new Employee();
        employee1.setUser(user1);
        employee1.setDepartment(dept1);

        attendance1 = new Attendance();
        attendance1.setId(5L);
        attendance1.setEmployee(employee1);
        attendance1.setWorkDate(LocalDate.now());
        attendance1.setCheckIn(LocalTime.of(8, 30));
        attendance1.setCheckOut(LocalTime.of(17, 0));
        attendance1.setStatus("Present");
    }

    @Test
    void testGetAllAttendances() {
        when(attendanceRepository.findAll()).thenReturn(Arrays.asList(attendance1));

        List<HrAttendanceDTO> result = hrAttendanceService.getAllAttendances();

        assertEquals(1, result.size());
        HrAttendanceDTO dto = result.get(0);
        assertEquals("Charlie Chaplin", dto.employeeName());
        assertEquals("CC", dto.initials());
        assertEquals("Entertainment", dto.department());
        assertEquals(LocalTime.of(8, 30), dto.checkIn());
        assertEquals(LocalTime.of(17, 0), dto.checkOut());
        assertEquals("Present", dto.status());
    }
}
