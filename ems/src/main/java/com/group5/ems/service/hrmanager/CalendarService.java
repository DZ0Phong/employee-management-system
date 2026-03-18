package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.EventCreateDTO;
import com.group5.ems.dto.request.hrmanager.EventUpdateDTO;
import com.group5.ems.dto.response.hrmanager.EventResponseDTO;
import com.group5.ems.entity.Event;
import com.group5.ems.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final EventRepository eventRepository;


    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM").withLocale(java.util.Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("hh:mm a");

    // ── READ ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getEventsByMonth(int month, int year) {
        return eventRepository.findByMonthAndYear(month, year)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getEventsByWeek(LocalDate anyDayInWeek) {
        LocalDate weekStart = anyDayInWeek.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = anyDayInWeek.with(DayOfWeek.SUNDAY);
        return eventRepository.findByWeek(weekStart, weekEnd)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDate.now())
                .stream()
                .limit(3)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        return mapToResponseDTO(event);
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto, Long userId) {
        // 1. Validate
        validateEventData(dto.getTitle(), dto.getStartDate());

        // 2. Map DTO → Entity
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : dto.getStartDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setType(dto.getType());
        event.setColor(dto.getColor() != null ? dto.getColor() : "blue");
        event.setIsAllDay(dto.getIsAllDay() != null ? dto.getIsAllDay() : false);
        event.setDepartmentId(dto.getDepartmentId());
        event.setCreatedBy(userId);

        // 3. Save
        Event saved = eventRepository.save(event);

        // 4. Audit log ← QUAN TRỌNG

        return mapToResponseDTO(saved);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Transactional
    public EventResponseDTO updateEvent(EventUpdateDTO dto, Long userId) {
        // 1. Tìm event
        Event event = eventRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Event not found: " + dto.getId()));

        // 2. Validate
        validateEventData(dto.getTitle(), dto.getStartDate());

        // 3. Update fields
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : dto.getStartDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setType(dto.getType());
        event.setColor(dto.getColor());
        event.setIsAllDay(dto.getIsAllDay());
        event.setDepartmentId(dto.getDepartmentId());

        // 4. Save
        Event updated = eventRepository.save(event);

        // 5. Audit log ← QUAN TRỌNG

        return mapToResponseDTO(updated);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteEvent(Long id, Long userId) {
        // 1. Kiểm tra tồn tại
        eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        // 2. Delete
        eventRepository.deleteById(id);

        // 3. Audit log ← QUAN TRỌNG
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    private EventResponseDTO mapToResponseDTO(Event event) {
        String monthLabel = event.getStartDate().format(MONTH_FMT).toUpperCase();
        String dayLabel   = String.valueOf(event.getStartDate().getDayOfMonth());
        String timeLabel  = buildTimeLabel(event);
        String colorClass = buildColorClass(event.getColor());

        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDate(event.getStartDate() != null ? event.getStartDate().toString() : null)
                .endDate(event.getEndDate() != null ? event.getEndDate().toString() : null)
                .startTime(event.getStartTime() != null ? event.getStartTime().toString() : null)
                .endTime(event.getEndTime() != null ? event.getEndTime().toString() : null)
                .type(event.getType())
                .color(event.getColor())
                .isAllDay(event.getIsAllDay())
                .creatorName(event.getCreator() != null ? event.getCreator().getFullName() : "N/A")
                .departmentName(event.getDepartment() != null ? event.getDepartment().getName() : "All Departments")
                .monthLabel(monthLabel)
                .dayLabel(dayLabel)
                .timeLabel(timeLabel)
                .colorClass(colorClass)
                .build();
    }

    private String buildTimeLabel(Event event) {
        if (Boolean.TRUE.equals(event.getIsAllDay())) return "All Day";
        if (event.getStartTime() == null) return "TBD";
        String start = event.getStartTime().format(TIME_FMT);
        if (event.getEndTime() == null) return start;
        return start + " - " + event.getEndTime().format(TIME_FMT);
    }

    private String buildColorClass(String color) {
        if (color == null) return "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400";
        return switch (color) {
            case "purple"  -> "bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400";
            case "emerald" -> "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400";
            case "amber"   -> "bg-amber-50 dark:bg-amber-900/20 text-amber-600 dark:text-amber-400";
            case "rose"    -> "bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400";
            default        -> "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400";
        };
    }

    // ── VALIDATION ────────────────────────────────────────────────────────────

    private void validateEventData(String title, LocalDate startDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
    }
}