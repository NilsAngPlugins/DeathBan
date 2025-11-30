package de.t0g3pii.deathban.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.t0g3pii.deathban.NilsAngDeathBanPlugin;
import de.t0g3pii.deathban.store.BanRecord;
import de.t0g3pii.deathban.store.ModSpectateRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.AbstractMap;

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

		// Spieler-spezifische Checks
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
			if (target == null) target = Bukkit.getOfflinePlayer(name);
			if (target == null || target.getUniqueId() == null) return "false";
			UUID id = target.getUniqueId();
			boolean dead = plugin.getStore().isBanned(id) || plugin.getModSpectateStore().isActive(id);
			return String.valueOf(dead);
		}

		// Mod/Streamer: verbleibende Zeit / Ende
		if (key.equals("mod_remaining")) {
			if (player == null) return "";
			return modRemaining(player.getUniqueId());
		}
		if (key.equals("mod_until")) {
			if (player == null) return "";
			return modUntil(player.getUniqueId());
		}
		if (key.startsWith("mod_remaining_")) {
			String name = params.substring("mod_remaining_".length());
			OfflinePlayer target = Bukkit.getPlayerExact(name);
			if (target == null) target = Bukkit.getOfflinePlayer(name);
			if (target == null || target.getUniqueId() == null) return "";
			return modRemaining(target.getUniqueId());
		}
		if (key.startsWith("mod_until_")) {
			String name = params.substring("mod_until_".length());
			OfflinePlayer target = Bukkit.getPlayerExact(name);
			if (target == null) target = Bukkit.getOfflinePlayer(name);
			if (target == null || target.getUniqueId() == null) return "";
			return modUntil(target.getUniqueId());
		}

		// Nächster wiederzubelebender Mod/Streamer
		if (key.equals("next_mod")) {
			Optional<Map.Entry<UUID, ModSpectateRecord>> next = nextMod();
			if (next.isEmpty()) return "";
			OfflinePlayer op = Bukkit.getOfflinePlayer(next.get().getKey());
			return op.getName() != null ? op.getName() : next.get().getKey().toString();
		}
		if (key.equals("next_mod_until")) {
			Optional<Map.Entry<UUID, ModSpectateRecord>> next = nextMod();
			if (next.isEmpty()) return "";
			return formatUntil(next.get().getValue().untilEpochSeconds);
		}
		if (key.equals("next_mod_remaining")) {
			Optional<Map.Entry<UUID, ModSpectateRecord>> next = nextMod();
			if (next.isEmpty()) return "";
			long now = Instant.now().getEpochSecond();
			long until = next.get().getValue().untilEpochSeconds;
			return until > now ? humanDuration(until - now) : "0s";
		}

		// Nächste Gesamt-Freigabe (Spieler ODER Mod)
		if (key.equals("next")) {
			Optional<AbstractMap.SimpleEntry<UUID, Long>> any = nextAny();
			if (any.isEmpty()) return "";
			OfflinePlayer op = Bukkit.getOfflinePlayer(any.get().getKey());
			return op.getName() != null ? op.getName() : any.get().getKey().toString();
		}
		if (key.equals("next_until")) {
			Optional<AbstractMap.SimpleEntry<UUID, Long>> any = nextAny();
			if (any.isEmpty()) return "";
			return formatUntil(any.get().getValue());
		}
		if (key.equals("next_remaining")) {
			Optional<AbstractMap.SimpleEntry<UUID, Long>> any = nextAny();
			if (any.isEmpty()) return "";
			long now = Instant.now().getEpochSecond();
			long until = any.get().getValue();
			return until > now ? humanDuration(until - now) : "0s";
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

	private String modRemaining(UUID id) {
		long now = Instant.now().getEpochSecond();
		OptionalLong o = plugin.getModSpectateStore().getUntil(id);
		if (o.isPresent() && o.getAsLong() > now) {
			return humanDuration(o.getAsLong() - now);
		}
		return "";
	}

	private String modUntil(UUID id) {
		OptionalLong o = plugin.getModSpectateStore().getUntil(id);
		if (o.isPresent()) {
			return formatUntil(o.getAsLong());
		}
		return "";
	}

	private Optional<Map.Entry<UUID, ModSpectateRecord>> nextMod() {
		long now = Instant.now().getEpochSecond();
		return plugin.getModSpectateStore().all().entrySet().stream()
				.filter(e -> e.getValue().untilEpochSeconds > now)
				.min(Comparator.comparingLong(e -> e.getValue().untilEpochSeconds));
	}

	private Optional<AbstractMap.SimpleEntry<UUID, Long>> nextAny() {
		long now = Instant.now().getEpochSecond();
		Optional<Map.Entry<UUID, BanRecord>> nextPlayer = plugin.getStore().listAll().entrySet().stream()
				.filter(e -> e.getValue().untilEpochSeconds > now)
				.min(Comparator.comparingLong(e -> e.getValue().untilEpochSeconds));
		Optional<Map.Entry<UUID, ModSpectateRecord>> nextMod = nextMod();
		Optional<AbstractMap.SimpleEntry<UUID, Long>> a = nextPlayer.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().untilEpochSeconds));
		Optional<AbstractMap.SimpleEntry<UUID, Long>> b = nextMod.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().untilEpochSeconds));
		if (a.isEmpty()) return b;
		if (b.isEmpty()) return a;
		return a.get().getValue() <= b.get().getValue() ? a : b;
	}

	private String formatUntil(long epochSeconds) {
		String pattern = plugin.getConfig().getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
		return dtf.format(Instant.ofEpochSecond(epochSeconds));
	}

	private String humanDuration(long seconds) {
		long s = seconds % 60; seconds /= 60;
		long m = seconds % 60; seconds /= 60;
		long h = seconds % 24; seconds /= 24;
		long d = seconds;
		StringBuilder sb = new StringBuilder();
		if (d > 0) sb.append(d).append("d");
		if (h > 0) sb.append(h).append("h");
		if (m > 0) sb.append(m).append("m");
		if (sb.length() == 0) sb.append(s).append("s");
		return sb.toString();
	}
}
