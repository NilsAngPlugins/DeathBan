package de.t0g3pii.deathban.listener;

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
import org.bukkit.plugin.Plugin;

import de.t0g3pii.deathban.core.DeathBanService;
import de.t0g3pii.deathban.discord.DiscordNotifier;
import de.t0g3pii.deathban.store.ModSpectateRecord;
import de.t0g3pii.deathban.store.ModSpectateStore;
import de.t0g3pii.deathban.util.DiscordLinkUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class DeathListener implements Listener {
	private final DeathBanService service;
	private final FileConfiguration config;
	private final FileConfiguration discordCfg;
	private final DiscordNotifier discord;
	private final ModSpectateStore modStore;
	private final Plugin plugin;

	public DeathListener(DeathBanService service, FileConfiguration config, FileConfiguration discordCfg, DiscordNotifier discord, ModSpectateStore modStore, Plugin plugin) {
		this.service = service;
		this.config = config;
		this.discordCfg = discordCfg;
		this.discord = discord;
		this.modStore = modStore;
		this.plugin = plugin;
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
		String timeFormatted = dtf.format(Instant.ofEpochSecond(now));

		if (moderatorMode) {
			long until = now + de.t0g3pii.deathban.util.DurationParser.parseOrThrow(config.getString("banDuration", "24h")).toSeconds();
			ModSpectateRecord rec = new ModSpectateRecord(until, p.getName(), service.worldKey(p.getWorld()), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), now);
			modStore.putRecord(p.getUniqueId(), rec);
			modStore.save();
			String untilFormatted = dtf.format(Instant.ofEpochSecond(until));
			long remaining = Math.max(0, until - now);
			String durationStr = humanDuration(remaining);
			MiniMessage mm = service.mm();
			Bukkit.getScheduler().runTask(plugin, () -> {
				p.setGameMode(GameMode.SPECTATOR);
				String msg = config.getString("moderator.enterMessage", "<yellow>Du bist im Zuschauermodus bis %until%.")
						.replace("%until%", untilFormatted);
				p.sendMessage(mm.deserialize(service.getPrefix() + " " + msg));
				if (streamerMode) {
					String kickMsg = config.getString("moderator.streamerKickMessage", "<gray>Stream-Übergang. Du wurdest kurz getrennt.</gray>")
						.replace("%spielername%", p.getName())
						.replace("%X%", String.valueOf(loc.getBlockX()))
						.replace("%Y%", String.valueOf(loc.getBlockY()))
						.replace("%Z%", String.valueOf(loc.getBlockZ()))
						.replace("%dimension_phrase%", dimensionPhrase)
						.replace("%time%", timeFormatted)
						.replace("%until%", untilFormatted)
						.replace("%duration%", durationStr)
						.replace("%todesgrund%", funnyReason);
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

		Bukkit.getScheduler().runTask(plugin, () -> {
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
		String cause = last.getCause().name();
		switch (cause) {
			case "FALL": return who + " wollte fliegen lernen.";
			case "DROWNING": return who + " hat zu lange die Luft angehalten.";
			case "LAVA": return who + " dachte, Lava sei ein Thermalbad.";
			case "FIRE":
			case "FIRE_TICK": return who + " hat mit dem Feuer gespielt.";
			case "VOID": return who + " ist vom Rand der Welt gefallen.";
			case "STARVATION": return who + " hat die Lunchbox vergessen.";
			case "POISON": return who + " hat fragwürdige Pilze probiert.";
			case "CONTACT": return who + " hat die Umarmung der Kakteen unterschätzt.";
			case "SUFFOCATION": return who + " wurde von Blöcken erdrückt.";
			case "BLOCK_EXPLOSION":
			case "ENTITY_EXPLOSION": return who + " hat zu nah an der Party gefeiert.";
			case "MAGIC": return who + " hat magische Nebenwirkungen erfahren.";
			case "WITHER": return who + " hat den Wither unterschätzt.";
			case "CRAMMING": return who + " stand etwas zu eng.";
			case "PROJECTILE": return who + " hat ein fliegendes Geschenk gefangen.";
			case "ENTITY_ATTACK": return who + " hat eine ungesunde Umarmung kassiert.";
			case "ENTITY_SWEEP_ATTACK": return who + " ist in einen Schwung geraten.";
			case "FALLING_BLOCK": return who + " wurde von der Schwerkraft erschlagen.";
			case "LIGHTNING": return who + " hat kostenloses Solarladen ausprobiert.";
			case "HOT_FLOOR": return who + " hat den Boden aus Lava zu wörtlich genommen.";
			case "FLY_INTO_WALL": return who + " hat die Elytra ohne TÜV geflogen.";
			case "FREEZE": return who + " hat den Winter unterschätzt.";
			case "DRAGON_BREATH": return who + " hat zu nah am Drachen geatmet.";
			case "FIREWORK": return who + " ist mit Raketen wirklich durchgestartet.";
			case "THORNS": return who + " hat sich an Dornen die Meinung geholt.";
			case "SONIC_BOOM": return who + " hat den Warden zu laut geweckt.";
			case "MELTING": return who + " ist einfach dahingeschmolzen.";
			case "DRYOUT": return who + " ist an akuter Trockenheit verendet.";
			case "WORLD_BORDER": return who + " hat am Rand der Welt zu sehr gedrängelt.";
			case "SUICIDE":
			case "KILL": return who + " hat dem Leben freiwillig den Rücken gekehrt.";
			case "CUSTOM": return who + " wurde auf mysteriöse Weise aus dem Leben gerissen.";
			default:
				return who + " starb an " + cause.toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
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
