package com.group5.ems.controller.admin;

import com.group5.ems.dto.request.AuditLogDTO;
import com.group5.ems.service.admin.AdminLogService;
import com.group5.ems.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("/admin")
@Controller
@RequiredArgsConstructor
public class SystemlogController {

    private final AdminLogService adminLogService;
    private final AdminService adminService;

    @GetMapping("system-log")
    public String systemlog(@RequestParam(defaultValue = "") String keyword,
                            @RequestParam(defaultValue = "") String action,
                            @RequestParam(defaultValue = "") String dateFrom,
                            @RequestParam(defaultValue = "") String dateTo,
                            @RequestParam(defaultValue = "createdAt") String sort,
                            @RequestParam(defaultValue = "desc") String dir,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int pageSize,
                            Model model){
        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));

        Page<AuditLogDTO> logs = adminLogService.getLogs(keyword, action, dateFrom, dateTo, sort, dir, page, pageSize);

        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", logs.getNumber());
        model.addAttribute("pageSize", logs.getSize());
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("totalElements", logs.getTotalElements());

        model.addAttribute("statTotal", adminLogService.countTotal());
        model.addAttribute("statToday", adminLogService.countToday());
        model.addAttribute("statCreate", adminLogService.countCreate());
        model.addAttribute("statUpdateDelete", adminLogService.countUpdateDelete());
        model.addAttribute("departmentCount", adminService.getAllDepartmentsCount());

        model.addAttribute("keyword", keyword);
        model.addAttribute("actionFilter", action);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDir", dir);

        return "admin/system-log";
    }

    @PostMapping("system-log/delete/{id}")
    public String deleteLog(@PathVariable Long id,
                            RedirectAttributes redirectAttributes) {
        try {
            adminLogService.deleteLog(id);
            redirectAttributes.addFlashAttribute("successMsg", "Log entry deleted successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("successMsg", e.getMessage());
        }
        return "redirect:/admin/system-log";
    }

    @PostMapping("system-log/clear")
    public String clearOldLogs(@RequestParam int olderThanDays,
                               RedirectAttributes redirectAttributes) {
        try {
            adminLogService.clearOlderThanDays(olderThanDays);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Logs older than " + olderThanDays + " days were cleared successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("successMsg", e.getMessage());
        }
        return "redirect:/admin/system-log";
    }
}
