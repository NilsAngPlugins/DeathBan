package dev.t0g3pii.deathban.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import dev.t0g3pii.deathban.core.DeathBanService;
import dev.t0g3pii.deathban.discord.DiscordNotifier;
import dev.t0g3pii.deathban.store.ModSpectateRecord;
import dev.t0g3pii.deathban.store.ModSpectateStore;
import dev.t0g3pii.deathban.util.DiscordLinkUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeathListener implements Listener {
	private final DeathBanService service;
	private final FileConfiguration config;
	private final FileConfiguration discordCfg;
	private final DiscordNotifier discord;
	private final ModSpectateStore modStore;

	public DeathListener(DeathBanService service, FileConfiguration config, FileConfiguration discordCfg, DiscordNotifier discord, ModSpectateStore modStore) {
		this.service = service;
		this.config = config;
		this.discordCfg = discordCfg;
		this.discord = discord;
		this.modStore = modStore;
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player p = event.getEntity();
		if (config.getBoolean("respectExemptPermission", true) && p.hasPermission("deathban.exempt")) {
			return;
		}

		boolean moderatorMode = config.getBoolean("moderator.enabled", true) && (p.hasPermission("deathban.moderator") || p.hasPermission("deathban.streamer"));
		boolean streamerMode = moderatorMode && p.hasPermission("deathban.streamer");
		Location loc = p.getLocation();
		long now = Instant.now().getEpochSecond();

		String datePattern = config.getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(datePattern).withZone(ZoneId.systemDefault());
		String dimensionPhrase = service.formatDimensionPhrase(p.getWorld());
		String funnyReason = buildFunnyReason(event);

		if (moderatorMode) {
			long until = now + dev.t0g3pii.deathban.util.DurationParser.parseOrThrow(config.getString("banDuration", "24h")).toSeconds();
			ModSpectateRecord rec = new ModSpectateRecord(until, p.getName(), service.worldKey(p.getWorld()), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), now);
			modStore.putRecord(p.getUniqueId(), rec);
			modStore.save();
			String untilFormatted = dtf.format(Instant.ofEpochSecond(until));
			MiniMessage mm = service.mm();
			Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("NilsANG-DeathBan"), () -> {
				p.setGameMode(GameMode.SPECTATOR);
				String msg = config.getString("moderator.enterMessage", "<yellow>Du bist im Zuschauermodus bis %until%.")
						.replace("%until%", untilFormatted);
				p.sendMessage(mm.deserialize(service.getPrefix() + " " + msg));
				if (streamerMode) {
					String kickMsg = config.getString("moderator.streamerKickMessage", "<gray>Stream-Übergang. Du wurdest kurz getrennt.</gray>")
							.replace("%until%", untilFormatted);
					p.kick(mm.deserialize(service.getPrefix() + " " + kickMsg));
				}
			});
			if (discord.isEnabled()) {
				String playerName = p.getName();
				String content = null;
				Map<String, Object> embed = null;
				String mentions = discordCfg.getString("mentions", "");
				String avatar = discordCfg.getString("avatarUrl", "").replace("%spielername%", playerName);
				String username = discordCfg.getString("username", "");
				if (discordCfg.getBoolean("useEmbed", true)) {
					String title = discordCfg.getString("embed.title", "DeathBan");
					String desc = discordCfg.getString("embed.description", "")
							.replace("%spielername%", playerName)
							.replace("%timestamp%", String.valueOf(now));
					int color = Integer.decode(discordCfg.getString("embed.color", "0xFF0000"));
					String footer = discordCfg.getString("embed.footer", "");
					String thumb = discordCfg.getString("embed.thumbnail", "").replace("%spielername%", playerName);
					List<Map<String, Object>> fields = new ArrayList<>();
					fields.add(field("Coords:", "X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ(), true));
					fields.add(field("Welt:", dimensionPhrase, true));
					fields.add(field("Todesgrund", funnyReason, false));
					embed = discord.buildEmbed(title, desc, color, footer, thumb, fields);
					if (mentions != null && !mentions.isBlank()) content = mentions;
				} else {
					content = discordCfg.getString("message", "")
							.replace("%spielername%", playerName)
							.replace("%X%", String.valueOf(loc.getBlockX()))
							.replace("%Y%", String.valueOf(loc.getBlockY()))
							.replace("%Z%", String.valueOf(loc.getBlockZ()))
							.replace("%dimension_phrase%", dimensionPhrase)
							.replace("%timestamp%", String.valueOf(now));
					if (mentions != null && !mentions.isBlank()) content = mentions + "\n" + content;
				}
				discord.send(content, username, avatar, embed);
			}
			return;
		}

		long until = service.banPlayerNow(p.getUniqueId(), p.getName(), p.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		long remaining = until - now;

		String timeFormatted = dtf.format(Instant.ofEpochSecond(now));
		String untilFormatted = dtf.format(Instant.ofEpochSecond(until));

		String banMessage = config.getString("banMessage", "<red>Gebannt</red>")
				.replace("%spielername%", p.getName())
				.replace("%X%", String.valueOf(loc.getBlockX()))
				.replace("%Y%", String.valueOf(loc.getBlockY()))
				.replace("%Z%", String.valueOf(loc.getBlockZ()))
				.replace("%dimension_phrase%", service.formatDimensionPhrase(p.getWorld()))
				.replace("%time%", timeFormatted)
				.replace("%until%", untilFormatted)
				.replace("%duration%", humanDuration(remaining));
		MiniMessage mm = service.mm();

		Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("NilsANG-DeathBan"), () -> {
			p.kick(mm.deserialize(service.getPrefix() + " " + banMessage));
		});

		if (discord.isEnabled()) {
			String playerName = p.getName();
			String content = null;
			Map<String, Object> embed = null;
			String mentions = discordCfg.getString("mentions", "");
			String avatar = discordCfg.getString("avatarUrl", "").replace("%spielername%", playerName);
			String username = discordCfg.getString("username", "");
			if (discordCfg.getBoolean("useEmbed", true)) {
				String title = discordCfg.getString("embed.title", "DeathBan");
				String desc = discordCfg.getString("embed.description", "")
						.replace("%spielername%", playerName)
						.replace("%timestamp%", String.valueOf(now));
				int color = Integer.decode(discordCfg.getString("embed.color", "0xFF0000"));
				String footer = discordCfg.getString("embed.footer", "");
				String thumb = discordCfg.getString("embed.thumbnail", "").replace("%spielername%", playerName);
				List<Map<String, Object>> fields = new ArrayList<>();
				fields.add(field("Coords:", "X: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ(), true));
				fields.add(field("Welt:", dimensionPhrase, true));
				fields.add(field("Todesgrund", funnyReason, false));
				embed = discord.buildEmbed(title, desc, color, footer, thumb, fields);
				if (mentions != null && !mentions.isBlank()) {
					content = mentions;
				}
			} else {
				content = discordCfg.getString("message", "")
						.replace("%spielername%", playerName)
						.replace("%X%", String.valueOf(loc.getBlockX()))
						.replace("%Y%", String.valueOf(loc.getBlockY()))
						.replace("%Z%", String.valueOf(loc.getBlockZ()))
						.replace("%dimension_phrase%", dimensionPhrase)
						.replace("%timestamp%", String.valueOf(now));
				if (mentions != null && !mentions.isBlank()) {
					content = mentions + "\n" + content;
				}
			}
			discord.send(content, username, avatar, embed);
		}
	}

	private Map<String, Object> field(String name, String value, boolean inline) {
		Map<String, Object> f = new HashMap<>();
		f.put("name", name);
		f.put("value", value);
		f.put("inline", inline);
		return f;
	}

	private String buildFunnyReason(PlayerDeathEvent event) {
		Player victim = event.getEntity();
		EntityDamageEvent last = victim.getLastDamageCause();
		String mentionVictim = DiscordLinkUtil.getDiscordMention(victim.getUniqueId());
		String who = mentionVictim != null ? mentionVictim : victim.getName();
		Player killer = victim.getKiller();
		String killerMention = killer != null ? DiscordLinkUtil.getDiscordMention(killer.getUniqueId()) : null;
		String killerName = killer != null ? (killerMention != null ? killerMention : killer.getName()) : null;
		if (killer != null) {
			return who + " wurde von " + killerName + " vernascht.";
		}
		if (last == null) return who + " hat sehr unglücklich geatmet.";
		switch (last.getCause()) {
			case FALL: return who + " wollte fliegen lernen.";
			case DROWNING: return who + " hat zu lange die Luft angehalten.";
			case LAVA: return who + " dachte, Lava sei ein Thermalbad.";
			case FIRE: case FIRE_TICK: return who + " hat mit dem Feuer gespielt.";
			case VOID: return who + " ist vom Rand der Welt gefallen.";
			case STARVATION: return who + " hat die Lunchbox vergessen.";
			case POISON: return who + " hat fragwürdige Pilze probiert.";
			case CONTACT: return who + " hat die Umarmung der Kakteen unterschätzt.";
			case SUFFOCATION: return who + " wurde von Blöcken erdrückt.";
			case BLOCK_EXPLOSION: case ENTITY_EXPLOSION: return who + " hat zu nah an der Party gefeiert.";
			case MAGIC: return who + " hat magische Nebenwirkungen erfahren.";
			case WITHER: return who + " hat den Wither unterschätzt.";
			case CRAMMING: return who + " stand etwas zu eng.";
			default: return who + " ist einen mysteriösen Tod gestorben.";
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
