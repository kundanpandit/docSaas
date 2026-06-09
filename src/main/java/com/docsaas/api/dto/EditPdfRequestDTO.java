package com.docsaas.api.dto;

import lombok.Data;

@Data
public class EditPdfRequestDTO {
    private Long fileId;
    private String action; // ADD_TEXT, HIGHLIGHT, ADD_IMAGE
    private String value;  // text or image path (later)
    private int pageNumber;
    private float x;
    private float y;
}
