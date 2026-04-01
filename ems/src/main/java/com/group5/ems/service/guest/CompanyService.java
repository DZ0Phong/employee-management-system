package com.group5.ems.service.guest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.group5.ems.dto.response.RewardSpotlightDTO;
import com.group5.ems.entity.CompanyInfo;
import com.group5.ems.entity.User;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.CompanyInfoRepository;
import com.group5.ems.repository.RewardDisciplineRepository;
import com.group5.ems.service.common.LogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyInfoRepository companyInfoRepository;
    private final RewardDisciplineRepository rewardDisciplineRepository;
    private final LogService logService;

    // ─── Dùng cho trang Home (news) và About ────────────────────────
    public List<CompanyInfo> getPublicCompanyInfo() {
        return companyInfoRepository.findByIsPublicTrue();
    }

    // ─── Lấy tất cả (admin) ─────────────────────────────────────────
    public List<CompanyInfo> getAllCompanyInfo() {
        return companyInfoRepository.findAll();
    }

    // ─── Lấy theo key (cho Hero / Stats đặc biệt) ───────────────────
    public Optional<CompanyInfo> getByKey(String key) {
        return companyInfoRepository.findByInfoKey(key);
    }

    /**
     * Trả về Map<key, content> để Thymeleaf dùng dễ hơn:
     * ${heroData['hero_title']}
     */
    public Map<String, String> getHomeConfigMap() {
        List<String> specialKeys = List.of(
                "hero_title", "hero_subtitle",
                "stats_employees", "stats_offices", "stats_founded", "stats_rating",
                "cta_title", "cta_subtitle");
        return companyInfoRepository.findAll()
                .stream()
                .filter(c -> specialKeys.contains(c.getInfoKey()))
                .collect(Collectors.toMap(
                        CompanyInfo::getInfoKey,
                        c -> c.getContent() != null ? c.getContent() : ""));
    }

    // ─── CRUD (admin) ────────────────────────────────────────────────
    public CompanyInfo createCompanyInfo(
            String infoKey, String title, String content, boolean isPublic, Long actorId) {

        if (companyInfoRepository.existsByInfoKey(infoKey)) {
            throw new IllegalArgumentException("Info key '" + infoKey + "' đã tồn tại.");
        }
        CompanyInfo info = new CompanyInfo();
        info.setInfoKey(infoKey);
        info.setTitle(title);
        info.setContent(content);
        info.setIsPublic(isPublic);
        info.setUpdatedBy(actorId);
        CompanyInfo saved = companyInfoRepository.save(info);

        // ── Audit log ──────────────────────────────────────────
        logService.log(AuditAction.CREATE, AuditEntityType.COMPANY, saved.getId());

        return saved;
    }

    public CompanyInfo updateCompanyInfo(
            Long id, String title, String content, boolean isPublic, Long actorId) {

        CompanyInfo info = companyInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục id=" + id));
        info.setTitle(title);
        info.setContent(content);
        info.setIsPublic(isPublic);
        info.setUpdatedBy(actorId);
        CompanyInfo saved = companyInfoRepository.save(info);

        // ── Audit log ──────────────────────────────────────────
        logService.log(AuditAction.UPDATE, AuditEntityType.COMPANY, saved.getId());

        return saved;
    }

    public void deleteCompanyInfo(Long id) {
        CompanyInfo info = companyInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục id=" + id));

        // ── Audit log trước khi xoá (sau khi xoá thì id không còn) ──
        logService.log(AuditAction.DELETE, AuditEntityType.COMPANY, id);

        companyInfoRepository.delete(info);
    }

    public List<RewardSpotlightDTO> getTopRewards(int limit) {
    return rewardDisciplineRepository
        .findTopRewardsWithEmployee("REWARD")
        .stream()
        .limit(limit)
        .map(r -> {
            String name     = "Employee";
            String initials = "EE";
            String dept     = "EMS Pro";

            if (r.getEmployee() != null) {
                // fullName nằm trong User, không phải Employee
                User user = r.getEmployee().getUser();
                if (user != null && user.getFullName() != null) {
                    name = user.getFullName();
                    String[] parts = name.trim().split("\\s+");
                    if (parts.length >= 2) {
                        initials = (parts[0].substring(0, 1)
                                 + parts[parts.length - 1].substring(0, 1))
                                 .toUpperCase();
                    } else {
                        initials = name.substring(0, Math.min(2, name.length()))
                                      .toUpperCase();
                    }
                }

                if (r.getEmployee().getDepartment() != null) {
                    dept = r.getEmployee().getDepartment().getName();
                }
            }

            return new RewardSpotlightDTO(
                name, initials, dept,
                r.getTitle(), r.getAmount(), r.getDecisionDate()
            );
        })
        .toList();
}
}
