package com.garbigo.collection.dto;

import com.garbigo.collection.model.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCreateRequest {

    @NotBlank
    private String collectionRequestId;

    @NotNull
    private ComplaintCategory category;

    @NotBlank
    private String description;
}