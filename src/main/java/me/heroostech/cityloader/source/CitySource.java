package me.heroostech.cityloader.source;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public interface CitySource {

     @NotNull InputStream load();

     void save(byte[] bytes);

}