package ru.practicum.model;

import lombok.*;
import jakarta.persistence.*;


/**
 * Объект для хранения координат места проведения события.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    private Float lat;
    private Float lon;
}
