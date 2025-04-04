package net.infvoid.scenastic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

public class CustomLoadingScreen extends Screen {
    private final MinecraftClient client;

    // Constructor for the custom loading screen
    public CustomLoadingScreen(MinecraftClient client) {
        super(Text.of("Custom Loading Screen")); // Call the parent constructor with a title
        this.client = client;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Set the path of the background texture
        Identifier backgroundTexture = Identifier.of("minecraft", "screenshots/background.png");

        // Bind the texture into the rendering pipeline
        RenderSystem.setShaderTexture(0, backgroundTexture);

        // Draw the full-screen background image
        context.drawTexture(backgroundTexture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        // Render the default loading screen elements (progress bar, etc.)
        super.render(context, mouseX, mouseY, delta);
    }
}