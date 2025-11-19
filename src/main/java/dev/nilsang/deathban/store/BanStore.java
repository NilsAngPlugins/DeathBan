package dev.nilsang.deathban.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BanStore {
	private final File file;
	private final Map<UUID, BanRecord> playerIdToRecord = new ConcurrentHashMap<>();

	public BanStore(File dataFolder) {
		this.file = new File(dataFolder, "bans.yml");
	}

	public void load() {
		playerIdToRecord.clear();
		if (!file.exists()) {
			return;
		}
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
		for (String key : cfg.getKeys(false)) {
			try {
				UUID id = UUID.fromString(key);
				Object node = cfg.get(key);
				if (node instanceof Number) {
					long until = ((Number) node).longValue();
					if (until > 0L) {
						playerIdToRecord.put(id, new BanRecord(until, null, null, 0, 0, 0, 0L));
					}
				} else if (node instanceof ConfigurationSection) {
					ConfigurationSection sec = cfg.getConfigurationSection(key);
					if (sec == null) continue;
					long until = sec.getLong("until", 0L);
					String name = sec.getString("name", null);
					String world = sec.getString("world", null);
					int x = sec.getInt("x", 0);
					int y = sec.getInt("y", 0);
					int z = sec.getInt("z", 0);
					long death = sec.getLong("death", 0L);
					if (until > 0L) {
						playerIdToRecord.put(id, new BanRecord(until, name, world, x, y, z, death));
					}
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		cleanupExpired();
	}

	public void save() {
		FileConfiguration cfg = new YamlConfiguration();
		for (Map.Entry<UUID, BanRecord> e : playerIdToRecord.entrySet()) {
			String key = e.getKey().toString();
			BanRecord r = e.getValue();
			cfg.set(key + ".until", r.untilEpochSeconds);
			if (r.playerName != null) cfg.set(key + ".name", r.playerName);
			if (r.worldKey != null) cfg.set(key + ".world", r.worldKey);
			cfg.set(key + ".x", r.x);
			cfg.set(key + ".y", r.y);
			cfg.set(key + ".z", r.z);
			cfg.set(key + ".death", r.deathEpochSeconds);
		}
		try {
			cfg.save(file);
		} catch (IOException e) {
			throw new RuntimeException("Failed saving bans.yml", e);
		}
	}

	public void putBanRecord(UUID playerId, BanRecord record) {
		playerIdToRecord.put(playerId, record);
	}

	public void putBan(UUID playerId, long untilEpochSeconds) {
		playerIdToRecord.put(playerId, new BanRecord(untilEpochSeconds, null, null, 0, 0, 0, 0L));
	}

	public void unban(UUID playerId) {
		playerIdToRecord.remove(playerId);
	}

	public Optional<BanRecord> getRecord(UUID playerId) {
		return Optional.ofNullable(playerIdToRecord.get(playerId));
	}

	public OptionalLong getUntil(UUID playerId) {
		BanRecord r = playerIdToRecord.get(playerId);
		return r == null ? OptionalLong.empty() : OptionalLong.of(r.untilEpochSeconds);
	}

	public boolean isBanned(UUID playerId) {
		OptionalLong o = getUntil(playerId);
		if (o.isEmpty()) return false;
		long now = Instant.now().getEpochSecond();
		return o.getAsLong() > now;
	}

	public OptionalLong getRemainingSeconds(UUID playerId) {
		OptionalLong o = getUntil(playerId);
		if (o.isEmpty()) return OptionalLong.empty();
		long now = Instant.now().getEpochSecond();
		long remaining = o.getAsLong() - now;
		return remaining > 0 ? OptionalLong.of(remaining) : OptionalLong.empty();
	}

	public Map<UUID, BanRecord> listAll() {
		cleanupExpired();
		return new LinkedHashMap<>(playerIdToRecord);
	}

	public void cleanupExpired() {
		long now = Instant.now().getEpochSecond();
		List<UUID> expired = playerIdToRecord.entrySet().stream()
				.filter(e -> e.getValue().untilEpochSeconds <= now)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
		expired.forEach(playerIdToRecord::remove);
	}
}
