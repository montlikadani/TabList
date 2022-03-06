package hu.montlikadani.tablist.commands;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

abstract class ICommand {

	protected final boolean hasPerm(CommandSource src, String perm) {
		return !(src instanceof Player) || src.hasPermission(perm);
	}

	protected final void sendMsg(CommandSource src, String msg) {
		if (msg != null && !msg.isEmpty()) {
			sendMsg(src, TextSerializers.FORMATTING_CODE.deserialize(msg));
		}
	}

	protected final void sendMsg(CommandSource src, Text text) {
		src.sendMessage(text);
	}
}
