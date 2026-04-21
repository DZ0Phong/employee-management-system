package com.group5.ems.controller.admin;

import com.group5.ems.dto.request.DepartmentFormDTO;
import com.group5.ems.dto.response.DepartmentDTO;
import com.group5.ems.dto.response.UserDTO;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.User;
import com.group5.ems.service.admin.AdminService;
import com.group5.ems.service.employee.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DepartmentController {

    private final AdminService adminService;
    private final EmployeeService employeeService;

    @GetMapping("/departments")
    public String department(@RequestParam(defaultValue = "") String keyword,
                             @RequestParam(defaultValue = "name") String sort,
                             @RequestParam(defaultValue = "asc") String dir,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "9") int pageSize,
                             Model model) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDir", dir);

        model.addAttribute("statTotal", adminService.getAllDepartmentsCount());
        model.addAttribute("statTotalStaff", adminService.getAllEmployeesCount());
        model.addAttribute("statWithChildren", adminService.getAllParents());

        Page<DepartmentDTO> pg = adminService.getDepartmentsFilter(keyword,sort,dir,page,pageSize);
        model.addAttribute("departments", pg);
        model.addAttribute("currentPage", pg.getNumber());
        model.addAttribute("totalPages", pg.getTotalPages());
        model.addAttribute("totalElements", pg.getTotalElements());
        model.addAttribute("pageSize", pg.getSize());

        model.addAttribute("deptForm", new DepartmentFormDTO());
        List<DepartmentDTO> allDepts = adminService.getAllDepartmentsDTO();
        model.addAttribute("allDepartments", allDepts);

        List<UserDTO> allManagers = adminService.getAllManagersForSelect();
        model.addAttribute("allManagers", allManagers);

        adminService.getUserDTO().ifPresent(u -> model.addAttribute("currentUser", u));

        return "admin/department";
    }


    @GetMapping("/departments/{id}/members")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<UserDTO> getDeptMember(@PathVariable("id") Long departmentId) {
        List<Employee> employees = employeeService.getAllEmployeeFromDepartment(departmentId);
        List<User> user = employees.stream().map(Employee::getUser).toList();
        return user.stream().map(adminService::toUserDTO).toList();
    }

    @PostMapping("/departments/save")
    public String saveDepartment(@ModelAttribute("deptForm") DepartmentFormDTO form, RedirectAttributes ra) {
        try {
            adminService.saveDepartment(form);
            ra.addFlashAttribute("successMsg", (form.getId() == null) ? "Department created" : "Department updated");
            return "redirect:/admin/departments";
        }
        catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/delete/{id}")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteDepartment(id);
            ra.addFlashAttribute("successMsg", "Department deleted");
        }
        catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/departments";
    }
}
