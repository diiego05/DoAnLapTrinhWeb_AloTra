package com.alotra.service;

import com.alotra.entity.PromotionalCampaign;
import com.alotra.repository.PromotionalCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionalCampaignRepository campaignRepo;
    private final CloudinaryService cloudinaryService;

    // ==================== 📌 Helper: Sinh slug từ tên ====================
    private String generateSlug(String input) {
        if (input == null || input.isBlank()) return null;

        // Bỏ dấu tiếng Việt
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("");

        // Loại ký tự đặc biệt, chuyển khoảng trắng thành "-"
        slug = slug.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        return slug;
    }

    public List<PromotionalCampaign> getAll() {
        return campaignRepo.findAll();
    }

    public PromotionalCampaign get(Long id) {
        return campaignRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));
    }

    public PromotionalCampaign create(String name, String description, String type,
    		 BigDecimal value, LocalDateTime start, LocalDateTime end,
                                      MultipartFile bannerFile) {
        PromotionalCampaign c = new PromotionalCampaign();
        c.setName(name);
        c.setSlug(generateSlug(name)); // ✅ tự sinh slug
        c.setDescription(description);
        c.setType(PromotionalCampaign.CampaignType.valueOf(type));
        c.setValue(value);
        c.setStartAt(start);
        c.setEndAt(end);

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadFile(bannerFile);
            c.setBannerUrl(url);
        }

        return campaignRepo.save(c);
    }

    public PromotionalCampaign update(Long id, String name, String description, String type,
                                      BigDecimal value, LocalDateTime start, LocalDateTime end,
                                      MultipartFile bannerFile) {
        PromotionalCampaign c = get(id);
        c.setName(name);
        c.setSlug(generateSlug(name)); // ✅ update slug nếu đổi tên
        c.setDescription(description);
        c.setType(PromotionalCampaign.CampaignType.valueOf(type));
        c.setValue(value);
        c.setStartAt(start);
        c.setEndAt(end);

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String url = cloudinaryService.uploadFile(bannerFile);
            c.setBannerUrl(url);
        }

        return campaignRepo.save(c);
    }

    public void delete(Long id) {
        campaignRepo.deleteById(id);
    }

    public List<PromotionalCampaign> getActiveCampaigns() {
        return campaignRepo.findActiveCampaigns(LocalDateTime.now());
    }
}
