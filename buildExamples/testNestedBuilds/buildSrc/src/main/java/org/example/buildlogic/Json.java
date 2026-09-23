package org.example.buildlogic;

import com.google.gson.Gson;

public final class Json {
    private Json() {
    }

    public static String of(Object value) {
        return new Gson().toJson(value);
    }
}
