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

        public Map<String, Object> getTeamMockData() {
                Map<String, Object> data = new HashMap<>();

                Map<String, String> manager = new HashMap<>();
                manager.put("name", "Marcus Thorne");
                manager.put("role", "Operations Manager");
                manager.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBqwQXs0h175jU3uh3FhKRdzGE3Nl1I6Pgzyq7GXasySRzzcXSBy5POHZeZM_tH82Vlp9Hu_GjbhkuXoZU_Clt4cUru6YspBR1wTGQAbsE50KrLXGV6NtJgi7Bg6O4fABG6g3akaXqIfIGP_-qzg1jgQHlDW-7asQ2QMS0Dpifs_E-AcNgemm3DJnnc8wk8rbkUYAF_BLdX2Smm03gADNQP1M44hUyN5SjpN07FF9GbSZUM9rGMQ-vd4wTleWw8FCvWohN7qCxsk9E");
                data.put("manager", manager);

                List<Map<String, String>> members = new ArrayList<>();

                Map<String, String> member1 = new HashMap<>();
                member1.put("name", "John Smith");
                member1.put("email", "john.smith@hrmpro.com");
                member1.put("role", "Senior Analyst");
                member1.put("rating", "Exceeds Expectations");
                member1.put("ratingClass", "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400");
                member1.put("status", "Active");
                member1.put("statusDot", "bg-green-500");
                member1.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuAx3bm_6ROku45Qad2UC6L8WqGYQTSxbQfGbrIsZyy-UW0G-0eeaUe05OzGGUPVXtUgSAXYY1km4lsQ8OMlKocQqnLvoWylgqv8HhjdOhc-kA7_Y9WGXOHncHiVIom2GDXi5UFfTRWNw-kIM5Tj5rLVJx3alhzAv1liLktNE8Zt65-kYJuInGPkWm85aD_STgeoCKnakLN1ZpxNfG-GLOhHh26_zxMgT8NQ21STEfw2DrFNb7ygWY6IQKmzRFuP-NmzVNfiEHO9zvA");
                members.add(member1);

                Map<String, String> member2 = new HashMap<>();
                member2.put("name", "Sarah Chen");
                member2.put("email", "s.chen@hrmpro.com");
                member2.put("role", "Project Coordinator");
                member2.put("rating", "Meets Expectations");
                member2.put("ratingClass", "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400");
                member2.put("status", "On Leave");
                member2.put("statusDot", "bg-amber-500");
                member2.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuA7G1b01aHHL0AJL0yUVoFZMCKU9bDDMteeaCXpHNE9SpXfXv6Z7-6xBidTFosgqi23DHGBl-nVCRdYX6RBMQy2B4aCg16AQq38KdtKZ_rIiuNEKa-YgfktCU6yTarPxouN0nWllLaZvLSa8VexvbMBYQY_VLQTIa6Qk1rrqPPnaiHqvDsTosCDx_Xxukef-DFybP0rhm6nLiCE-s6SbSLN2of9xinrPm5uzAAWsJ4YvlQQGOOgnAwLWjjgCJKsAy04FfHVI7FFSIY");
                members.add(member2);

                Map<String, String> member3 = new HashMap<>();
                member3.put("name", "Michael Ross");
                member3.put("email", "m.ross@hrmpro.com");
                member3.put("role", "Junior Developer");
                member3.put("rating", "Developing");
                member3.put("ratingClass", "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400");
                member3.put("status", "Active");
                member3.put("statusDot", "bg-green-500");
                member3.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuDSyHUdl6eG9dyfarZDFUD5MXvV0zQ0JX7jTznxXDo8wxPkSRn71gj--7CMISBlaXuXXVHEy-nOfVMmSuFeeb98p5OSJXBu1rhs19LctISJn2fZh-ORSbw65N7R1ciXAViqhwGaEIuSjqFgxRTLWqes0ax240fVJh6jMEJrLTto_n7wHAo-7MfIPsIlHM666cZRI0D7WE9HGuJ7jBNyYHYxinysZBUf0UNEhsFYrNpoFgQGrWPFgacsEoRo7JOBBuHr_8frtV26R8c");
                members.add(member3);

                Map<String, String> member4 = new HashMap<>();
                member4.put("name", "Elena Rodriguez");
                member4.put("email", "elena.r@hrmpro.com");
                member4.put("role", "UX Designer");
                member4.put("rating", "Exceeds Expectations");
                member4.put("ratingClass", "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400");
                member4.put("status", "Active");
                member4.put("statusDot", "bg-green-500");
                member4.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuAS1uGqByS-wP9J8zYCks7y6MjZq7qF7nC86UkV8vq1BsbcBmqMUKwKMds9dzUCiIg_-cxrVxFcAMw7KeRpfWEMJveSbCbtIfC-LaH8bJ6nTihi2Y-Md-1uOyK6HPRHeTGoRWTpuvN6MKBFhypgjT_0aQg0aPGNQIr214vJv3X6SGSCin5IYml7kb6sE0uJAADiFOFIFww5-bohosFolP19XdlA1HUIcsDgpOUT9PHeWD2Du6i6JsoRJALpr5kLY7HnLJNibrnvJ6Y");
                members.add(member4);

                Map<String, String> member5 = new HashMap<>();
                member5.put("name", "David Kim");
                member5.put("email", "d.kim@hrmpro.com");
                member5.put("role", "QA Engineer");
                member5.put("rating", "Meets Expectations");
                member5.put("ratingClass", "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400");
                member5.put("status", "Active");
                member5.put("statusDot", "bg-green-500");
                member5.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuCWP5ma7zd2kxZX7A5nvpWTD4qdUdCPiQNjCwhDWrwQeMzUcbgL-TJzk18xVoK_xx9DscNHneOTKzKdceHe0rucBqYJuNIWph3_GHHKmaw36JUFjtLcrb0Eg235qQEsB8J_yJqNerqK1U10gurvspYV2FDpjXiWtRrxbdr77BKcGv3PuQIyx5Z9kOlsAyepNYWAAWehtgfpQdhgYPLt2xFzDvSLo1HDuz2a0ue9gxWOsRM6grbqCloLYUMubcTBJMZnlEEOBkjYtTQ");
                members.add(member5);

                data.put("teamMembers", members);

                return data;
        }

        public Map<String, Object> getDepartmentMockData() {
                Map<String, Object> data = new HashMap<>();

                Map<String, String> manager = new HashMap<>();
                manager.put("name", "Marcus Thorne");
                manager.put("role", "Operations Manager");
                manager.put("avatarUrl",
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBqwQXs0h175jU3uh3FhKRdzGE3Nl1I6Pgzyq7GXasySRzzcXSBy5POHZeZM_tH82Vlp9Hu_GjbhkuXoZU_Clt4cUru6YspBR1wTGQAbsE50KrLXGV6NtJgi7Bg6O4fABG6g3akaXqIfIGP_-qzg1jgQHlDW-7asQ2QMS0Dpifs_E-AcNgemm3DJnnc8wk8rbkUYAF_BLdX2Smm03gADNQP1M44hUyN5SjpN07FF9GbSZUM9rGMQ-vd4wTleWw8FCvWohN7qCxsk9E");
                data.put("manager", manager);

                // Core Department Details
                Map<String, String> department = new HashMap<>();
                department.put("name", "Operations");
                department.put("code", "OPS-001");
                department.put("description",
                                "Ensures smooth execution of core business processes, resource allocation, and daily administrative activities.");
                department.put("manager", "Marcus Thorne");
                department.put("totalEmployees", "24");
                department.put("openPositions", "3");
                department.put("budgetUtilization", "85%");
                data.put("department", department);

                // Sub-departments / Teams
                List<Map<String, String>> teams = new ArrayList<>();

                Map<String, String> team1 = new HashMap<>();
                team1.put("name", "Logistics & Supply");
                team1.put("headcount", "12");
                team1.put("lead", "Sarah Chen");
                teams.add(team1);

                Map<String, String> team2 = new HashMap<>();
                team2.put("name", "Facilities Management");
                team2.put("headcount", "8");
                team2.put("lead", "David Kim");
                teams.add(team2);

                Map<String, String> team3 = new HashMap<>();
                team3.put("name", "Quality Assurance");
                team3.put("headcount", "4");
                team3.put("lead", "Elena Rodriguez");
                teams.add(team3);

                data.put("teams", teams);

                // Required Positions in Department
                List<Map<String, String>> positions = new ArrayList<>();

                Map<String, String> pos1 = new HashMap<>();
                pos1.put("title", "Operations Manager");
                pos1.put("headcount", "1");
                pos1.put("status", "Filled");
                pos1.put("statusClass", "bg-green-100 text-green-700");
                positions.add(pos1);

                Map<String, String> pos2 = new HashMap<>();
                pos2.put("title", "Team Lead");
                pos2.put("headcount", "3");
                pos2.put("status", "Filled");
                pos2.put("statusClass", "bg-green-100 text-green-700");
                positions.add(pos2);

                Map<String, String> pos3 = new HashMap<>();
                pos3.put("title", "Logistics Coordinator");
                pos3.put("headcount", "5");
                pos3.put("status", "Recruiting (2)");
                pos3.put("statusClass", "bg-amber-100 text-amber-700");
                positions.add(pos3);

                Map<String, String> pos4 = new HashMap<>();
                pos4.put("title", "Quality Inspector");
                pos4.put("headcount", "4");
                pos4.put("status", "Filled");
                pos4.put("statusClass", "bg-green-100 text-green-700");
                positions.add(pos4);

                Map<String, String> pos5 = new HashMap<>();
                pos5.put("title", "Facilities Admin");
                pos5.put("headcount", "2");
                pos5.put("status", "Recruiting (1)");
                pos5.put("statusClass", "bg-amber-100 text-amber-700");
                positions.add(pos5);

                data.put("positions", positions);

                return data;
        }
}
