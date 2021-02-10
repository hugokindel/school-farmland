package com.ustudents.farmland.common;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.BufferUtils.createByteBuffer;

public class FileUtil {
    /**
     * Creates directories recursively if needed (meaning one or more folder doesn't exist).
     *
     * @param filePath The path to use.
     */
    public static void createDirectoryIfNeeded(String filePath) throws Exception {
        File saveDirectory = new File(filePath);

        if (!saveDirectory.exists()) {
            if (!saveDirectory.mkdirs()) {
                throw new Exception("Cannot create directory at path: " + filePath + "!");
            }
        }
    }

    /**
     * Read file at path to a memory buffer.
     *
     * @param filePath The file path.
     *
     * @return a buffer.
     */
    public static ByteBuffer readFile(String filePath) {
        ByteBuffer buffer;
        Path path = Paths.get(filePath);

        try {
            SeekableByteChannel fc = Files.newByteChannel(path);
            buffer = createByteBuffer((int)fc.size() + 1);
            while (fc.read(buffer) != -1) {}
            buffer.flip();
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}