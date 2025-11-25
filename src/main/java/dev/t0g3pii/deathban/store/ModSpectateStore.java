package dev.t0g3pii.deathban.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModSpectateStore {
	private final File file;
	private final Map<UUID, ModSpectateRecord> map = new ConcurrentHashMap<>();

	public ModSpectateStore(File dataFolder) {
		this.file = new File(dataFolder, "mod_spectate.yml");
	}

	public void load() {
		map.clear();
		if (!file.exists()) return;
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
		for (String key : cfg.getKeys(false)) {
			try {
				UUID id = UUID.fromString(key);
				Object node = cfg.get(key);
				if (node instanceof Number) {
					long until = ((Number) node).longValue();
					map.put(id, new ModSpectateRecord(until, null, null, 0, 0, 0, 0L));
				} else if (node instanceof ConfigurationSection) {
					ConfigurationSection sec = cfg.getConfigurationSection(key);
					long until = sec.getLong("until", 0L);
					String name = sec.getString("name", null);
					String world = sec.getString("world", null);
					int x = sec.getInt("x", 0);
					int y = sec.getInt("y", 0);
					int z = sec.getInt("z", 0);
					long death = sec.getLong("death", 0L);
					if (until > 0L) {
						map.put(id, new ModSpectateRecord(until, name, world, x, y, z, death));
					}
				}
			} catch (IllegalArgumentException ignored) { }
		}
		cleanupExpired();
	}

	public void save() {
		FileConfiguration cfg = new YamlConfiguration();
		for (Map.Entry<UUID, ModSpectateRecord> e : map.entrySet()) {
			String base = e.getKey().toString();
			ModSpectateRecord r = e.getValue();
			cfg.set(base + ".until", r.untilEpochSeconds);
			if (r.playerName != null) cfg.set(base + ".name", r.playerName);
			if (r.worldKey != null) cfg.set(base + ".world", r.worldKey);
			cfg.set(base + ".x", r.x);
			cfg.set(base + ".y", r.y);
			cfg.set(base + ".z", r.z);
			cfg.set(base + ".death", r.deathEpochSeconds);
		}
		try {
			cfg.save(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putRecord(UUID id, ModSpectateRecord record) { map.put(id, record); }
	public void remove(UUID id) { map.remove(id); }
	public Optional<ModSpectateRecord> getRecord(UUID id) { return Optional.ofNullable(map.get(id)); }
	public OptionalLong getUntil(UUID id) { ModSpectateRecord r = map.get(id); return r==null?OptionalLong.empty():OptionalLong.of(r.untilEpochSeconds); }
	public boolean isActive(UUID id) {
		OptionalLong o = getUntil(id);
		if (o.isEmpty()) return false;
		return o.getAsLong() > Instant.now().getEpochSecond();
	}
	public Map<UUID, ModSpectateRecord> all() { return new LinkedHashMap<>(map); }
	public void cleanupExpired() {
		long now = Instant.now().getEpochSecond();
		map.entrySet().removeIf(e -> e.getValue().untilEpochSeconds <= now);
	}
}
