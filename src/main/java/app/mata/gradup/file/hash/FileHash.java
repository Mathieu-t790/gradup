package app.mata.gradup.file.hash;

import app.mata.gradup.PojaGenerated;

@PojaGenerated
public record FileHash(FileHashAlgorithm algorithm, String value) {}
