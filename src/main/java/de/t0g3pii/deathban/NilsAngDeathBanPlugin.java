package de.t0g3pii.deathban;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import de.t0g3pii.deathban.command.DeathBanCommand;
import de.t0g3pii.deathban.core.DeathBanService;
import de.t0g3pii.deathban.discord.DiscordNotifier;
import de.t0g3pii.deathban.listener.DeathListener;
import de.t0g3pii.deathban.listener.GameModeGuardListener;
import de.t0g3pii.deathban.listener.JoinListener;
import de.t0g3pii.deathban.papi.DeathBanExpansion;
import de.t0g3pii.deathban.store.BanStore;
import de.t0g3pii.deathban.store.ModSpectateRecord;
import de.t0g3pii.deathban.store.ModSpectateStore;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class NilsAngDeathBanPlugin extends JavaPlugin {
	private BanStore store;
	private DeathBanService service;
	private FileConfiguration discordCfg;
	private DiscordNotifier discord;
	private ModSpectateStore modSpectateStore;
	private int schedulerTaskId = -1;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		File discordFile = new File(getDataFolder(), "discord.yml");
		if (!discordFile.exists()) {
			saveResource("discord.yml", false);
		}
		this.discordCfg = YamlConfiguration.loadConfiguration(discordFile);

		this.store = new BanStore(getDataFolder());
		this.store.load();

		this.modSpectateStore = new ModSpectateStore(getDataFolder());
		this.modSpectateStore.load();

		this.service = new DeathBanService(store, getConfig());
		this.discord = new DiscordNotifier(discordCfg);

		getServer().getPluginManager().registerEvents(new DeathListener(service, getConfig(), discordCfg, discord, modSpectateStore, this), this);
		getServer().getPluginManager().registerEvents(new JoinListener(service, getConfig(), modSpectateStore, this), this);
		getServer().getPluginManager().registerEvents(new GameModeGuardListener(modSpectateStore, service.getPrefix()), this);
		DeathBanCommand cmd = new DeathBanCommand(this);
		getCommand("deathban").setExecutor(cmd);
		getCommand("deathban").setTabCompleter(cmd);

		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new DeathBanExpansion(this).register();
		}

		startModeratorRestoreTask();

		getLogger().info("DeathBan aktiviert.");
	}

	@Override
	public void onDisable() {
		if (store != null) {
			store.cleanupExpired();
			store.save();
		}
		if (modSpectateStore != null) {
			modSpectateStore.cleanupExpired();
			modSpectateStore.save();
		}
		if (schedulerTaskId != -1) {
			Bukkit.getScheduler().cancelTask(schedulerTaskId);
		}
	}

	public void reloadAll() {
		reloadConfig();
		this.discordCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "discord.yml"));
		this.discord.setConfig(discordCfg);
		this.service.reload();
	}

	private void startModeratorRestoreTask() {
		if (!getConfig().getBoolean("moderator.enabled", true)) return;
		schedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
			long now = Instant.now().getEpochSecond();
			Map<UUID, ModSpectateRecord> all = modSpectateStore.all();
			if (all.isEmpty()) return;
			for (Map.Entry<UUID, ModSpectateRecord> e : all.entrySet()) {
				UUID id = e.getKey();
				Player p = Bukkit.getPlayer(id);
				if (p == null) continue; // offline -> bei Join wird wiederhergestellt
				long until = e.getValue().untilEpochSeconds;
				if (until <= now) {
					Location safe = computeSafeRespawn(p);
					p.teleport(safe);
					p.setGameMode(GameMode.SURVIVAL);
					p.sendMessage(service.mm().deserialize(service.getPrefix() + " " + getConfig().getString("moderator.restoreMessage", "<green>Du bist wieder im Survival.")));
					modSpectateStore.remove(id);
					modSpectateStore.save();
				} else {
					if (p.getGameMode() != GameMode.SPECTATOR) {
						p.setGameMode(GameMode.SPECTATOR);
					}
				}
			}
		}, 40L, 100L);
	}

	private Location computeSafeRespawn(Player p) {
		@SuppressWarnings("deprecation")
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
		return safe;
	}

	public BanStore getStore() { return store; }
	public DeathBanService getService() { return service; }
	public ModSpectateStore getModSpectateStore() { return modSpectateStore; }
}
