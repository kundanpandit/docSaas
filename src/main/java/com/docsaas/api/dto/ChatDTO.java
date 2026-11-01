package com.docsaas.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ChatDTO {

    private ChatDTO() {
    }

    public record Turn(
            @Pattern(regexp = "user|assistant", message = "Invalid chat role") String role,
            @NotBlank @Size(max = 4000, message = "Message is too long") String content) {
    }

    public record Request(
            @NotBlank(message = "Please type a question")
            @Size(max = 1000, message = "Questions can be at most 1000 characters") String question,
            @Valid @Size(max = 10, message = "Too many previous messages") List<Turn> history) {
    }

    /** {@code pages}: pages the answer cites, for "see page N" links in the UI. */
    public record Answer(String answer, List<Integer> pages) {
    }
}
