package com.alotra.repository;

import com.alotra.entity.BranchInventory;
import com.alotra.entity.BranchInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, BranchInventoryId> {

    @Modifying
    @Transactional
    @Query("DELETE FROM BranchInventory bi WHERE bi.variantId = :variantId")
    void deleteByVariantId(Long variantId);

    boolean existsByBranchIdAndVariantIdAndStatus(Long branchId, Long variantId, String status); // ✅ thêm dòng này
}
