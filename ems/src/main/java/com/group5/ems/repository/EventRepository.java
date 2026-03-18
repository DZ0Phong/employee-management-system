package com.group5.ems.repository;

import com.group5.ems.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Lấy events theo tháng và năm
    @Query("SELECT e FROM Event e WHERE MONTH(e.startDate) = :month AND YEAR(e.startDate) = :year ORDER BY e.startDate ASC")
    List<Event> findByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Lấy events theo tuần
    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :weekStart AND :weekEnd ORDER BY e.startDate ASC, e.startTime ASC")
    List<Event> findByWeek(@Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    // Lấy events sắp tới (cho Dashboard)
    @Query("SELECT e FROM Event e WHERE e.startDate >= :today ORDER BY e.startDate ASC")
    List<Event> findUpcomingEvents(@Param("today") LocalDate today);

    // Lấy events theo type
    List<Event> findByTypeOrderByStartDateAsc(String type);

    // Lấy events theo department
    List<Event> findByDepartmentIdOrderByStartDateAsc(Long departmentId);
}