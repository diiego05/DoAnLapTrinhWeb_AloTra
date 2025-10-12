package com.alotra.service;

import com.alotra.dto.CampaignTargetRequest;
import com.alotra.dto.PromotionTargetDTO;
import com.alotra.entity.Product;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.entity.PromotionTarget;
import com.alotra.repository.ProductRepository;
import com.alotra.repository.PromotionalCampaignRepository;
import com.alotra.repository.PromotionTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionTargetService {

    private final PromotionTargetRepository targetRepo;
    private final ProductRepository productRepo;
    private final PromotionalCampaignRepository campaignRepo;

    // 🟢 Lấy target theo Product ID (entity)
    public List<PromotionTarget> findByProductId(Long productId) {
        return targetRepo.findByProductId(productId);
    }

    // 🟢 Lấy target theo Campaign ID (entity)
    public List<PromotionTarget> findByCampaignId(Long campaignId) {
        return targetRepo.findByCampaign_Id(campaignId);
    }

    // 🟡 Thêm mới một target
    public PromotionTarget save(PromotionTarget t) {
        return targetRepo.save(t);
    }

    // 🔴 Xóa 1 target theo ID
    public void delete(Long id) {
        targetRepo.deleteById(id);
    }

    // 🔴 Xóa tất cả target theo Campaign ID
    public void deleteByCampaignId(Long campaignId) {
        targetRepo.deleteAll(findByCampaignId(campaignId));
    }

    // 🧭 Gán đối tượng áp dụng cho 1 chiến dịch
    public void setTargetsForCampaign(Long campaignId, CampaignTargetRequest request) {
        PromotionalCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch khuyến mãi"));

        // Xóa các target cũ trước khi lưu mới
        deleteByCampaignId(campaignId);

        List<Product> products;
        switch (request.getTargetType()) {
            case "ALL_PRODUCTS" -> {
                products = productRepo.findAll();
            }
            case "CATEGORY" -> {
                if (request.getCategoryId() == null) {
                    throw new IllegalArgumentException("categoryId không được null khi chọn CATEGORY");
                }
                products = productRepo.findByCategory_Id(request.getCategoryId());
            }
            case "PRODUCTS" -> {
                if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
                    throw new IllegalArgumentException("Danh sách productIds không được rỗng khi chọn PRODUCTS");
                }
                products = productRepo.findAllById(request.getProductIds());
            }
            default -> throw new IllegalArgumentException("Loại đối tượng áp dụng không hợp lệ");
        }

        // Tạo list target mới dựa theo productId
        List<PromotionTarget> targets = products.stream()
                .map(p -> PromotionTarget.builder()
                        .campaign(campaign)
                        .productId(p.getId())
                        .targetType(request.getTargetType())
                        .build())
                .toList();

        targetRepo.saveAll(targets);
    }

    // 🆕 Trả về DTO theo Campaign ID
    public List<PromotionTargetDTO> getTargetDTOsByCampaign(Long campaignId) {
        return findByCampaignId(campaignId).stream()
                .map(t -> new PromotionTargetDTO(
                        t.getId(),
                        t.getCampaign() != null ? t.getCampaign().getId() : null,
                        t.getCampaign() != null ? t.getCampaign().getName() : null,
                        t.getTargetType(),
                        t.getProductId()
                ))
                .collect(Collectors.toList());
    }

    // 🆕 Trả về DTO theo Product ID
    public List<PromotionTargetDTO> getTargetDTOsByProduct(Long productId) {
        return findByProductId(productId).stream()
                .map(t -> new PromotionTargetDTO(
                        t.getId(),
                        t.getCampaign() != null ? t.getCampaign().getId() : null,
                        t.getCampaign() != null ? t.getCampaign().getName() : null,
                        t.getTargetType(),
                        t.getProductId()
                ))
                .collect(Collectors.toList());
    }
}
