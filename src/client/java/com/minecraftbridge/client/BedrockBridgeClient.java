package com.minecraftbridge.client;

import com.minecraftbridge.BedrockBridge;
import com.minecraftbridge.client.playit.PlayitManager;
import com.minecraftbridge.state.BedrockBridgeState;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
// Remove: import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BedrockBridgeClient implements ClientModInitializer {

	private boolean lanWasOpen = false;
	private KeyMapping toggleHudKey;

	@Override
	public void onInitializeClient() {
		BedrockBridge.LOGGER.info("BedrockBridgeClient initialized on the client side.");
		ScreenEvents.AFTER_INIT.register(this::onScreenAfterInit);
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(BedrockBridge.MOD_ID, "playit_status_hud"),
				new PlayitStatusHud());
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(BedrockBridge.MOD_ID, "main"));
		toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.bedrockbridge.toggle_hud",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				category));
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onScreenAfterInit(Minecraft client, net.minecraft.client.gui.screens.Screen screen, int width, int height) {
    // Check for ShareToLanScreen by class name instead of direct import
    if (!screen.getClass().getSimpleName().equals("ShareToLanScreen")) {
        return;
    }

		Checkbox checkbox = Checkbox.builder(
				Component.literal(Lang.get("bedrockbridge.checkbox.share_with_bedrock")),
				Screens.getFont(screen)
			)
			.pos(10, height - 55)
			.selected(BedrockBridgeState.shareWithBedrock)
			.onValueChange((cb, value) -> {
				BedrockBridgeState.shareWithBedrock = value;
				BedrockBridge.LOGGER.info("'Share with Bedrock' checkbox toggled to: {}", value);
			})
			.build();

		Screens.getWidgets(screen).add(checkbox);
		BedrockBridge.LOGGER.info("Checkbox injected into ShareToLanScreen (initial state: {}).", BedrockBridgeState.shareWithBedrock);
	}

	private void onClientTick(Minecraft client) {
		while (toggleHudKey != null && toggleHudKey.consumeClick()) {
			PlayitStatus.toggleHud();
		}

		boolean lanIsOpen = client.hasSingleplayerServer()
				&& client.getSingleplayerServer() != null
				&& client.getSingleplayerServer().isPublished();

		if (lanIsOpen && !lanWasOpen) {
			onLanOpened(client);
		}
		if (!lanIsOpen && lanWasOpen) {
			onLanClosed(client);
		}
		lanWasOpen = lanIsOpen;
	}

	private void onLanOpened(Minecraft client) {
		int port = client.getSingleplayerServer().getPort();
		boolean share = BedrockBridgeState.shareWithBedrock;
		BedrockBridge.LOGGER.info("World opened to LAN. Java port: {} (shareWithBedrock={})", port, share);

		if (share) {
			MutableComponent msg = Chat.header().append(Chat.ok(Lang.get("bedrockbridge.chat.lan_opened")));
			Chat.send(msg);
			Chat.send(Chat.label(Lang.get("bedrockbridge.chat.label.java_lan")).append(Chat.value(port)));
			Chat.send(Chat.label(Lang.get("bedrockbridge.chat.label.bedrock_lan")).append(Chat.value(19132))
					.append(Component.literal(" ")).append(Chat.muted(Lang.get("bedrockbridge.chat.value.floodgate_active"))));
			Chat.send(Chat.label(Lang.get("bedrockbridge.chat.label.internet")).append(Chat.muted(Lang.get("bedrockbridge.chat.value.preparing_tunnel"))));
			PlayitManager.get().start();
		} else {
			MutableComponent msg = Chat.header()
					.append(Chat.ok(Lang.get("bedrockbridge.chat.lan_opened_java_only_prefix")))
					.append(Chat.value(port))
					.append(Component.literal(" "))
					.append(Chat.muted(Lang.get("bedrockbridge.chat.lan_opened_java_only_suffix")));
			Chat.send(msg);
		}
	}

	private void onLanClosed(Minecraft client) {
		BedrockBridge.LOGGER.info("LAN world closed.");
		PlayitManager.get().stop();
		PlayitStatus.set(PlayitStatus.IDLE, "");
		MutableComponent msg = Chat.header()
				.append(Chat.muted(Lang.get("bedrockbridge.chat.lan_closed")));
		Chat.send(msg);
	}
}
