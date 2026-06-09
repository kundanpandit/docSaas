package com.docsaas.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class MergePdfRequestDTO {
    private List<Long> fileIds;   // IDs of PDFs to merge
}
