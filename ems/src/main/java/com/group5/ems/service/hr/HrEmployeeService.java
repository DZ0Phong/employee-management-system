package com.group5.ems.service.hr;

import com.group5.ems.dto.response.HrEmployeeDTO;
import com.group5.ems.dto.response.HrEmployeeDetailDTO;
import com.group5.ems.entity.Contract;
import com.group5.ems.entity.Employee;
import com.group5.ems.entity.Salary;
import com.group5.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.service.common.LogService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final com.group5.ems.repository.EmployeeSkillRepository employeeSkillRepository;
    private final com.group5.ems.repository.DepartmentRepository departmentRepository;
    private final com.group5.ems.repository.PositionRepository positionRepository;
    private final com.group5.ems.repository.PerformanceReviewRepository performanceReviewRepository;
    private final com.group5.ems.repository.RewardDisciplineRepository rewardDisciplineRepository;
    private final LogService logService;


    public Page<HrEmployeeDTO> searchEmployees(String search, String department, String status, List<Long> skillIds, Integer minProficiency, Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Join User for name/email search
            Join<Employee, com.group5.ems.entity.User> userJoin = root.join("user", JoinType.INNER);

            // Filtering
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(userJoin.get("fullName")), searchPattern),
                    cb.like(cb.lower(root.get("employeeCode")), searchPattern),
                    cb.like(cb.lower(userJoin.get("email")), searchPattern)
                ));
            }

            if (department != null && !department.trim().isEmpty() && !"All".equalsIgnoreCase(department.trim())) {
                predicates.add(cb.equal(root.get("department").get("name"), department.trim()));
            }

            if (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }

            // Skill Filter using Subquery (Prevents duplicates)
            if (skillIds != null && !skillIds.isEmpty()) {
                Subquery<Long> skillSubquery = query.subquery(Long.class);
                Root<com.group5.ems.entity.EmployeeSkill> esRoot = skillSubquery.from(com.group5.ems.entity.EmployeeSkill.class);
                skillSubquery.select(esRoot.get("employeeId"));
                
                List<Predicate> esPredicates = new ArrayList<>();
                esPredicates.add(esRoot.get("skillId").in(skillIds));
                if (minProficiency != null) {
                    esPredicates.add(cb.greaterThanOrEqualTo(esRoot.get("proficiency"), minProficiency));
                }
                
                skillSubquery.where(cb.and(esPredicates.toArray(new Predicate[0])));
                skillSubquery.groupBy(esRoot.get("employeeId"));
                skillSubquery.having(cb.equal(cb.countDistinct(esRoot.get("skillId")), skillIds.size()));
                predicates.add(cb.in(root.get("id")).value(skillSubquery));
            }

            // Advanced Sorting: Proficiency Rank
            Sort.Order proficiencyOrder = pageable.getSort().getOrderFor("proficiency");
            if (proficiencyOrder != null) {
                // Subquery for Sorting Value (Max proficiency for the filtered skill or global max)
                Subquery<Integer> sortSubquery = query.subquery(Integer.class);
                Root<com.group5.ems.entity.EmployeeSkill> sortEsRoot = sortSubquery.from(com.group5.ems.entity.EmployeeSkill.class);
                sortSubquery.select(cb.max(sortEsRoot.get("proficiency")));
                
                Predicate sortSkillPred = (skillIds != null && !skillIds.isEmpty()) 
                    ? sortEsRoot.get("skillId").in(skillIds)
                    : cb.conjunction();
                
                sortSubquery.where(
                    cb.equal(sortEsRoot.get("employee"), root),
                    sortSkillPred
                );

                if (proficiencyOrder.isDescending()) {
                    query.orderBy(cb.desc(cb.coalesce(sortSubquery, 0)), cb.desc(root.get("hireDate")));
                } else {
                    query.orderBy(cb.asc(cb.coalesce(sortSubquery, 0)), cb.desc(root.get("hireDate")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // If sorting by proficiency, we've already set the order in the query via Criteria
        // We need a Pageable without the 'proficiency' sort to avoid Hibernate mapping errors
        Pageable effectivePageable = pageable;
        if (pageable.getSort().getOrderFor("proficiency") != null) {
            effectivePageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize()
            );
        }

        Page<Employee> page = employeeRepository.findAll(spec, effectivePageable);
        List<HrEmployeeDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        List<Long> empIds = dtos.stream().map(HrEmployeeDTO::id).toList();
        if (!empIds.isEmpty()) {
            List<com.group5.ems.entity.EmployeeSkill> empSkills = employeeSkillRepository.findByEmployeeIdInWithSkill(empIds);
            java.util.Map<Long, List<String>> skillsMap = empSkills.stream()
                .collect(Collectors.groupingBy(
                    com.group5.ems.entity.EmployeeSkill::getEmployeeId,
                    Collectors.mapping(es -> "Lvl " + es.getProficiency() + " • " + es.getSkill().getName(), Collectors.toList())
                ));

            for (int i = 0; i < dtos.size(); i++) {
                HrEmployeeDTO dto = dtos.get(i);
                List<String> matchingSkills = skillsMap.getOrDefault(dto.id(), new java.util.ArrayList<>());
                
                // If a filter is applied, ONLY show the specific skills that were filtered for
                if (skillIds != null && !skillIds.isEmpty()) {
                    matchingSkills = empSkills.stream()
                        .filter(es -> es.getEmployeeId().equals(dto.id()) && skillIds.contains(es.getSkillId()))
                        .map(es -> "Lvl " + es.getProficiency() + " • " + es.getSkill().getName())
                        .distinct()
                        .toList();
                }
                
                dtos.set(i, new HrEmployeeDTO(
                    dto.id(), dto.initials(), dto.fullName(), dto.position(), dto.department(), 
                    dto.code(), dto.status(), dto.email(), dto.phone(), dto.avatarUrl(), matchingSkills
                ));
            }
        }

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    public HrEmployeeDetailDTO getEmployeeDetail(Long id) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String initials = "";
        String fullName = "Unknown";
        String email = "N/A";
        String phone = "N/A";
        String username = "N/A";

        if (employee.getUser() != null) {
            fullName = employee.getUser().getFullName() != null ? employee.getUser().getFullName() : fullName;
            email = employee.getUser().getEmail() != null ? employee.getUser().getEmail() : email;
            phone = employee.getUser().getPhone() != null ? employee.getUser().getPhone() : phone;
            username = employee.getUser().getUsername() != null ? employee.getUser().getUsername() : username;
            if (!"Unknown".equals(fullName) && !fullName.trim().isEmpty()) {
                String[] names = fullName.trim().split("\\s+");
                initials += names[0].charAt(0);
                if (names.length > 1) {
                    initials += names[names.length - 1].charAt(0);
                }
            }
        }

        String departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
        String positionName = employee.getPosition() != null ? employee.getPosition().getName() : "N/A";

        // Get latest salary
        BigDecimal baseSalary = BigDecimal.ZERO;
        BigDecimal allowance = BigDecimal.ZERO;
        String salaryType = "N/A";
        LocalDate salaryEffectiveFrom = null;

        List<Salary> salaries = employee.getSalaries();
        if (salaries != null && !salaries.isEmpty()) {
            Salary latest = salaries.stream()
                    .max(Comparator.comparing(Salary::getEffectiveFrom))
                    .orElse(null);
            if (latest != null) {
                baseSalary = latest.getBaseAmount() != null ? latest.getBaseAmount() : BigDecimal.ZERO;
                allowance = latest.getAllowanceAmount() != null ? latest.getAllowanceAmount() : BigDecimal.ZERO;
                salaryType = latest.getSalaryType() != null ? latest.getSalaryType() : "N/A";
                salaryEffectiveFrom = latest.getEffectiveFrom();
            }
        }

        // Get latest contract
        String contractType = "N/A";
        LocalDate contractStart = null;
        LocalDate contractEnd = null;
        String contractStatus = "N/A";

        List<Contract> contracts = employee.getContracts();
        if (contracts != null && !contracts.isEmpty()) {
            Contract latest = contracts.stream()
                    .max(Comparator.comparing(Contract::getStartDate))
                    .orElse(null);
            if (latest != null) {
                contractType = latest.getContractType() != null ? latest.getContractType() : "N/A";
                contractStart = latest.getStartDate();
                contractEnd = latest.getEndDate();
                contractStatus = latest.getStatus() != null ? latest.getStatus() : "N/A";
            }
        }

        // Fetch skills
        List<com.group5.ems.dto.response.HrEmployeeSkillDTO> skills = employeeSkillRepository.findByEmployeeId(id).stream()
                .map(es -> com.group5.ems.dto.response.HrEmployeeSkillDTO.builder()
                        .skillId(es.getSkillId())
                        .name(es.getSkill().getName())
                        .category(es.getSkill().getCategory())
                        .proficiency(es.getProficiency())
                        .build())
                .collect(Collectors.toList());

        // Fetch performance reviews
        List<com.group5.ems.dto.response.HrEmployeePerformanceDTO> performanceReviews = performanceReviewRepository.findByEmployeeIdOrderByCreatedAtDesc(id).stream()
                .filter(pr -> pr.getStatus() != null && pr.getStatus().equalsIgnoreCase("PUBLISHED"))
                .map(pr -> com.group5.ems.dto.response.HrEmployeePerformanceDTO.builder()
                        .id(pr.getId())
                        .reviewPeriod(pr.getReviewPeriod())
                        .performanceScore(pr.getPerformanceScore())
                        .potentialScore(pr.getPotentialScore())
                        .status(pr.getStatus())
                        .reviewerName(pr.getReviewer() != null && pr.getReviewer().getUser() != null ? pr.getReviewer().getUser().getFullName() : "System")
                        .createdAt(pr.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Fetch reward and disciplines
        List<com.group5.ems.dto.response.HrEmployeeDisciplineDTO> disciplines = rewardDisciplineRepository.findByEmployeeIdOrderByDecisionDateDesc(id).stream()
                .map(d -> com.group5.ems.dto.response.HrEmployeeDisciplineDTO.builder()
                        .id(d.getId())
                        .recordType(d.getRecordType())
                        .title(d.getTitle())
                        .description(d.getDescription())
                        .decisionDate(d.getDecisionDate())
                        .amount(d.getAmount())
                        .decidedBy(d.getDecidedByUser() != null ? d.getDecidedByUser().getFullName() : "N/A")
                        .build())
                .collect(Collectors.toList());

        HrEmployeeDetailDTO dto = HrEmployeeDetailDTO.builder()
                .id(employee.getId())
                .initials(initials.toUpperCase())
                .avatarUrl(employee.getUser() != null ? employee.getUser().getAvatarUrl() : null)
                .fullName(fullName)
                .code(employee.getEmployeeCode())
                .department(departmentName)
                .position(positionName)
                .status(employee.getStatus())
                .hireDate(employee.getHireDate())
                .email(email)
                .phone(phone)
                .username(username)
                .baseSalary(baseSalary)
                .allowance(allowance)
                .salaryType(salaryType)
                .salaryEffectiveFrom(salaryEffectiveFrom)
                .contractType(contractType)
                .contractStart(contractStart)
                .contractEnd(contractEnd)
                .contractStatus(contractStatus)
                .skills(skills)
                .performanceReviews(performanceReviews)
                .disciplines(disciplines)
                .build();

        return dto;
    }

    @Transactional
    public void updateJobInfo(Long employeeId, Long departmentId, Long positionId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (departmentId != null) {
            com.group5.ems.entity.Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            employee.setDepartmentId(dept.getId());
        } else {
            employee.setDepartmentId(null);
        }

        if (positionId != null) {
            com.group5.ems.entity.Position pos = positionRepository.findById(positionId)
                    .orElseThrow(() -> new RuntimeException("Position not found"));
            
            // Track promotion if position changes
            if (!pos.getId().equals(employee.getPositionId())) {
                employee.setPreviousPositionId(employee.getPositionId());
                employee.setPromotionDate(LocalDate.now());
                employee.setPositionId(pos.getId());
            }
        } else {
            throw new RuntimeException("Position is required");
        }

        employeeRepository.save(employee);
        logService.log(AuditAction.UPDATE, AuditEntityType.EMPLOYEE, employee.getId());
    }


    private HrEmployeeDTO mapToDTO(Employee employee) {
        String initials = "";
        String fullName = "Unknown";
        String email = "N/A";
        String phone = "N/A";
        if (employee.getUser() != null) {
            fullName = employee.getUser().getFullName() != null ? employee.getUser().getFullName() : fullName;
            email = employee.getUser().getEmail() != null ? employee.getUser().getEmail() : email;
            phone = employee.getUser().getPhone() != null ? employee.getUser().getPhone() : phone;
            if (!"Unknown".equals(fullName) && !fullName.trim().isEmpty()) {
                String[] names = fullName.trim().split("\\s+");
                initials += names[0].charAt(0);
                if (names.length > 1) {
                    initials += names[names.length - 1].charAt(0);
                }
            }
        }

        String departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A";
        String positionName = employee.getPosition() != null ? employee.getPosition().getName() : "N/A";

        return HrEmployeeDTO.builder()
                .id(employee.getId())
                .initials(initials.toUpperCase())
                .fullName(fullName)
                .position(positionName)
                .department(departmentName)
                .code(employee.getEmployeeCode())
                .status(employee.getStatus())
                .email(email)
                .phone(phone)
                .avatarUrl(employee.getUser() != null ? employee.getUser().getAvatarUrl() : null)
                .build();
    }
}
