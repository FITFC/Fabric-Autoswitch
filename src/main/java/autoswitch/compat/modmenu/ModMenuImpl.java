package autoswitch.compat.modmenu;

import autoswitch.AutoSwitch;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        //TODO proper config screen - rendering is hard
        try {
            return new ASConfigScreenFactory();
        } catch (Throwable e) {
            AutoSwitch.logger.error("Failed to make ModMenu screen for AutoSwitch");
            AutoSwitch.logger.error(e);
        }

        return null;
    }

}
