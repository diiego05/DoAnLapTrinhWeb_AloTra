package com.alotra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDTO {

    @NotNull
    private Long orderItemId;

    @NotNull
    private Long productId;

    @Min(1)
    private int rating;

    @NotBlank
    @Size(min = 50, message = "Nội dung đánh giá phải có ít nhất 50 ký tự")
    private String content;
}
