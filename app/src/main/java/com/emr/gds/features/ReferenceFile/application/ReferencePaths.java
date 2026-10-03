package com.emr.gds.features.ReferenceFile.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ReferencePaths {
    private ReferencePaths() {}

    public static Optional<Path> resolve(Path base, String relative) {
        if (base == null || relative == null || relative.isBlank()) return Optional.empty();
        String normalized = relative.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) return Optional.empty();
        try {
            Path basePath = base.toAbsolutePath().normalize();
            Path candidate = basePath.resolve(normalized).normalize();
            if (!candidate.startsWith(basePath)) return Optional.empty();
            // Check existing ancestors too, so links cannot escape the base folder.
            Path ancestor = candidate;
            while (ancestor != null && !Files.exists(ancestor)) {
                if (Files.isSymbolicLink(ancestor)) return Optional.empty();
                ancestor = ancestor.getParent();
            }
            if (Files.exists(basePath) && ancestor != null
                    && !ancestor.toRealPath().startsWith(basePath.toRealPath())) return Optional.empty();
            return Optional.of(candidate);
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
