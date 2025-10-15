package com.alotra.scheduler;

import com.alotra.entity.PromotionalCampaign;
import com.alotra.repository.PromotionalCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromotionStatusScheduler {

    private final PromotionalCampaignRepository campaignRepo;

    // 🕒 Chạy mỗi phút (60000ms)
    @Scheduled(fixedRate = 60000)
    public void autoUpdateCampaignStatus() {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionalCampaign> campaigns = campaignRepo.findAll();

        for (PromotionalCampaign c : campaigns) {
            // Nếu chưa bắt đầu → SCHEDULE
            if (now.isBefore(c.getStartAt())) {
                if (c.getStatus() != PromotionalCampaign.CampaignStatus.SCHEDULED) {
                    c.setStatus(PromotionalCampaign.CampaignStatus.SCHEDULED);
                }
            }
            // Nếu đã hết hạn → EXPIRED
            else if (now.isAfter(c.getEndAt())) {
                if (c.getStatus() != PromotionalCampaign.CampaignStatus.EXPIRED) {
                    c.setStatus(PromotionalCampaign.CampaignStatus.EXPIRED);
                }
            }
            // Nếu đang trong thời gian chạy → ACTIVE
            else {
                if (c.getStatus() != PromotionalCampaign.CampaignStatus.ACTIVE) {
                    c.setStatus(PromotionalCampaign.CampaignStatus.ACTIVE);
                }
            }
        }

        campaignRepo.saveAll(campaigns);
        System.out.println("✅ Promotion status auto-updated at " + now);
    }
}
