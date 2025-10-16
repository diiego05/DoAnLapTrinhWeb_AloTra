package com.alotra.controller.api;

import com.alotra.dto.BranchDTO;
import com.alotra.entity.Branch;
import com.alotra.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchApiController {

    private final BranchRepository branchRepository;

    @GetMapping
    public List<BranchDTO> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private BranchDTO mapToDTO(Branch b) {
        return new BranchDTO(
                b.getId(),
                b.getName(),
                b.getSlug(),
                b.getAddress(),
                b.getPhone(),
                b.getStatus()
        );
    }
}
