package net.infvoid.scenastic;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
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
                // Switch to the custom loading screen after world is loaded
                MinecraftClient.getInstance().setScreen(new CustomLoadingScreen(MinecraftClient.getInstance()));
                screenshotCaptured = false; // Reset flag to allow another screenshot next time
            }
        });

        LOGGER.info("Scenastic client initialized!");
    }

    private void captureScreenshot() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Screen dimensions
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        LOGGER.info("Capturing screenshot with dimensions: " + width + "x" + height);

        // Allocate pixel buffers
        int[] pixels = new int[width * height];
        IntBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).asIntBuffer();

        // Read framebuffer data
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

        // Prepare image data
        pixelBuffer.get(pixels);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Flip the image vertically
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = x + (height - y - 1) * width; // Flip vertically
                int pixel = pixels[i];

                // Extract RGBA components to ARGB format
                int r = (pixel >> 0) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;
                int a = (pixel >> 24) & 0xFF;
                image.setRGB(x, y, ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
            }
        }

        // Save the image to disk
        File screenshotFile = new File(client.runDirectory, "screenshots/background.png");
        if (!screenshotFile.getParentFile().exists()) {
            screenshotFile.getParentFile().mkdirs();
        }

        try {
            ImageIO.write(image, "png", screenshotFile);
            LOGGER.info("Screenshot saved to: " + screenshotFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to save screenshot", e);
        }
    }
}
