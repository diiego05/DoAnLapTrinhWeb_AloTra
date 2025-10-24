package com.alotra.dto;

import java.util.List;

public record ReviewResponseDTO(
        Long id,
        Long orderItemId,
        Long productId,
        String content,
        int rating,
        String userName,
        List<String> mediaUrls
) {}
