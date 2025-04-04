package net.infvoid.scenastic;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Scenastic implements ModInitializer {
	public static final String MOD_ID = "scenastic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private boolean screenshotCaptured = false; // Prevent duplicate screenshots

	@Override
	public void onInitialize() {
		// Listen for the end of client tick events
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (MinecraftClient.getInstance().world == null && !screenshotCaptured) {
				screenshotCaptured = true;
				captureScreenshot();
			}
		});

		// Reset screenshotCaptured flag when the world is loaded
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (MinecraftClient.getInstance().world != null) {
				screenshotCaptured = false; // Reset flag when a new world is loaded
			}
		});

		LOGGER.info("Scenastic mod initialized!");
	}

	private void captureScreenshot() {
		MinecraftClient client = MinecraftClient.getInstance();
		RenderSystem.assertOnRenderThread(); // Ensure we're on the render thread

		// Capture screen dimensions
		int width = client.getWindow().getFramebufferWidth();
		int height = client.getWindow().getFramebufferHeight();

		// Allocate a buffer to store pixel data
		int[] pixels = new int[width * height];
		IntBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).asIntBuffer();

		// Read the pixels from the framebuffer
		GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

		// Flip pixels vertically (OpenGL starts from bottom-left, but we need top-left)
		pixelBuffer.get(pixels);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = (x + (height - y - 1) * width); // Flip vertically
				int pixel = pixels[i];

				// Extract RGBA components from OpenGL pixel format
				int r = (pixel >> 0) & 0xFF;
				int g = (pixel >> 8) & 0xFF;
				int b = (pixel >> 16) & 0xFF;
				int a = (pixel >> 24) & 0xFF;
				image.setRGB(x, y, ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
			}
		}

		// Save the screenshot to a file
		File screenshotFile = new File(client.runDirectory, "screenshots/background.png");
		if (!screenshotFile.getParentFile().exists()) {
			screenshotFile.getParentFile().mkdirs(); // Ensure directory exists
		}

		try {
			ImageIO.write(image, "png", screenshotFile);
			LOGGER.info("Screenshot saved successfully to: " + screenshotFile.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.error("Failed to save screenshot", e);
		}
	}
}
