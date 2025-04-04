package net.infvoid.scenastic;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ScenasticClient implements ClientModInitializer {
    public static final String MOD_ID = "scenastic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private boolean screenshotCaptured = false;
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of(MOD_ID, "textures/gui/background.png");

    @Override
    public void onInitializeClient() {
        // Listen for the end of client tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().world == null && !screenshotCaptured) {
                screenshotCaptured = true;
                MinecraftClient.getInstance().execute(this::captureScreenshot); // Ensure thread safety
            }
        });

        // Listen for the start of client tick events
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().world != null && screenshotCaptured) {
                // Preload the texture and switch to a custom loading screen
                preloadBackgroundTexture();
                MinecraftClient.getInstance().setScreen(new CustomLoadingScreen(MinecraftClient.getInstance()));
                screenshotCaptured = false; // Reset flag for the next transition
            }
        });

        LOGGER.info("Scenastic client initialized!");
    }

    private void captureScreenshot() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Screen dimensions
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        // Allocate pixel buffers
        int[] pixels = new int[width * height];
        IntBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).asIntBuffer();

        try {
            // Read framebuffer data using OpenGL
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

            // Transfer the pixel buffer data to an array
            pixelBuffer.get(pixels);

            // Create a BufferedImage to hold the pixel data
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // Flip the image vertically and set the pixel data
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = x + (height - y - 1) * width; // Flip vertically
                    int pixel = pixels[i];

                    // Extract RGBA components and convert to ARGB
                    int r = (pixel >> 0) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = (pixel >> 16) & 0xFF;
                    int a = (pixel >> 24) & 0xFF;
                    image.setRGB(x, y, ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
                }
            }

            // Save the file to "screenshots/background.png"
            File screenshotFile = new File(client.runDirectory, "screenshots/background.png");
            if (!screenshotFile.getParentFile().exists() && !screenshotFile.getParentFile().mkdirs()) {
                LOGGER.error("Failed to create screenshot directory: {}", screenshotFile.getParentFile());
                return;
            }

            ImageIO.write(image, "png", screenshotFile);
            LOGGER.info("Screenshot saved to: {}", screenshotFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to capture screenshot.", e);
        }
    }

    private void preloadBackgroundTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        File screenshotFile = new File(client.runDirectory, "screenshots/background.png");

        if (screenshotFile.exists()) {
            try {
                BufferedImage image = ImageIO.read(screenshotFile);
                NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);

                // Populate the NativeImage with converted ABGR pixel data
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;

                        // Convert and set ABGR using NativeImage utility
                        nativeImage.setColor(x, y, ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF));
                    }
                }

                NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                client.getTextureManager().registerTexture(BACKGROUND_TEXTURE, texture);
                LOGGER.info("Background texture preloaded successfully.");
            } catch (IOException e) {
                LOGGER.error("Failed to preload background.png", e);
            }
        } else {
            LOGGER.warn("Screenshot file not found. Texture won't be loaded.");
        }
    }
}