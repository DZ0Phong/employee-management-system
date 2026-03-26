import java.nio.file.*;
import java.util.*;
import java.io.IOException;

public class ReplaceAside {
    public static void main(String[] args) throws IOException {
        String dir = "ems/src/main/resources/templates/hr";
        Map<String, String> map = new LinkedHashMap<>();
        map.put("dashboard.html", "dashboard");
        map.put("attendance.html", "attendance");
        map.put("bank-details.html", "employees");
        map.put("employees.html", "employees");
        map.put("leave.html", "leave");
        map.put("payroll-periods.html", "payroll-periods");
        map.put("payroll-preview.html", "payroll-periods");
        map.put("payroll-review.html", "payroll-periods");
        map.put("payroll.html", "payroll");
        map.put("performance.html", "performance");
        map.put("recruitment.html", "recruitment");
        map.put("requests.html", "requests");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            Path path = Paths.get(dir, entry.getKey());
            if (Files.exists(path)) {
                String content = new String(Files.readAllBytes(path), "UTF-8");
                int start = content.indexOf("<aside");
                int end = content.indexOf("</aside>");
                if (start != -1 && end != -1) {
                    end += "</aside>".length();
                    String replacement = "<!-- ══════════════════════════════════════════════════════════ -->\n        <!-- SIDEBAR                                                   -->\n        <!-- ══════════════════════════════════════════════════════════ -->\n        <div th:replace=\"~{fragments/hr-sidebar :: sidebar('" + entry.getValue() + "')}\"></div>";
                    String newContent = content.substring(0, start) + replacement + content.substring(end);
                    Files.write(path, newContent.getBytes("UTF-8"));
                    System.out.println("Updated " + entry.getKey());
                } else {
                    System.out.println("Could not find <aside> in " + entry.getKey());
                }
            } else {
                System.out.println("File not found: " + path);
            }
        }
    }
}
