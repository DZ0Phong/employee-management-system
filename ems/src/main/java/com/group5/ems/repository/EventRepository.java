package com.group5.ems.repository;

import com.group5.ems.entity.Event;
import com.group5.ems.dto.response.hr.HrEventDTO;
import com.group5.ems.dto.response.hr.HrEventResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // HR Dashboard: Upcoming events as DTO
    @Query("""
        SELECT new com.group5.ems.dto.response.hr.HrEventDTO(
            e.title, e.startDate, e.startTime, e.endTime, e.color
        )
        FROM Event e 
        WHERE e.startDate >= :today 
        ORDER BY e.startDate ASC, e.startTime ASC
    """)
    List<HrEventDTO> findUpcomingEventsDto(@Param("today") LocalDate today);

    // HR Calendar: Events by month as DTO
    @Query("""
        SELECT new com.group5.ems.dto.response.hr.HrEventResponseDTO(
            e.id, e.title, e.description, e.startDate, e.endDate, 
            e.startTime, e.endTime, e.type, e.color, e.isAllDay, e.departmentId
        )
        FROM Event e 
        WHERE (e.startDate BETWEEN :start AND :end)
        ORDER BY e.startDate ASC, e.startTime ASC
    """)
    List<HrEventResponseDTO> findByDateRangeDto(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Lấy events theo tháng và năm (Original for HR Manager)
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

    // Lấy policy reviews (type = REVIEW hoặc title/description chứa 'training')
    @Query("SELECT e FROM Event e WHERE e.type = 'REVIEW' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY " +
            "CASE WHEN e.status = 'IN_REVIEW' THEN 1 " +
            "     WHEN e.status = 'DRAFTING' THEN 2 " +
            "     WHEN e.status = 'FINALIZED' THEN 3 " +
            "     ELSE 4 END, e.startDate ASC")
    List<Event> findPolicyReviews(@Param("keyword") String keyword);

    // Lấy events theo department
    List<Event> findByDepartmentIdOrderByStartDateAsc(Long departmentId);
    
    // Lấy events theo khoảng thời gian
    @Query("SELECT e FROM Event e WHERE " +
           "(e.startDate > :startDate OR (e.startDate = :startDate AND e.startTime >= :startTime)) AND " +
           "(e.startDate < :endDate OR (e.startDate = :endDate AND e.startTime <= :endTime)) " +
           "ORDER BY e.startDate ASC, e.startTime ASC")
    List<Event> findByStartTimeBetween(@Param("startDate") java.time.LocalDate startDate,
                                       @Param("startTime") java.time.LocalTime startTime,
                                       @Param("endDate") java.time.LocalDate endDate,
                                       @Param("endTime") java.time.LocalTime endTime);

    // Lấy training events đang active (chưa kết thúc hoặc vừa kết thúc gần đây)
    @Query("SELECT e FROM Event e WHERE e.type = 'TRAINING' " +
           "AND (e.endDate IS NULL OR e.endDate >= :cutoffDate) " +
           "ORDER BY e.startDate DESC")
    List<Event> findActiveTrainingEvents(@Param("cutoffDate") LocalDate cutoffDate);
}