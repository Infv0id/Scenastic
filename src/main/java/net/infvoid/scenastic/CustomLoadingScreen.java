package net.infvoid.scenastic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

public class CustomLoadingScreen extends Screen {
    private final MinecraftClient client;

    public CustomLoadingScreen(MinecraftClient client) {
        super(Text.translatable("screen.custom_loading")); // Set a custom title for the loading screen
        this.client = client;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the screenshot as the background texture
        Identifier backgroundTexture = Identifier.of("minecraft", "screenshots/background.png");

        // Bind the texture (ensure it's loaded into the texture manager)
        RenderSystem.setShaderTexture(0, backgroundTexture);

        // Draw the texture on the screen (fill the whole screen)
        context.drawTexture(backgroundTexture, 0, 0, 0, 0, this.width, this.height);

        // Call the super method to render the default loading screen elements (like progress bar)
        super.render(context, mouseX, mouseY, delta);
    }
}
