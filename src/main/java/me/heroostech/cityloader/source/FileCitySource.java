package me.heroostech.cityloader.source;

import lombok.SneakyThrows;
import me.heroostech.cityloader.CityLoaderJava;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public record FileCitySource(Path path) implements CitySource {
    @SneakyThrows
    @Override
    public @NotNull InputStream load() {
        if (!Files.exists(path)) {
            // No world folder

            if (Files.isDirectory(path.getParent().resolve(FilenameUtils.removeExtension(String.valueOf(path))))) {
                CityLoaderJava.getInstance().getLogger().info("Path is an anvil world. Converting! (This might take a bit)");

                CityLoaderJava.getInstance().convertAnvilToCity(path.getParent().resolve(FilenameUtils.removeExtension(String.valueOf(path))), new FileCitySource(path));
                CityLoaderJava.getInstance().getLogger().info("Converted!");
            } else {
                CityLoaderJava.getInstance().getLogger().error("Path doesn't exist!");
            }
        }

        return Files.newInputStream(path);
    }

    @SneakyThrows
    @Override
    public void save(byte[] bytes) {
        Files.write(path, bytes);
    }
}