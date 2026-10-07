package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class AsyncFileWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger("baity-async-writer");
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "baity-file-writer");
        thread.setDaemon(true);
        return thread;
    });

    private AsyncFileWriter() {
    }

    public static void write(String label, Path directory, Path file, Tag root) {
        WRITER.execute(() -> {
            try {
                Files.createDirectories(directory);
                try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(file))) {
                    NbtIo.writeUnnamedTagWithFallback(root, output);
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to save {}: {}", label, exception.toString());
            }
        });
    }

    public static void writeLater(String label, Path directory, Path file, Supplier<Tag> encoder) {
        WRITER.execute(() -> {
            try {
                Tag root = encoder.get();
                Files.createDirectories(directory);
                try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(file))) {
                    NbtIo.writeUnnamedTagWithFallback(root, output);
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to save {}: {}", label, exception.toString());
            }
        });
    }
}
