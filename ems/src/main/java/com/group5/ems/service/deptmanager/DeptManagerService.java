package com.group5.ems.service.deptmanager;

import com.group5.ems.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeptManagerService {

    private final DepartmentRepository departmentRepository;

    public int getTeamSize(Long managerId) {
        return departmentRepository.findByManagerId(managerId).size();
    }

    public Map<String, Object> getDashboardMockData() {
        Map<String, Object> data = new HashMap<>();
        data.put("teamSize", 24);
        data.put("newApprovals", 3);
        data.put("pendingApprovals", 8);
        data.put("teamAttendance", "92%");
        data.put("nextReview", "Oct 15");

        Map<String, String> manager = new HashMap<>();
        manager.put("name", "Alex Johnson");
        manager.put("role", "Manager");
        data.put("manager", manager);

        List<Map<String, String>> activities = new ArrayList<>();

        Map<String, String> emp1 = new HashMap<>();
        emp1.put("name", "Sarah Chen");
        emp1.put("title", "Sr. Software Engineer");
        emp1.put("avatarUrl",
                "https://lh3.googleusercontent.com/aida-public/AB6AXuC5CGURyOdKZg1nPS5tSotOqt9DZB78eRGH3eg3EC-4AMB4rdDXy9jeQr4qOj7ChckGmlmMGLtDvSGTG-xjpnI3_Nfylvv685wy7ZZt-l-5S7goNULPP3vcdleWBtr01Pep3ZA9mtCX5hkoy6OkrIFGl8u9d9KfmPZQw2_aut4hAoECaycxw2Oz1wyAZA4-eKUk6Jbrk2Maoj0rN7YEVuVvYssm7pQ4monLETA3SqC89B2yDAt2DK9icMHRpTDW18H21JOyHXMqMQA");
        emp1.put("status", "Active");
        emp1.put("statusClass", "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400");
        emp1.put("attendance", "98%");
        emp1.put("lastReview", "Aug 12, 2023");
        activities.add(emp1);

        Map<String, String> emp2 = new HashMap<>();
        emp2.put("name", "Marcus Lee");
        emp2.put("title", "UI Designer");
        emp2.put("avatarUrl",
                "https://lh3.googleusercontent.com/aida-public/AB6AXuBs-HI0oyOlrbksbCAUaIvDOfrdW5Ox78e4MEJxDgtXDobtrwyWmYhDENzlNy8um84LLmRNZItpEZ6r3b6mgU5twMihJE-bNP3XKgA9Qnqr7KpPa2SnDZlMo1kgsKl1T_72T0n1NmtO7RC1YnGzLUJi5-PfWGuIIwQm8iW8M1aZmTj8u5TgTloLhjm2KqOCqH4hoghqRybQ-QkITsgLJ_ToezRId4ND-mu-TK5M2v0sfGRfNBeCNUdKzAduCD6wWzQsVDDk5H9W9ec");
        emp2.put("status", "On Leave");
        emp2.put("statusClass", "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400");
        emp2.put("attendance", "85%");
        emp2.put("lastReview", "Jul 20, 2023");
        activities.add(emp2);

        Map<String, String> emp3 = new HashMap<>();
        emp3.put("name", "Jamie Fox");
        emp3.put("title", "Project Manager");
        emp3.put("avatarUrl",
                "https://lh3.googleusercontent.com/aida-public/AB6AXuBJ9tUnPLQlzvrB_HxGeGFg6Yu8sA8xAqdspuwuXKPoSXfQPzy-fQ4sjOAhKCZol-M6PvSrbOeJP0BA5tqHFFxc706q20JvnRL-b13Jy7DAOLpboj7gCz364XOv2i19O3mEw3xdzIdRMUie8bD55Viw_BuniHhrLQXqqGrwBjGYO5CorjnHHz-BP0Cx56gBxOjcz8WJTdVv0jfzfeMLbtYbeULwnx1PaIeFbzDarsZEhPmpnLoJLUY2sarC9-0IyGDcA_pG1ylHGrY");
        emp3.put("status", "Active");
        emp3.put("statusClass", "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400");
        emp3.put("attendance", "94%");
        emp3.put("lastReview", "Sep 05, 2023");
        activities.add(emp3);

        data.put("recentTeamActivities", activities);

        return data;
    }
}
