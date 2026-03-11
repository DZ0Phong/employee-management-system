package com.group5.ems.controller.guest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.group5.ems.dto.request.ApplyJobRequestDTO;
import com.group5.ems.dto.request.ContactRequestDTO;
import com.group5.ems.dto.response.ApplicationResponseDTO;
import com.group5.ems.entity.CandidateCv;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.CandidateCvRepository;
import com.group5.ems.service.guest.EmailService;
import com.group5.ems.service.guest.GuestService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestController {

        private final GuestService guestService;
        private final CandidateCvRepository candidateCvRepository;
        private final EmailService emailService;

        // =============================
        // COMPANY INFO
        // =============================

        @GetMapping("/info")
        public String viewCompanyInfo(Model model) {

                model.addAttribute("info",
                                guestService.getPublicCompanyInfo());

                return "guest/company-info";
        }

        // =============================
        // APPLICATIONS
        // =============================

        @GetMapping("/applications/{candidateId}")
        public String viewApplications(
                        @PathVariable Long candidateId,
                        Model model) {

                model.addAttribute("applications",
                                guestService.getApplicationsByCandidate(candidateId));

                return "guest/applications";
        }

        // =============================
        // APPLY JOB FULL FLOW
        // =============================

        @PostMapping(value = "/apply-full", consumes = "multipart/form-data")
        @ResponseBody
        public ApplicationResponseDTO applyFull(
                        @ModelAttribute ApplyJobRequestDTO request) throws Exception {

                return guestService.applyJobFullFlow(request);
        }

        // apply when candidate already exists
        @PostMapping("/apply")
        @ResponseBody
        public String applyJob(
                        @RequestParam Long candidateId,
                        @RequestParam Long jobId,
                        @RequestParam Long cvId) {

                guestService.applyJob(candidateId, jobId, cvId);

                return "ok";
        }

        // =============================
        // CANDIDATE CVS
        // =============================

        @GetMapping("/candidate-cv/{candidateId}")
        public String viewCandidateCv(
                        @PathVariable Long candidateId,
                        Model model) {

                model.addAttribute("cvs",
                                guestService.getCandidateCvs(candidateId));

                return "guest/candidate-cv";
        }

        // =============================
        // HOME
        // =============================

        @GetMapping({ "", "/" })
        public String home(Model model) {

                model.addAttribute("companyNews",
                                guestService.getPublicCompanyInfo());

                model.addAttribute("featuredJobs",
                                guestService.getOpenJobs()
                                                .stream()
                                                .limit(6)
                                                .toList());

                model.addAttribute("openCount",
                                guestService.getOpenJobs().size());

                model.addAttribute("deptCount",
                                guestService.getDepartmentCount());

                return "guest/index";
        }

        // =============================
        // JOB LIST
        // =============================

        @GetMapping("/jobs")
        public String jobs(Model model) {

                List<JobPost> jobs = guestService.getOpenJobs();
                List<Department> departments = guestService.getAllDepartments();

                Map<Long, Long> deptCounts = new HashMap<>();

                for (Department d : departments) {
                        deptCounts.put(d.getId(),
                                        guestService.countJobsByDepartment(d.getId()));
                }

                model.addAttribute("jobs", jobs);
                model.addAttribute("departments", departments);
                model.addAttribute("deptCounts", deptCounts);
                model.addAttribute("openCount", jobs.size());

                return "guest/jobs";
        }

        // jobs by department
        @GetMapping("/jobs/department/{id}")
        public String jobsByDepartment(
                        @PathVariable Long id,
                        Model model) {

                model.addAttribute("jobs",
                                guestService.getJobsByDepartment(id));

                model.addAttribute("departments",
                                guestService.getAllDepartments());

                model.addAttribute("openCount",
                                guestService.getOpenJobs().size());

                return "guest/jobs";
        }

        // job detail
        @GetMapping("/jobs/{id}")
        public String jobDetail(
                        @PathVariable Long id,
                        Model model) {

                JobPost job = guestService.getJobDetail(id);

                if (job == null) {
                        return "redirect:/guest/jobs";
                }

                model.addAttribute("jobs",
                                guestService.getOpenJobs());

                model.addAttribute("departments",
                                guestService.getAllDepartments());

                model.addAttribute("openCount",
                                guestService.getOpenJobs().size());

                model.addAttribute("openJobId", id);

                return "guest/jobs";
        }

        // =============================
        // ABOUT
        // =============================

        @GetMapping("/about")
        public String about(Model model) {

                model.addAttribute("companyInfoList",
                                guestService.getPublicCompanyInfo());

                return "guest/about";
        }

        // =============================
        // CONTACT
        // =============================

        @GetMapping("/contact")
        public String contact() {

                return "guest/contact";
        }

        @PostMapping("/contact/send")
        @ResponseBody
        public String sendContact(@RequestBody ContactRequestDTO request) {

                emailService.sendContactEmail(
                                request.getSenderName(),
                                request.getSenderEmail(),
                                request.getSenderPhone(),
                                request.getTopic(),
                                request.getMessage());

                return "success";
        }

        // =============================
        // TRACK APPLICATION
        // =============================

        // API for JS
        @GetMapping("/track/api/{token}")
        @ResponseBody
        public ApplicationResponseDTO trackApplicationApi(
                        @PathVariable String token) {

                return guestService.trackApplicationDTO(token);
        }

        @PostMapping("/test-upload")
        @ResponseBody
        public String testUpload(@RequestParam("file") MultipartFile file) {
                System.out.println(file.getOriginalFilename());
                return "ok";
        }

        @GetMapping("/cv/{id}")
        @ResponseBody
        public ResponseEntity<byte[]> downloadCv(@PathVariable Long id) {

                CandidateCv cv = candidateCvRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException("CV not found"));

                return ResponseEntity.ok()
                                .header("Content-Disposition",
                                                "attachment; filename=\"" + cv.getFileName() + "\"")
                                .header("Content-Type", cv.getFileType())
                                .body(cv.getFileData());
        }
}