package com.group5.ems.service.guest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.group5.ems.dto.request.ApplyJobRequestDTO;
import com.group5.ems.dto.response.ApplicationResponseDTO;
import com.group5.ems.entity.Application;
import com.group5.ems.entity.Candidate;
import com.group5.ems.entity.CandidateCv;
import com.group5.ems.entity.CompanyInfo;
import com.group5.ems.entity.Department;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.CandidateCvRepository;
import com.group5.ems.repository.CandidateRepository;
import com.group5.ems.repository.CompanyInfoRepository;
import com.group5.ems.repository.DepartmentRepository;
import com.group5.ems.repository.JobPostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final CandidateRepository candidateRepository;
    private final JobPostRepository jobPostRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final CandidateCvRepository candidateCvRepository;
    private final ApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;

    // =============================
    // CANDIDATES
    // =============================

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public List<Candidate> searchCandidate(String email, String phone) {
        return candidateRepository
                .findByEmailContainingOrPhoneContaining(email, phone);
    }

    // create candidate if not exist
    public Candidate createCandidateIfNotExist(
            String fullName,
            String email,
            String phone,
            String address,
            LocalDate dateOfBirth,
            String introduction,
            Integer yearsExperience,
            BigDecimal expectedSalary) {

        Optional<Candidate> existed = candidateRepository.findByEmailAndPhone(email, phone);

        if (existed.isPresent()) {
            return existed.get();
        }

        Candidate candidate = new Candidate();

        candidate.setFullName(fullName);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        candidate.setAddress(address);
        candidate.setDateOfBirth(dateOfBirth);
        candidate.setIntroduction(introduction);
        candidate.setYearsExperience(yearsExperience);
        candidate.setExpectedSalary(expectedSalary);

        return candidateRepository.save(candidate);
    }

    // =============================
    // JOBS
    // =============================

    public List<JobPost> getOpenJobs() {
        return jobPostRepository.findByStatus("OPEN");
    }

    public List<JobPost> getJobsByDepartment(Long id) {
        return jobPostRepository.findByDepartmentId(id);
    }

    public JobPost getJobDetail(Long id) {
        return jobPostRepository.findById(id).orElse(null);
    }

    public long countJobsByDepartment(Long deptId) {
        return jobPostRepository.countByDepartment(deptId);
    }

    // =============================
    // COMPANY INFO
    // =============================

    public List<CompanyInfo> getPublicCompanyInfo() {
        return companyInfoRepository.findByIsPublicTrue();
    }

    // =============================
    // Department
    // =============================

    public long getDepartmentCount() {
        return jobPostRepository.countDistinctDepartment();
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // =============================
    // APPLICATIONS
    // =============================

    public List<Application> getApplicationsByCandidate(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId);
    }

    // =============================
    // CV
    // =============================

    // upload cv
    public CandidateCv uploadCv(Long candidateId, MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        CandidateCv cv = new CandidateCv();

        cv.setCandidateId(candidateId);
        cv.setFileName(file.getOriginalFilename());
        cv.setFileType(file.getContentType());
        cv.setFileData(file.getBytes());

        return candidateCvRepository.save(cv);
    }

    // view candidate cvs
    public List<CandidateCv> getCandidateCvs(Long candidateId) {
        return candidateCvRepository.findByCandidateId(candidateId);
    }

    // =============================
    // APPLY JOB
    // =============================

    public Application applyJob(Long candidateId, Long jobId, Long cvId) {

        Optional<Application> existed = applicationRepository.findByJobPostIdAndCandidateId(jobId, candidateId);

        if (existed.isPresent()) {
            return existed.get();
        }

        Application app = new Application();

        app.setCandidateId(candidateId);
        app.setJobPostId(jobId);
        app.setCvId(cvId);

        String token = UUID.randomUUID().toString().replace("-", "");
        app.setTrackingToken(token);

        return applicationRepository.save(app);
    }

    // =============================
    // FULL APPLY FLOW
    // =============================

    public ApplicationResponseDTO applyJobFullFlow(
            ApplyJobRequestDTO request) throws Exception {

        Candidate candidate = createCandidateIfNotExist(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress(),
                request.getDateOfBirth(),
                request.getIntroduction(),
                request.getYearsExperience(),
                request.getExpectedSalary());

        CandidateCv cv = uploadCv(candidate.getId(), request.getFile());

        Application app = applyJob(candidate.getId(), request.getJobId(), cv.getId());

        return new ApplicationResponseDTO(
                app.getId(),
                app.getCandidateId(),
                app.getJobPostId(),
                app.getCvId(),
                app.getTrackingToken());
    }

    // =============================
    // TRACK APPLICATION
    // =============================

    public ApplicationResponseDTO trackApplicationDTO(String token) {

        Application app = applicationRepository.findByTrackingToken(token);

        if (app == null) {
            return null;
        }

        return new ApplicationResponseDTO(
                app.getId(),
                app.getCandidateId(),
                app.getJobPostId(),
                app.getCvId(),
                app.getTrackingToken());
    }
}