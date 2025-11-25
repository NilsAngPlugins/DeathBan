package de.t0g3pii.deathban.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import de.t0g3pii.deathban.NilsAngDeathBanPlugin;
import de.t0g3pii.deathban.core.DeathBanService;
import de.t0g3pii.deathban.store.BanRecord;
import de.t0g3pii.deathban.store.ModSpectateRecord;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DeathBanCommand implements CommandExecutor, TabCompleter {
	private final NilsAngDeathBanPlugin plugin;

	public DeathBanCommand(NilsAngDeathBanPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		DeathBanService service = plugin.getService();
		MiniMessage mm = service.mm();
		String px = service.getPrefix();

		boolean hasAdmin = sender.hasPermission("deathban.admin");
		if (!hasAdmin) {
			if (args.length == 0) {
				sendInfo(sender, mm, px);
				return true;
			} else {
				sender.sendMessage(mm.deserialize(px + " <red>Keine Berechtigung.</red>"));
				return true;
			}
		}

		if (args.length == 0) {
			sendUsage(sender, mm, px, label);
			return true;
		}

		String sub = args[0].toLowerCase();
		switch (sub) {
			case "reload":
				plugin.reloadAll();
				sender.sendMessage(mm.deserialize(px + " <green>Konfiguration neu geladen.</green>"));
				return true;

			case "unban":
				if (args.length < 2) { sendUsage(sender, mm, px, label); return true; }
				OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
				UUID uid = op.getUniqueId();
				if (!service.isBanned(uid)) {
					sender.sendMessage(mm.deserialize(px + " <red>" + (op.getName()==null?uid:op.getName()) + " ist nicht tot/gebanned.</red>"));
					return true;
				}
				service.unban(uid);
				sender.sendMessage(mm.deserialize(px + " <green>Spieler <white>" + (op.getName()==null?uid:op.getName()) + "</white> wurde entbannt.</green>"));
				return true;

			case "remaining":
				if (args.length < 2) { sendUsage(sender, mm, px, label); return true; }
				OfflinePlayer op2 = Bukkit.getOfflinePlayer(args[1]);
				OptionalLong rem = service.getRemainingSeconds(op2.getUniqueId());
				if (rem.isEmpty()) {
					sender.sendMessage(mm.deserialize(px + " <gray>" + (op2.getName()==null?op2.getUniqueId():op2.getName()) + "</gray> <green>ist nicht gebannt.</green>"));
					return true;
				}
				long now = Instant.now().getEpochSecond();
				long until = now + rem.getAsLong();
				String datePattern = plugin.getConfig().getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern(datePattern).withZone(ZoneId.systemDefault());
				String untilFormatted = dtf.format(Instant.ofEpochSecond(until));
				Optional<BanRecord> rec = service.getRecord(op2.getUniqueId());
				String extra = rec.map(r -> {
					String world = r.worldKey != null ? r.worldKey : "?";
					return " <dark_gray>|</dark_gray> <gray>Welt:</gray> <white>" + world + "</white> <dark_gray>|</dark_gray> <gray>XYZ:</gray> <white>" + r.x + ", " + r.y + ", " + r.z + "</white>";
				}).orElse("");
				sender.sendMessage(mm.deserialize(px + " <gold>Verbleibend:</gold> <yellow>" + humanDuration(rem.getAsLong()) + "</yellow> <dark_gray>|</dark_gray> <gray>Ende:</gray> <white>" + untilFormatted + "</white>" + extra));
				return true;

			case "list":
				Map<UUID, BanRecord> all = plugin.getStore().listAll();
				if (all.isEmpty()) {
					sender.sendMessage(mm.deserialize(px + " <gray>Keine aktiven DeathBans.</gray>"));
					return true;
				}
				String datePattern2 = plugin.getConfig().getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
				DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern(datePattern2).withZone(ZoneId.systemDefault());
				sender.sendMessage(mm.deserialize(px + " <#9483ff>Aktive DeathBans</#9483ff>"));
				for (Map.Entry<UUID, BanRecord> e : all.entrySet()) {
					BanRecord r = e.getValue();
					String name = r.playerName != null ? r.playerName : e.getKey().toString();
					String untilStr = dtf2.format(Instant.ofEpochSecond(r.untilEpochSeconds));
					String deathStr = r.deathEpochSeconds > 0 ? dtf2.format(Instant.ofEpochSecond(r.deathEpochSeconds)) : "?";
					sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>" + name + "</white> <dark_gray>|</dark_gray> <gray>Tod:</gray> <white>" + deathStr + "</white> <dark_gray>|</dark_gray> <gray>Entbannung:</gray> <white>" + untilStr + "</white> <dark_gray>|</dark_gray> <gray>Welt:</gray> <white>" + (r.worldKey == null ? "?" : r.worldKey) + "</white> <dark_gray>|</dark_gray> <gray>XYZ:</gray> <white>" + r.x + ", " + r.y + ", " + r.z + "</white>"));
				}
				return true;

			case "listmods":
				Map<UUID, ModSpectateRecord> mods = plugin.getModSpectateStore().all();
				if (mods.isEmpty()) {
					sender.sendMessage(mm.deserialize(px + " <gray>Keine aktiven Moderator-Sperren.</gray>"));
					return true;
				}
				String datePattern3 = plugin.getConfig().getString("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
				DateTimeFormatter dtf3 = DateTimeFormatter.ofPattern(datePattern3).withZone(ZoneId.systemDefault());
				sender.sendMessage(mm.deserialize(px + " <#fb9af2>Aktive Moderator-Sperren</#fb9af2>"));
				for (Map.Entry<UUID, ModSpectateRecord> e : mods.entrySet()) {
					ModSpectateRecord r = e.getValue();
					String name = r.playerName != null ? r.playerName : e.getKey().toString();
					String untilStr = dtf3.format(Instant.ofEpochSecond(r.untilEpochSeconds));
					String deathStr = r.deathEpochSeconds > 0 ? dtf3.format(Instant.ofEpochSecond(r.deathEpochSeconds)) : "?";
					sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>" + name + "</white> <dark_gray>|</dark_gray> <gray>Tod:</gray> <white>" + deathStr + "</white> <dark_gray>|</dark_gray> <gray>Ende:</gray> <white>" + untilStr + "</white> <dark_gray>|</dark_gray> <gray>Welt:</gray> <white>" + (r.worldKey == null ? "?" : r.worldKey) + "</white> <dark_gray>|</dark_gray> <gray>XYZ:</gray> <white>" + r.x + ", " + r.y + ", " + r.z + "</white>"));
				}
				return true;

			case "modunban":
				if (args.length < 2) { sendUsage(sender, mm, px, label); return true; }
				OfflinePlayer mop = Bukkit.getOfflinePlayer(args[1]);
				UUID mid = mop.getUniqueId();
				Optional<ModSpectateRecord> recOpt = plugin.getModSpectateStore().getRecord(mid);
				if (recOpt.isEmpty()) {
					sender.sendMessage(mm.deserialize(px + " <red>" + (mop.getName()==null?mid:mop.getName()) + " ist nicht tot (Moderator/Streamer).</red>"));
					return true;
				}
				if (mop.isOnline()) {
					Player online = mop.getPlayer();
					plugin.getModSpectateStore().remove(mid);
					plugin.getModSpectateStore().save();
					Location safe = computeSafeRespawn(online);
					online.teleport(safe);
					online.setGameMode(GameMode.SURVIVAL);
					sender.sendMessage(mm.deserialize(px + " <green>Moderator <white>" + online.getName() + "</white> wurde wiederbelebt.</green>"));
				} else {
					ModSpectateRecord r = recOpt.get();
					ModSpectateRecord updated = new ModSpectateRecord(Instant.now().getEpochSecond() - 1, r.playerName, r.worldKey, r.x, r.y, r.z, r.deathEpochSeconds);
					plugin.getModSpectateStore().putRecord(mid, updated);
					plugin.getModSpectateStore().save();
					sender.sendMessage(mm.deserialize(px + " <green>Moderator <white>" + (mop.getName()==null?mid:mop.getName()) + "</white> wird beim nächsten Join wiederbelebt.</green>"));
				}
				return true;

			default:
				sendUsage(sender, mm, px, label);
				return true;
		}
	}

	private void sendInfo(CommandSender sender, MiniMessage mm, String px) {
		@SuppressWarnings("deprecation")
		String version = plugin.getDescription().getVersion();
		@SuppressWarnings("deprecation")
		String authors = String.join(", ", plugin.getDescription().getAuthors());
		sender.sendMessage(mm.deserialize(px + " <gray>v" + version + "</gray>"));
		sender.sendMessage(mm.deserialize("<gray>Autor(en):</gray> <white>" + authors + "</white>"));
		sender.sendMessage(mm.deserialize("<gray>Funktionen:</gray> <white>DeathBan ist ein Plugin das Spieler nach dem Tod für eine konfigurierbare Dauer bannt.</white>"));
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("deathban.admin")) return Collections.emptyList();
		if (args.length == 1) {
			List<String> subs = Arrays.asList("reload","unban","remaining","list","listmods","modunban");
			String p = args[0].toLowerCase();
			return subs.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
		}
		if (args.length == 2) {
			String sub = args[0].toLowerCase();
			if (sub.equals("unban") || sub.equals("remaining")) {
				Map<UUID, BanRecord> all = plugin.getStore().listAll();
				return toNames(all.keySet(), args[1]);
			}
			if (sub.equals("modunban")) {
				Map<UUID, ModSpectateRecord> mods = plugin.getModSpectateStore().all();
				return toNames(mods.keySet(), args[1]);
			}
		}
		return Collections.emptyList();
	}

	private List<String> toNames(Set<UUID> ids, String prefix) {
		String p = prefix == null ? "" : prefix.toLowerCase();
		List<String> names = new ArrayList<>();
		for (UUID id : ids) {
			OfflinePlayer op = Bukkit.getOfflinePlayer(id);
			String name = op != null && op.getName() != null ? op.getName() : id.toString();
			if (name.toLowerCase().startsWith(p)) names.add(name);
		}
		return names;
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

	private void sendUsage(CommandSender sender, MiniMessage mm, String px, String label) {
		sender.sendMessage(mm.deserialize(px + " <#9483ff>Befehle</#9483ff>"));
		sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>/" + label + " reload</white> <dark_gray>-</dark_gray> <gray>Konfiguration neu laden</gray>"));
		sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>/" + label + " unban <spieler></white> <dark_gray>-</dark_gray> <gray>Spieler entbannen</gray>"));
		sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>/" + label + " remaining <spieler></white> <dark_gray>-</dark_gray> <gray>Restzeit anzeigen</gray>"));
		sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>/" + label + " list</white> <dark_gray>-</dark_gray> <gray>Aktive DeathBans</gray>"));
		sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>/" + label + " listmods</white> <dark_gray>-</dark_gray> <gray>Aktive Moderator-Sperren</gray>"));
		sender.sendMessage(mm.deserialize("<gray>\u2022</gray> <white>/" + label + " modunban <spieler></white> <dark_gray>-</dark_gray> <gray>Moderator-Sperre aufheben</gray>"));
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
