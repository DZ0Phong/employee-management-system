package com.group5.ems.service.hrmanager;

import com.group5.ems.dto.response.hrmanager.EventCreateDTO;
import com.group5.ems.dto.response.hrmanager.EventResponseDTO;
import com.group5.ems.dto.response.hrmanager.EventUpdateDTO;
import com.group5.ems.entity.Event;
import com.group5.ems.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    
    private final EventRepository eventRepository;
    
    public List<EventDTO> getUpcomingEvents() {
        List<EventDTO> events = new ArrayList<>();
        
        // Sample upcoming events - you can replace this with actual database queries
        LocalDate now = LocalDate.now();
        
        events.add(new EventDTO(
            "New Employee Orientation",
            now.plusDays(2),
            "09:00",
            "11:00",
            "blue"
        ));
        
        events.add(new EventDTO(
            "Q3 Performance Reviews",
            now.plusDays(4),
            "00:00",
            "23:59",
            "purple"
        ));
        
        events.add(new EventDTO(
            "Benefits Enrollment Ends",
            now.plusDays(5),
            "00:00",
            "17:00",
            "emerald"
        ));
        
        return events;
    }
    
    public List<EventResponseDTO> getEventsByMonth(int month, int year) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        
        List<Event> events = eventRepository.findByStartTimeBetween(
            startOfMonth, java.time.LocalTime.MIN,
            endOfMonth, java.time.LocalTime.MAX
        );
        
        return events.stream()
                .map(this::mapToEventResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void createEvent(EventCreateDTO dto, Long createdBy) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setType(dto.getType());
        event.setColor(dto.getColor());
        event.setIsAllDay(dto.getIsAllDay());
        event.setDepartmentId(dto.getDepartmentId());
        event.setCreatedBy(createdBy);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        
        eventRepository.save(event);
    }
    
    @Transactional
    public void updateEvent(EventUpdateDTO dto, Long updatedBy) {
        Event event = eventRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setType(dto.getType());
        event.setColor(dto.getColor());
        event.setIsAllDay(dto.getIsAllDay());
        event.setDepartmentId(dto.getDepartmentId());
        event.setUpdatedAt(LocalDateTime.now());
        
        eventRepository.save(event);
    }
    
    @Transactional
    public void deleteEvent(Long id, Long deletedBy) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        eventRepository.delete(event);
    }
    
    private EventResponseDTO mapToEventResponseDTO(Event event) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStartDate(event.getStartDate() != null ? event.getStartDate().toString() : null);
        dto.setEndDate(event.getEndDate() != null ? event.getEndDate().toString() : null);
        dto.setStartTime(event.getStartTime() != null ? event.getStartTime().toString() : null);
        dto.setEndTime(event.getEndTime() != null ? event.getEndTime().toString() : null);
        dto.setType(event.getType());
        dto.setColor(event.getColor());
        dto.setIsAllDay(event.getIsAllDay());
        return dto;
    }
    
    public static class EventDTO {
        private String title;
        private LocalDate date;
        private String startTime;
        private String endTime;
        private String color;
        
        public EventDTO(String title, LocalDate date, String startTime, String endTime, String color) {
            this.title = title;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.color = color;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getMonthLabel() {
            return date.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
        }
        
        public String getDayLabel() {
            return String.valueOf(date.getDayOfMonth());
        }
        
        public String getTimeLabel() {
            if ("00:00".equals(startTime) && "23:59".equals(endTime)) {
                return "All Day";
            }
            return startTime + " - " + endTime;
        }
        
        public String getColor() {
            return color;
        }
        
        public String getColorClass() {
            switch (color) {
                case "blue":
                    return "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400";
                case "purple":
                    return "bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400";
                case "emerald":
                    return "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400";
                default:
                    return "bg-slate-50 dark:bg-slate-900/20 text-slate-600 dark:text-slate-400";
            }
        }
    }
}
