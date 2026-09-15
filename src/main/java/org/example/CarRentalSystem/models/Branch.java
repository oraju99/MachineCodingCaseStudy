package org.example.CarRentalSystem.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class Branch {
    private final String id;
    private final String name;

    public Branch(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
