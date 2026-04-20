package com.group5.ems.service.hr;

import com.group5.ems.dto.response.hr.TopPerformerDTO;
import com.group5.ems.entity.RewardDiscipline;
import com.group5.ems.enums.AuditAction;
import com.group5.ems.enums.AuditEntityType;
import com.group5.ems.repository.PerformanceReviewRepository;
import com.group5.ems.repository.RewardDisciplineRepository;
import com.group5.ems.service.common.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class YearEndRewardScheduler {

    private final PerformanceReviewRepository performanceReviewRepository;
    private final RewardDisciplineRepository rewardDisciplineRepository;
    private final LogService logService;

    @Autowired
    public YearEndRewardScheduler(PerformanceReviewRepository performanceReviewRepository,
                                  RewardDisciplineRepository rewardDisciplineRepository,
                                  LogService logService) {
        this.performanceReviewRepository = performanceReviewRepository;
        this.rewardDisciplineRepository = rewardDisciplineRepository;
        this.logService = logService;
    }

    @Scheduled(cron = "0 59 23 31 12 ?")
    @Transactional
    public void generateYearEndRewards() {
        int currentYear = LocalDate.now().getYear();

        // Note: As requested by the user, we DO NOT delete past runs. We ONLY create.
        
        var topPerformers = performanceReviewRepository.findTopPerformersByYear(currentYear);
        if (topPerformers.isEmpty()) {
            return;
        }

        List<RewardDiscipline> rewardsToSave = new ArrayList<>();

        int currentRank = 1;
        int employeesAtCurrentRank = 0;
        Double previousScore = null;

        for (var result : topPerformers) {
            Long empId = result.employeeId();
            Double avgScore = result.averageScore();

            if (previousScore == null || Double.compare(avgScore, previousScore) != 0) {
                if (previousScore != null) {
                    currentRank += employeesAtCurrentRank;
                }
                employeesAtCurrentRank = 1;
                previousScore = avgScore;
            } else {
                employeesAtCurrentRank++;
            }

            if (currentRank > 3) {
                break; // We only reward top 3 ranks
            }

            BigDecimal amount = switch (currentRank) {
                case 1 -> new BigDecimal("10000000"); // Top 1: 10M
                case 2 -> new BigDecimal("5000000");  // Top 2: 5M
                case 3 -> new BigDecimal("3000000");  // Top 3: 3M
                default -> BigDecimal.ZERO;
            };

            RewardDiscipline reward = new RewardDiscipline();
            reward.setEmployeeId(empId);
            reward.setRecordType("REWARD");
            reward.setTitle("Top Performer of the Year " + currentYear + " (Rank " + currentRank + ")");
            reward.setDescription("""
                Automated reward for highest performance average in %d. 
                Rank: %d
                Score: %.2f
                """.formatted(currentYear, currentRank, avgScore));
            reward.setDecisionDate(LocalDate.now());
            reward.setAmount(amount);
            reward.setDecidedBy(null); // System generated

            rewardsToSave.add(reward);
        }

        if (!rewardsToSave.isEmpty()) {
            rewardDisciplineRepository.saveAll(rewardsToSave);
            
            // Log the action for each reward created
            for (RewardDiscipline reward : rewardsToSave) {
                try {
                    logService.log(AuditAction.CREATE, AuditEntityType.EVENT, reward.getId(), null);
                } catch (Exception e) {
                   // Fallback if logService requires valid user context in SecurityContext
                   System.err.println("""
                       Could not log automated reward: %s
                       """.formatted(e.getMessage()));
                }
            }
        }
    }
}
