package com.example.hds;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;

public class KeyInputHandler {
    private final AtomicBoolean isGenerating = new AtomicBoolean(false);

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW_PRESS) return;

        int key = event.getKey();
        int modifiers = event.getModifiers();

        if (key == GLFW_KEY_F2 && (modifiers & GLFW_MOD_ALT) != 0) {
            if (isGenerating.get()) {
                sendChatMessage("§cAlready generating a HD screenshot, please wait...");
                return;
            }

            sendChatMessage("§eCapturing screen and generating ~1.8GB TGA (keeping aspect ratio)...");
            isGenerating.set(true);

            CompletableFuture<ByteBuffer> captureFuture = new CompletableFuture<>();
            Minecraft.getInstance().execute(() -> {
                try {
                    ByteBuffer rawBuffer = TGAScreenshotGenerator.captureRawPixels();
                    captureFuture.complete(rawBuffer);
                } catch (Exception e) {
                    captureFuture.completeExceptionally(e);
                }
            });

            captureFuture.thenAcceptAsync(rawBuffer -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    RenderTarget mainRenderTarget = mc.getMainRenderTarget();
                    int srcWidth = mainRenderTarget.width;
                    int srcHeight = mainRenderTarget.height;

                    Path screenshotDir = mc.gameDirectory.toPath().resolve("screenshots");
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    Path outputFile = screenshotDir.resolve("hds_screenshot_" + timestamp + ".tga");

                    long start = System.currentTimeMillis();
                    long fileSize = TGAScreenshotGenerator.generateFromRaw(rawBuffer, srcWidth, srcHeight, outputFile);
                    long duration = System.currentTimeMillis() - start;

                    double sizeMB = fileSize / (1024.0 * 1024.0);
                    sendChatMessage("§aHD screenshot saved: " + outputFile.getFileName());
                    sendChatMessage(String.format("§aFile size: %.2f MB (%.2f GB) – generated in %.1f seconds",
                            sizeMB, sizeMB / 1024.0, duration / 1000.0));
                } catch (IOException e) {
                    sendChatMessage("§cFailed to generate HD screenshot: " + e.getMessage());
                    HDSScreenshotMod.LOGGER.error("HD screenshot generation failed", e);
                } catch (Exception e) {
                    sendChatMessage("§cUnexpected error: " + e.getMessage());
                    HDSScreenshotMod.LOGGER.error("Unexpected error during HD screenshot", e);
                } finally {
                    isGenerating.set(false);
                }
            }).exceptionally(ex -> {
                sendChatMessage("§cFailed to capture screen: " + ex.getMessage());
                HDSScreenshotMod.LOGGER.error("Screen capture failed", ex);
                isGenerating.set(false);
                return null;
            });
        }
    }

    private void sendChatMessage(String message) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal(message), false);
            }
        });
    }
}