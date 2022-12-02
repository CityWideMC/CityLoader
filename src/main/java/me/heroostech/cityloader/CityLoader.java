package me.heroostech.cityloader;

import com.github.luben.zstd.Zstd;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.SneakyThrows;
import me.heroostech.cityloader.source.CitySource;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.*;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.utils.binary.BinaryReader;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.CompressedProcesser;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTReader;
import java.util.concurrent.CompletableFuture;

public final class CityLoader implements IChunkLoader {

    @Getter private final Instance instance;
    @Getter private final CitySource citySource;
    @Getter private final Point offset;
    private final Long2ObjectOpenHashMap<CityChunk> chunksMap = new Long2ObjectOpenHashMap<>();

    @SneakyThrows
    public CityLoader(Instance instance, CitySource citySource, Point offset) {
        this.instance = instance;
        this.citySource = citySource;
        this.offset = offset;

        BlockManager blockManager = MinecraftServer.getBlockManager();

        byte[] byteArray = citySource.load().readAllBytes();
        byte[] decompressed = Zstd.decompress(byteArray, (int) Zstd.decompressedSize(byteArray));
        BinaryReader reader = new BinaryReader(decompressed);
        NBTReader nbtReader = new NBTReader(reader, CompressedProcesser.NONE);

        int chunks = reader.readInt();

        for (int i = 0; i < chunks; i++) {
            ChunkBatch batch = new ChunkBatch();

            int chunkX = reader.readInt();
            int chunkZ = reader.readInt();

            int minSection = reader.readByte();
            int maxSection = reader.readByte();

            CityChunk mstChunk = new CityChunk(batch, maxSection, minSection);

            for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
                int airSkip = 0;
                Section section = mstChunk.getSections().get(sectionY - minSection);

                for (int x = 0; i < Chunk.CHUNK_SIZE_X; i++) {
                    for (int y = 0; y < Chunk.CHUNK_SECTION_SIZE; y++) {
                        for (int z = 0; z < Chunk.CHUNK_SIZE_X; z++) {
                            if (airSkip > 0) {
                                airSkip--;
                                continue;
                            }

                            short stateId = reader.readShort();

                            if (stateId == 0) {
                                airSkip = reader.readInt() - 1;
                                continue;
                            }

                            boolean hasNbt = reader.readBoolean();

                            Block block;

                            if (hasNbt) {
                                NBT nbt = nbtReader.read();

                                Block b = Block.fromStateId(stateId);
                                block = b.withHandler(blockManager.getHandlerOrDummy(b.name())).withNbt((NBTCompound) nbt);
                            } else {
                                block = Block.fromStateId(stateId);
                            }

                            // TODO: fix X and Z offset
                            batch.setBlock(x/* + offset.blockX()*/, y + (sectionY * 16) + offset.blockY(), z/* + offset.blockZ()*/, block);
                        }
                    }
                }

                byte[] blockLights = reader.readByteArray();
                byte[] skyLights = reader.readByteArray();
                section.setBlockLight(blockLights);
                section.setSkyLight(skyLights);
            }

            chunksMap.put(ChunkUtils.getChunkIndex(chunkX, chunkZ), mstChunk);
        }

        reader.close();
        nbtReader.close();
    }

    @Override
    public @NotNull CompletableFuture<@Nullable Chunk> loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        CityChunk mstChunk = chunksMap.get(ChunkUtils.getChunkIndex(chunkX, chunkZ));
        if(mstChunk == null)
            return CompletableFuture.completedFuture(null);
        DynamicChunk chunk = new DynamicChunk(instance, chunkX, chunkZ);

        CompletableFuture<Chunk> future = new CompletableFuture<>();

        // Copy chunk light from mstChunk to the new chunk
        chunk.getSections().forEach(it -> {
            Section sec = mstChunk.getSections().get(chunk.getSections().indexOf(it));
            it.setBlockLight(sec.getBlockLight());
            it.setSkyLight(sec.getSkyLight());
        });

        mstChunk.getChunkBatch().apply(instance, chunk, future::complete);

        instance.saveChunksToStorage();

        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> saveChunk(@NotNull Chunk chunk) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean supportsParallelLoading() {
        return true;
    }

    @Override
    public boolean supportsParallelSaving() {
        return true;
    }
}