package de.t0g3pii.deathban.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.t0g3pii.deathban.NilsAngDeathBanPlugin;
import de.t0g3pii.deathban.store.BanRecord;
import de.t0g3pii.deathban.store.ModSpectateRecord;

import java.util.Map;
import java.util.UUID;

public class DeathBanExpansion extends PlaceholderExpansion {
	private final NilsAngDeathBanPlugin plugin;

	public DeathBanExpansion(NilsAngDeathBanPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public @NotNull String getIdentifier() { return "deathban"; }

	@SuppressWarnings("deprecation")
	@Override
	public @NotNull String getAuthor() { return plugin.getDescription().getAuthors().get(0); }

	@SuppressWarnings("deprecation")
	@Override
	public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

	@Override
	public boolean persist() { return true; }

	@Override
	public boolean canRegister() { return true; }

	@Override
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
		String key = params.toLowerCase();

		// dynamic checks first
		if (key.equals("isdead")) {
			if (player == null) return "false";
			UUID id = player.getUniqueId();
			boolean dead = plugin.getStore().isBanned(id) || plugin.getModSpectateStore().isActive(id);
			return String.valueOf(dead);
		}
		if (key.startsWith("isdead_")) {
			String name = params.substring("isdead_".length());
			if (name.isEmpty()) return "false";
			OfflinePlayer target = Bukkit.getPlayerExact(name);
			if (target == null) {
				// fall back to offline lookup
				target = Bukkit.getOfflinePlayer(name);
			}
			if (target == null || (target.getUniqueId() == null)) return "false";
			UUID id = target.getUniqueId();
			boolean dead = plugin.getStore().isBanned(id) || plugin.getModSpectateStore().isActive(id);
			return String.valueOf(dead);
		}

		switch (key) {
			case "players":
				Map<UUID, BanRecord> all = plugin.getStore().listAll();
				return String.valueOf(all.size());
			case "mods":
				Map<UUID, ModSpectateRecord> mods = plugin.getModSpectateStore().all();
				return String.valueOf(mods.size());
			case "total":
				int p = plugin.getStore().listAll().size();
				int m = plugin.getModSpectateStore().all().size();
				return String.valueOf(p + m);
			default:
				return null;
		}
	}
}
