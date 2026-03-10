package com.group5.ems.controller.guest;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.group5.ems.entity.Candidate;
import com.group5.ems.entity.JobPost;
import com.group5.ems.service.guest.GuestService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestController {
   
    
     @GetMapping("")
    public String getMethodName() {
        return "";
    }
    

    private final GuestService guestService;

    // View all candidates
    @GetMapping("/candidates")
    public String viewCandidates(Model model) {

        model.addAttribute("candidates",
                guestService.getAllCandidates());

        return "guest/candidates";
    }

    // search candidate
    @PostMapping("/search")
    public String findCandidate(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            Model model) {

        List<Candidate> candidates =
                guestService.searchCandidate(email, phone);

        model.addAttribute("candidates", candidates);

        return "guest/candidates";
    }

    // job list
    @GetMapping("/jobs")
    public String viewJobs(Model model) {

        model.addAttribute("jobs",
                guestService.getOpenJobs());

        return "guest/jobs";
    }

    // job by department
    @GetMapping("/jobs/department/{id}")
    public String findByDepartmentId(
            @PathVariable Long id,
            Model model) {

        model.addAttribute("jobs",
                guestService.getJobsByDepartment(id));

        return "guest/jobs";
    }

    // job detail
    @GetMapping("/jobs/{id}")
    public String viewJobDetail(
            @PathVariable Long id,
            Model model) {

        JobPost job = guestService.getJobDetail(id);

        if (job == null) {
            return "redirect:/guest/jobs";
        }

        model.addAttribute("job", job);

        return "guest/job-detail";
    }

    // company info
    @GetMapping("/info")
    public String viewCompanyInfo(Model model) {

        model.addAttribute("info",
                guestService.getPublicCompanyInfo());

        return "guest/company-info";
    }

    // view applications
    @GetMapping("/applications/{candidateId}")
    public String viewApplications(
            @PathVariable Long candidateId,
            Model model) {

        model.addAttribute("applications",
                guestService.getApplicationsByCandidate(candidateId));

        return "guest/applications";
    }

    // upload CV
    @PostMapping("/upload-cv")
    public String uploadCv(
            @RequestParam Long candidateId,
            @RequestParam("file") MultipartFile file) {

        try {

            guestService.uploadCv(candidateId, file);

            return "redirect:/guest/success";

        } catch (Exception e) {

            return "redirect:/guest/error";
        }
    }

    // view candidate cv
    @GetMapping("/candidate-cv/{candidateId}")
    public String viewCandidateCv(
            @PathVariable Long candidateId,
            Model model) {

        model.addAttribute("cvs",
                guestService.getCandidateCvs(candidateId));

        return "guest/candidate-cv";
    }
}