package com.group5.ems.service.guest;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.group5.ems.entity.Application;
import com.group5.ems.entity.Candidate;
import com.group5.ems.entity.CandidateCv;
import com.group5.ems.entity.CompanyInfo;
import com.group5.ems.entity.JobPost;
import com.group5.ems.repository.ApplicationRepository;
import com.group5.ems.repository.CandidateCvRepository;
import com.group5.ems.repository.CandidateRepository;
import com.group5.ems.repository.CompanyInfoRepository;
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
    private final FileStorageService fileStorageService;

    // candidates
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public List<Candidate> searchCandidate(String email, String phone) {
        return candidateRepository
                .findByEmailContainingOrPhoneContaining(email, phone);
    }

    // jobs
    public List<JobPost> getOpenJobs() {
        return jobPostRepository.findByStatus("OPEN");
    }

    public List<JobPost> getJobsByDepartment(Long id) {
        return jobPostRepository.findByDepartmentId(id);
    }

    public JobPost getJobDetail(Long id) {
        return jobPostRepository.findById(id).orElse(null);
    }

    // company info
    public List<CompanyInfo> getPublicCompanyInfo() {
        return companyInfoRepository.findByIsPublicTrue();
    }

    // applications
    public List<Application> getApplicationsByCandidate(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId);
    }

    // upload cv
    public void uploadCv(Long candidateId, MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileName = fileStorageService.uploadFile(file);

        CandidateCv cv = new CandidateCv();
        cv.setCandidateId(candidateId);
        cv.setFileName(file.getOriginalFilename());
        cv.setFilePath(fileName);

        candidateCvRepository.save(cv);
    }

    // view cv
    public List<CandidateCv> getCandidateCvs(Long candidateId) {
        return candidateCvRepository.findByCandidateId(candidateId);
    }
}