package com.group5.ems.controller.hr;

import com.group5.ems.dto.request.RecruitmentTicketDTO;
import com.group5.ems.entity.BenefitType;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Position;
import com.group5.ems.repository.BenefitTypeRepository;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.EmployeeRepository;
import com.group5.ems.repository.PositionRepository;
import com.group5.ems.service.hr.HrRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/hr/employee/new")
@RequiredArgsConstructor
public class RecruitmentRequestController {

    private final HrRequestService requestService;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitTypeRepository benefitTypeRepository;

    @GetMapping
    public String showForm(Model model) {
        populateFormModel(model);
        if (!model.containsAttribute("ticket")) {
            model.addAttribute("ticket", RecruitmentTicketDTO.builder().build());
        }
        return "hr/recruit-request-form";
    }

    @PostMapping
    public String submitTicket(
            @Valid @ModelAttribute("ticket") RecruitmentTicketDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateFormModel(model);
            return "hr/recruit-request-form";
        }

        try {
            requestService.submitRecruitmentTicket(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Onboarding request for \"" + dto.firstName() + " " + dto.lastName()
                            + "\" submitted successfully to Admin.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/hr/requests?tab=pending";
    }

    private void populateFormModel(Model model) {
        List<Department> departments = departmentRepository.findAll();
        List<Position> positions = positionRepository.findAll();
        List<Employee> managers = employeeRepository.findAllWithUser();
        List<BenefitType> benefitTypes = benefitTypeRepository.findByIsActiveTrue();

        model.addAttribute("departments", departments);
        model.addAttribute("positions", positions);
        model.addAttribute("managers", managers);
        model.addAttribute("benefitTypes", benefitTypes);
    }
}
