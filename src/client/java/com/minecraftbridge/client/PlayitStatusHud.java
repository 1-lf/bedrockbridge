package com.minecraftbridge.client;

import com.minecraftbridge.state.BedrockBridgeState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// Persistent badge in the top-left corner showing the tunnel status. Hidden when
// Share with Bedrock is off or no world is open so it doesn't clutter the screen
// when irrelevant.
public class PlayitStatusHud implements HudElement {

	private static final int PADDING_X = 4;
	private static final int PADDING_Y = 3;
	private static final int MARGIN = 6;
	private static final int BG_COLOR = 0xC0_00_00_00; // semi-transparent black
	private static final int TEXT_WHITE = 0xFF_FF_FF_FF;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (!PlayitStatus.hudVisible()) return;
		if (!BedrockBridgeState.shareWithBedrock) return;
		PlayitStatus status = PlayitStatus.current();
		if (status == PlayitStatus.IDLE) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null) return; // hide while a menu is open
		if (mc.options.hideGui()) return;

		Component text = buildBadge(status, PlayitStatus.detail());
		Font font = mc.font;
		int textW = font.width(text);
		int textH = 9;

		int x = MARGIN;
		int y = MARGIN;
		g.fill(x - PADDING_X, y - PADDING_Y, x + textW + PADDING_X, y + textH + PADDING_Y, BG_COLOR);
		g.text(font, text, x, y, TEXT_WHITE);
	}

	private Component buildBadge(PlayitStatus status, String detail) {
		ChatFormatting color = switch (status) {
			case BOOTSTRAPPING -> ChatFormatting.YELLOW;
			case CLAIMING -> ChatFormatting.GOLD;
			case ONLINE -> ChatFormatting.GREEN;
			case ERROR -> ChatFormatting.RED;
			default -> ChatFormatting.GRAY;
		};
		String label = switch (status) {
			case BOOTSTRAPPING -> Lang.get("bedrockbridge.hud.preparing");
			case CLAIMING -> Lang.get("bedrockbridge.hud.waiting_claim");
			case ONLINE -> Lang.get("bedrockbridge.hud.online");
			case ERROR -> Lang.get("bedrockbridge.hud.error");
			default -> "";
		};

		MutableComponent c = Component.literal("BedrockBridge ").withStyle(ChatFormatting.AQUA);
		c.append(Component.literal(label).withStyle(color, ChatFormatting.BOLD));
		if (!detail.isEmpty()) {
			c.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
			c.append(Component.literal(detail).withStyle(ChatFormatting.WHITE));
		}
		return c;
	}
}
