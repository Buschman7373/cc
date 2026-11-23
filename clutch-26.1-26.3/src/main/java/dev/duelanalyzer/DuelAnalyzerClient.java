package dev.duelanalyzer;

import com.mojang.blaze3d.platform.InputConstants;
import dev.duelanalyzer.ui.AnalysisScreen;
import dev.duelanalyzer.ui.DashboardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class DuelAnalyzerClient implements ClientModInitializer {
    /** Opens a screen. 26.1 uses Minecraft.setScreen, 26.2 and newer use Gui.setScreen, so we look up the right one. */
    public static void openScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        try {
            try {
                Minecraft.class.getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(mc, screen);
                return;
            } catch (NoSuchMethodException ignored) {
            }
            java.lang.reflect.Field f = Minecraft.class.getDeclaredField("gui");
            f.setAccessible(true);
            Object gui = f.get(mc);
            gui.getClass().getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(gui, screen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
    }

    public static final String MOD_ID = "duelanalyzer";
    public static KeyMapping menuKey, lastKey, overlayKey, toggleKey;

    @Override
    public void onInitializeClient() {
        Store.init();
        Recorder.init();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.duelanalyzer.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, cat));
        lastKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.duelanalyzer.last", InputConstants.Type.KEYBOARD, InputConstants.KEY_L, cat));
        overlayKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.duelanalyzer.overlay", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, cat));
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.duelanalyzer.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Recorder.tick(mc);
            while (menuKey.consumeClick()) {
                if (mc.screen == null) DuelAnalyzerClient.openScreen(mc, new DashboardScreen());
            }
            while (lastKey.consumeClick()) {
                if (mc.screen != null) continue;
                if (Store.duels.isEmpty()) {
                    msg(mc, "Clutch: no duels recorded yet. Fight a player first.");
                } else {
                    DuelAnalyzerClient.openScreen(mc, new AnalysisScreen(Store.duels.get(0), null));
                }
            }
            while (toggleKey.consumeClick()) {
                Store.cfg.enabled = !Store.cfg.enabled;
                Store.saveCfg();
                msg(mc, "Clutch: " + (Store.cfg.enabled ? "ON (recording)" : "OFF (paused)"));
            }
            while (overlayKey.consumeClick()) {
                Store.cfg.overlay = !Store.cfg.overlay;
                Store.saveCfg();
                msg(mc, "Clutch: live overlay " + (Store.cfg.overlay ? "ON" : "OFF"));
            }
        });
    }
}
