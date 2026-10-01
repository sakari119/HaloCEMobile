package com.halo.decomp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;

final class MapData {
    private static final int HEADER_SIZE = 0x800;

    private MapData() {}

    static boolean isValidCacheFile(File file) {
        if (!file.isFile() || file.length() < HEADER_SIZE)
            return false;

        byte[] header = new byte[HEADER_SIZE];
        try (FileInputStream input = new FileInputStream(file)) {
            int count = 0;
            while (count < header.length) {
                int read = input.read(header, count, header.length - count);
                if (read <= 0)
                    return false;
                count += read;
            }
        } catch (IOException exception) {
            return false;
        }

        int fileLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(8);
        if (fileLength < 0 || fileLength > 0x11600000)
            return false;
        int nameEnd = 0x20;
        while (nameEnd < 0x40 && header[nameEnd] != 0)
            nameEnd++;
        if (nameEnd == 0x40)
            return false;

        return header[0] == 'd' && header[1] == 'a' && header[2] == 'e' && header[3] == 'h'
            && header[0x7FC] == 't' && header[0x7FD] == 'o' && header[0x7FE] == 'o'
            && header[0x7FF] == 'f';
    }

    static String problem(File dataRoot) {
        if (dataRoot == null)
            return "App storage is unavailable.";

        File maps = new File(dataRoot, "maps");
        if (!new File(maps, "ui.map").isFile())
            return "The game data is missing.";

        File[] files = maps.listFiles((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".map"));
        if (files == null)
            return "The maps folder cannot be read.";

        Arrays.sort(files, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        StringBuilder invalid = new StringBuilder();
        for (File file : files) {
            if (isValidCacheFile(file))
                continue;
            if (invalid.length() != 0)
                invalid.append(", ");
            invalid.append(file.getName());
        }
        return invalid.length() == 0 ? null : "The saved maps are damaged: " + invalid + ".";
    }
}
