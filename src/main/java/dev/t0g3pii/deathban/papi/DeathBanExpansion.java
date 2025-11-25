package dev.t0g3pii.deathban.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.t0g3pii.deathban.NilsAngDeathBanPlugin;
import dev.t0g3pii.deathban.store.BanRecord;
import dev.t0g3pii.deathban.store.ModSpectateRecord;

import java.util.Map;
import java.util.UUID;

public class DeathBanExpansion extends PlaceholderExpansion {
	private final NilsAngDeathBanPlugin plugin;

	public DeathBanExpansion(NilsAngDeathBanPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public @NotNull String getIdentifier() { return "deathban"; }

	@Override
	public @NotNull String getAuthor() { return "NilsANG"; }

	@Override
	public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

	@Override
	public boolean persist() { return true; }

	@Override
	public boolean canRegister() { return true; }

	@Override
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
		String key = params.toLowerCase();
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
