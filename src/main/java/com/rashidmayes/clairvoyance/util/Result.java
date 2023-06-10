package com.rashidmayes.clairvoyance.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<DATA, ERROR> {

    private final DATA data;
    private final ERROR error;

    public static <DATA, ERROR> Result<DATA, ERROR> of(DATA data) {
        return new Result<>(data, null);
    }

    public static <DATA, ERROR> Result<DATA, ERROR> error(ERROR error) {
        return new Result<>(null, error);
    }

    public boolean hasError() {
        return error != null;
    }

}
