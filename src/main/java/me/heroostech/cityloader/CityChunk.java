package me.heroostech.cityloader;

import lombok.Getter;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.batch.ChunkBatch;

import java.util.Arrays;
import java.util.List;

public final class CityChunk {
    @Getter private final ChunkBatch chunkBatch;
    @Getter private final int maxSection;
    @Getter private final int minSection;

    @Getter private final List<Section> sections;

    public CityChunk(ChunkBatch chunkBatch, int maxSection, int minSection) {
        this.chunkBatch = chunkBatch;
        this.maxSection = maxSection;
        this.minSection = minSection;

        int sect = maxSection - minSection;

        Section[] sectionArray = new Section[sect];

        for(int i = 0; i < sect; i++) {
            Section section = new Section();
            sectionArray[i] = section;
        }

        this.sections = Arrays.stream(sectionArray).toList();
    }
}
