package com.example.hds;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.*;

public class TGAScreenshotGenerator {

    public static ByteBuffer captureRawPixels() {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();
        int width = mainRenderTarget.width;
        int height = mainRenderTarget.height;

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        glReadBuffer(GL_FRONT);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        return buffer;
    }

    private static int[] computeTargetSizeKeepRatio(int srcWidth, int srcHeight) {
        long targetPixels = (long) ((1.8 * 1024 * 1024 * 1024 - 18) / 3);
        double scale = Math.sqrt((double) targetPixels / (srcWidth * srcHeight));
        int targetWidth = (int) Math.round(srcWidth * scale);
        int targetHeight = (int) Math.round(srcHeight * scale);
        return new int[]{targetWidth, targetHeight};
    }

    public static long generateFromRaw(ByteBuffer rawBuffer, int srcWidth, int srcHeight, Path outputPath) throws IOException {
        int[] targetSize = computeTargetSizeKeepRatio(srcWidth, srcHeight);
        int targetWidth = targetSize[0];
        int targetHeight = targetSize[1];

        HDSScreenshotMod.LOGGER.info("Upscaling from {}x{} to {}x{} (keep ratio, ~1.8GB)", srcWidth, srcHeight, targetWidth, targetHeight);

        Files.createDirectories(outputPath.getParent());
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputPath), 256 * 1024)) {
            writeTGAHeader(os, targetWidth, targetHeight);

            double xScale = (double) srcWidth / targetWidth;
            double yScale = (double) srcHeight / targetHeight;
            byte[] targetRow = new byte[targetWidth * 3];

            for (int targetY = 0; targetY < targetHeight; targetY++) {
                int srcY = (int) ((targetHeight - 1 - targetY) * yScale);
                if (srcY < 0) srcY = 0;
                if (srcY >= srcHeight) srcY = srcHeight - 1;
                int srcRowStart = srcY * srcWidth * 4;

                for (int targetX = 0; targetX < targetWidth; targetX++) {
                    int srcX = (int) (targetX * xScale);
                    if (srcX >= srcWidth) srcX = srcWidth - 1;
                    int srcIdx = srcRowStart + srcX * 4;
                    byte r = rawBuffer.get(srcIdx);
                    byte g = rawBuffer.get(srcIdx + 1);
                    byte b = rawBuffer.get(srcIdx + 2);
                    targetRow[targetX * 3] = b;     // B
                    targetRow[targetX * 3 + 1] = g; // G
                    targetRow[targetX * 3 + 2] = r; // R
                }
                os.write(targetRow);
            }
        }

        long totalBytes = 18L + (long) targetWidth * targetHeight * 3L;
        HDSScreenshotMod.LOGGER.info("Wrote {} bytes to {}", totalBytes, outputPath);
        return totalBytes;
    }

    private static void writeTGAHeader(OutputStream os, int width, int height) throws IOException {
        byte[] header = new byte[18];
        header[0] = 0;
        header[1] = 0;
        header[2] = 2;
        // color map spec (5 bytes)
        header[3] = 0; header[4] = 0; header[5] = 0; header[6] = 0; header[7] = 0;
        // x origin
        header[8] = 0; header[9] = 0;
        // y origin
        header[10] = 0; header[11] = 0;
        // width (little-endian)
        header[12] = (byte) (width & 0xFF);
        header[13] = (byte) ((width >> 8) & 0xFF);
        // height (little-endian)
        header[14] = (byte) (height & 0xFF);
        header[15] = (byte) ((height >> 8) & 0xFF);
        // bits per pixel
        header[16] = 24;
        // descriptor
        header[17] = 0;
        os.write(header);
    }
}