package dev.nilsang.deathban.util;

import java.lang.reflect.Method;
import java.util.UUID;

public final class DiscordLinkUtil {
	private DiscordLinkUtil() {}

	public static String getDiscordMention(UUID playerId) {
		try {
			Class<?> clazz = Class.forName("github.scarsz.discordsrv.DiscordSRV");
			Method getPlugin = clazz.getMethod("getPlugin");
			Object plugin = getPlugin.invoke(null);
			Method getAccountLinkManager = plugin.getClass().getMethod("getAccountLinkManager");
			Object alm = getAccountLinkManager.invoke(plugin);
			Method getDiscordId = alm.getClass().getMethod("getDiscordId", UUID.class);
			Object id = getDiscordId.invoke(alm, playerId);
			if (id == null) return null;
			String idStr = String.valueOf(id);
			if (idStr.isBlank() || idStr.equals("0")) return null;
			return "<@" + idStr + ">";
		} catch (Throwable t) {
			return null;
		}
	}
}
