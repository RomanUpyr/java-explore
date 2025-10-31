package ru.practicum.dto;

import lombok.*;
import java.util.List;

/**
 * DTO для информации об ошибке
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private List<String> errors;
    private String message;
    private String reason;
    private String status;
    private String timestamp;
}
