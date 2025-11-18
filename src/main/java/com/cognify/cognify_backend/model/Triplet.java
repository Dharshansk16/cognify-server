package com.cognify.cognify_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Triplet {
    private String subject;
    private String predicate;
    private String object;
    private String personaId;
}
