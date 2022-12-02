package me.heroostech.cityloader;

import com.github.luben.zstd.Zstd;
import kotlin.collections.CollectionsKt;
import kotlin.io.path.PathsKt;
import kotlin.text.StringsKt;
import lombok.Getter;
import lombok.SneakyThrows;
import me.heroostech.cityloader.source.CitySource;
import me.heroostech.citystom.Extension;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public final class CityLoaderJava extends Extension {

    @Getter private static CityLoaderJava instance;

    @Override
    public void preInitialize() {
        instance = this;
    }

    @SneakyThrows
    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {

    }

    /**
     * Saves a .CITY world from chunks
     */
    @SneakyThrows
    public void createCityFile(Collection<Chunk> chunks, CitySource source) {
        BinaryWriter writer = new BinaryWriter();

        writer.writeInt(chunks.size());

        chunks.forEach(it -> {
            writer.writeInt(it.getChunkX());
            writer.writeInt(it.getChunkZ());

            writer.writeByte((byte) it.getMinSection());
            writer.writeByte((byte) it.getMaxSection());

            it.getSections().forEach(section -> {
                int airSkip = 0;
                boolean needsEnding = false;
                for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                    for (int y = 0; y < Chunk.CHUNK_SECTION_SIZE; y++) {
                        for (int z = 0; z < Chunk.CHUNK_SIZE_X; z++) {
                            Block block = it.getBlock(x, y + ((it.getSections().indexOf(section) + it.getMinSection()) * Chunk.CHUNK_SECTION_SIZE), z);

                            if (block == Block.AIR) {
                                airSkip++;
                                if (airSkip == 1) {
                                    writer.writeShort((short) 0);
                                    needsEnding = true;
                                }

                                continue;
                            }
                            if (airSkip > 0) {
                                writer.writeInt(airSkip);
                                needsEnding = false;
                            }

                            airSkip = 0;

                            NBT nbt = block.nbt();

                            writer.writeShort(block.stateId());
                            writer.writeBoolean(block.hasNbt());
                            if (nbt != null) {
                                writer.writeNBT("blockNBT", nbt);
                            }
                        }
                    }
                }

                // Air skip sometimes isn't written, maybe there is a cleaner way?
                if (needsEnding) {
                    writer.writeInt(airSkip);
                }

                writer.writeByteArray(section.getBlockLight());
                writer.writeByteArray(section.getSkyLight());
            });
        });

        byte[] bytes = writer.toByteArray();
        byte[] compressed = Zstd.compress(bytes);

        source.save(compressed);

        writer.close();
        writer.flush();
    }

    /**
     * Converts an anvil folder to a CITY file
     * <p>
     * Source support should be added
     */
    @SneakyThrows
    public void convertAnvilToCity(Path pathToAnvil, CitySource citySaveSource) {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        Set<Path> mcaFiles = Files.list(pathToAnvil.resolve("region")).collect(
                Collectors.toSet());

        InstanceContainer convertInstance = instanceManager.createInstanceContainer();
        IChunkLoader loader = new ConversionAnvilLoader(pathToAnvil);
        convertInstance.setChunkLoader(loader);

        CountDownLatch countDownLatch = new CountDownLatch((mcaFiles.size()) * 32 * 32); // each MCA file contains 32 chunks
        Set<Chunk> chunks = ConcurrentHashMap.newKeySet();

        mcaFiles.forEach(it -> {
            List<String> args = CollectionsKt.takeLast(StringsKt.split(PathsKt.getNameWithoutExtension(it), new String[]{"."}, false, 0), 2);
            int rX = Integer.parseInt(args.get(0));
            int rZ = Integer.parseInt(args.get(1));

            for (int x = rX * 32; x < rX * 32 + 32; x++) {
                for (int z = rZ * 32; z < rZ * 32 + 32; z++) {
                    convertInstance.loadChunk(x, z).thenAcceptAsync(chunk -> {

                        //for(Section section : chunk.getSections()) {
                            //if(section.blockPalette().count() > 0) {
                                chunks.add(chunk);
                                //break;
                            //}
                        //}

                        countDownLatch.countDown();
                    });
                }
            }
        });

        long before = System.nanoTime();
        countDownLatch.await();
        System.out.println("Took " + (System.nanoTime() - before) / 1_000_000 + "ms to convert");

        // TODO: make source independant
        createCityFile(chunks, citySaveSource);

        instanceManager.unregisterInstance(convertInstance);
    }
}