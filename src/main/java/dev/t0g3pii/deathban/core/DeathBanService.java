package dev.t0g3pii.deathban.core;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import dev.t0g3pii.deathban.store.BanRecord;
import dev.t0g3pii.deathban.store.BanStore;
import dev.t0g3pii.deathban.util.DurationParser;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class DeathBanService {
	private final BanStore store;
	private final FileConfiguration config;
	private final MiniMessage mm = MiniMessage.miniMessage();

	private Duration banDuration;

	public DeathBanService(BanStore store, FileConfiguration config) {
		this.store = store;
		this.config = config;
		this.banDuration = DurationParser.parseOrThrow(config.getString("banDuration", "24h"));
	}

	public void reload() {
		this.banDuration = DurationParser.parseOrThrow(config.getString("banDuration", "24h"));
		store.cleanupExpired();
		store.save();
	}

	public long banPlayerNow(UUID playerId, String playerName, World world, int x, int y, int z) {
		long now = Instant.now().getEpochSecond();
		long until = now + banDuration.toSeconds();
		store.putBanRecord(playerId, new BanRecord(until, playerName, worldKey(world), x, y, z, now));
		store.save();
		return until;
	}

	public void unban(UUID playerId) {
		store.unban(playerId);
		store.save();
	}

	public boolean isBanned(UUID playerId) {
		return store.isBanned(playerId);
	}

	public OptionalLong getRemainingSeconds(UUID playerId) {
		return store.getRemainingSeconds(playerId);
	}

	public Optional<BanRecord> getRecord(UUID playerId) {
		return store.getRecord(playerId);
	}

	public String worldKey(World world) {
		World.Environment env = world.getEnvironment();
		switch (env) {
			case NETHER: return "nether";
			case THE_END: return "the_end";
			case NORMAL:
			default: return "normal";
		}
	}

	public String formatDimensionPhrase(World world) {
		String base = config.getString("worldNames." + worldKey(world), worldKey(world));
		String lower = base.toLowerCase(Locale.ROOT);
		if ("oberwelt".equals(lower) || "overworld".equals(lower) || "normal".equals(lower)) {
			return "in der " + base;
		}
		return "im " + base;
	}

	public String getPrefix() {
		return config.getString("prefix", "");
	}

	public MiniMessage mm() {
		return mm;
	}
}
