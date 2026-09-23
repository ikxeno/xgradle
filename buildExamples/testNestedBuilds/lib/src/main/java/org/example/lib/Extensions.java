package org.example.lib;

import org.apache.commons.io.FilenameUtils;

public final class Extensions {
    private Extensions() {
    }

    public static String of(String fileName) {
        return FilenameUtils.getExtension(fileName);
    }
}
