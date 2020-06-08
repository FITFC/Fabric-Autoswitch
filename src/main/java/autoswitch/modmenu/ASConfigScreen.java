package autoswitch.modmenu;

import autoswitch.AutoSwitch;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ASConfigScreen extends Screen {

    ButtonWidget openFolder;
    ButtonWidget openConfigFiles;
    ButtonWidget question;

    protected ASConfigScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        try {
            this.openFolder = this.addButton(new ButtonWidget(this.width / 2 - 200, 2 * this.height / 3, 150, 20, new TranslatableText("button.autoswitch.openfolder"), (button -> Util.getOperatingSystem().open(FabricLoader.getInstance().getConfigDirectory()))) {
                public Text getMessage() {
                    return new TranslatableText("button.autoswitch.openfolder");
                }

                protected MutableText getNarrationMessage() {
                    return new TranslatableText("button.autoswitch.openfolder");
                }
            });

            this.openConfigFiles = this.addButton(new ButtonWidget(this.width / 2 + 50, 2 * this.height / 3, 150, 20, new TranslatableText("button.autoswitch.openconfigs"), (button -> {
                Util.getOperatingSystem().open(new File(FabricLoader.getInstance().getConfigDirectory().toString(), "autoswitch.cfg"));
                Util.getOperatingSystem().open(new File(FabricLoader.getInstance().getConfigDirectory().toString(), "autoswitchMaterials.cfg"));
            })) {
                public Text getMessage() {
                    return new TranslatableText("button.autoswitch.openconfigs");
                }

                protected MutableText getNarrationMessage() {
                    return new TranslatableText("button.autoswitch.openconfigs");
                }
            });

            this.question = this.addButton(new ButtonWidget(this.width / 2  - 150, this.height - 30, 300, 20, new TranslatableText("button.autoswitch.openwiki"), (buttonWidget) -> {
                try {
                    Util.getOperatingSystem().open(new URL("https://github.com/dexman545/Fabric-Autoswitch/wiki/Why-Does-AutoSwitch-Not-Include-an-In-Game-GUI-For-Editing-Configs%3F"));
                } catch (MalformedURLException e) {
                    AutoSwitch.logger.error("Failed to open Wiki Page");
                    AutoSwitch.logger.error(e);
                }
            }) {

                public Text getMessage() {
                    return new TranslatableText("button.autoswitch.openwiki");
                }

                protected MutableText getNarrationMessage() {
                    return new TranslatableText("button.autoswitch.openwiki");
                }
            });


        } catch (Exception e) {
            AutoSwitch.logger.error("Failed to initialize screen!");
            AutoSwitch.logger.error(e);
        }

    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        try {
            this.renderBackground(matrices);
            this.fillGradient(matrices, 0, 0, this.width, this.height, Color.GRAY.getRGB(), Color.GRAY.getRGB());
            int centerX = this.width / 2;
            int centerY = this.width / 2;

            for (int i = 1; i < 6; i++) {
                this.drawCenteredText(matrices, this.textRenderer, new TranslatableText("msg.autoswitch.config."+i), centerX, centerY / 3  + ((i) * 10), Color.RED.getRGB());
            }

            super.render(matrices, mouseX, mouseY, delta);

        } catch (Exception e) {
            AutoSwitch.logger.error("Failed to render screen!");
            AutoSwitch.logger.error(e);
        }

    }
}