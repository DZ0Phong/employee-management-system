package com.group5.ems.service.hr;

import com.group5.ems.dto.request.hr.HrEventCreateDTO;
import com.group5.ems.dto.request.hr.HrEventUpdateDTO;
import com.group5.ems.dto.response.hr.HrEventDTO;
import com.group5.ems.dto.response.hr.HrEventResponseDTO;
import com.group5.ems.entity.Event;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.EventRepository;
import com.group5.ems.service.common.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrCalendarService {

    private final EventRepository eventRepository;
    private final LogService logService;

    public List<HrEventDTO> getUpcomingEvents() {
        LocalDate today = LocalDate.now();
        List<HrEventDTO> events = eventRepository.findUpcomingEventsDto(today);
        
        // Limit to top 5 for dashboard
        return events.stream().limit(5).toList();
    }

    public List<HrEventResponseDTO> getEventsByMonth(int month, int year) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        
        return eventRepository.findByDateRangeDto(startOfMonth, endOfMonth);
    }

    @Transactional
    public void createEvent(HrEventCreateDTO dto, Long createdBy) {
        Event event = new Event();
        event.setTitle(dto.title());
        event.setDescription(dto.description());
        event.setStartDate(dto.startDate());
        event.setEndDate(dto.endDate());
        event.setStartTime(dto.startTime());
        event.setEndTime(dto.endTime());
        event.setType(dto.type());
        event.setColor(dto.color());
        event.setIsAllDay(dto.isAllDay());
        event.setDepartmentId(dto.departmentId());
        event.setCreatedBy(createdBy);
        
        Event saved = eventRepository.save(event);
        
        logService.log(AuditAction.CREATE, AuditEntityType.EVENT, saved.getId(), createdBy);
    }

    @Transactional
    public void updateEvent(HrEventUpdateDTO dto, Long updatedBy) {
        Event event = eventRepository.findById(dto.id())
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + dto.id()));

        event.setTitle(dto.title());
        event.setDescription(dto.description());
        event.setStartDate(dto.startDate());
        event.setEndDate(dto.endDate());
        event.setStartTime(dto.startTime());
        event.setEndTime(dto.endTime());
        event.setType(dto.type());
        event.setColor(dto.color());
        event.setIsAllDay(dto.isAllDay());
        event.setDepartmentId(dto.departmentId());
        
        eventRepository.save(event);
        
        logService.log(AuditAction.UPDATE, AuditEntityType.EVENT, event.getId(), updatedBy);
    }

    @Transactional
    public void deleteEvent(Long id, Long deletedBy) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));
        
        eventRepository.delete(event);
        
        logService.log(AuditAction.DELETE, AuditEntityType.EVENT, id, deletedBy);
    }
}
