package de.t0g3pii.deathban.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import de.t0g3pii.deathban.core.DeathBanService;
import de.t0g3pii.deathban.store.ModSpectateRecord;
import de.t0g3pii.deathban.store.ModSpectateStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.OptionalLong;

public class JoinListener implements Listener {
	private final DeathBanService service;
	private final org.bukkit.configuration.file.FileConfiguration config;
	private final ModSpectateStore modStore;
	private final Plugin plugin;

	public JoinListener(DeathBanService service, org.bukkit.configuration.file.FileConfiguration config, ModSpectateStore modStore, Plugin plugin) {
		this.service = service;
		this.config = config;
		this.modStore = modStore;
		this.plugin = plugin;
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		Player p = event.getPlayer();
		if (!service.isBanned(p.getUniqueId())) return;
		OptionalLong remaining = service.getRemainingSeconds(p.getUniqueId());
		if (remaining.isEmpty()) return;
		long untilEpoch = Instant.now().getEpochSecond() + remaining.getAsLong();

		String datePattern = config.getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(datePattern).withZone(ZoneId.systemDefault());
		String untilFormatted = dtf.format(Instant.ofEpochSecond(untilEpoch));

		String msg = config.getString("joinDenyMessage", "<red>Gesperrt</red>")
				.replace("%remaining%", humanDuration(remaining.getAsLong()))
				.replace("%until%", untilFormatted);
		MiniMessage mm = service.mm();
		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, mm.deserialize(service.getPrefix() + " " + msg));
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		Optional<ModSpectateRecord> rec = modStore.getRecord(p.getUniqueId());
		if (rec.isEmpty()) return;
		long until = rec.get().untilEpochSeconds;
		long now = Instant.now().getEpochSecond();
		String datePattern = config.getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(datePattern).withZone(ZoneId.systemDefault());
		MiniMessage mm = service.mm();
		if (until > now) {
			String untilFormatted = dtf.format(Instant.ofEpochSecond(until));
			Bukkit.getScheduler().runTask(plugin, () -> {
				p.setGameMode(GameMode.SPECTATOR);
				String msg = config.getString("moderator.enterMessage", "<yellow>Du bist im Zuschauermodus bis %until%.")
						.replace("%until%", untilFormatted);
				p.sendMessage(mm.deserialize(service.getPrefix() + " " + msg));
			});
		} else {
			Bukkit.getScheduler().runTask(plugin, () -> {
				Location base = p.getBedSpawnLocation();
				if (base == null) {
					World defaultWorld = Bukkit.getWorlds().isEmpty() ? p.getWorld() : Bukkit.getWorlds().get(0);
					base = defaultWorld.getSpawnLocation();
				}
				Location safe = base.clone();
				int highestY = safe.getWorld().getHighestBlockAt(safe).getY();
				safe.setY(highestY + 1.0);
				safe.setX(safe.getBlockX() + 0.5);
				safe.setZ(safe.getBlockZ() + 0.5);
				p.teleport(safe);
				p.setGameMode(GameMode.SURVIVAL);
				String msg = config.getString("moderator.restoreMessage", "<green>Du bist wieder im Survival.");
				p.sendMessage(mm.deserialize(service.getPrefix() + " " + msg));
				modStore.remove(p.getUniqueId());
				modStore.save();
			});
		}
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
