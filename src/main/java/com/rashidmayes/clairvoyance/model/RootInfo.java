package com.rashidmayes.clairvoyance.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RootInfo implements Identifiable {

    private final String connectionString;

    @Override
    public String getId() {
        return "$root." + connectionString;
    }

}
