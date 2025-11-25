# DeathBan (Purpur/Paper 1.21.10+, Java 21)
![banner-1280x640](https://github.com/user-attachments/assets/ca08173c-7636-4f0f-baa0-327a0e07b6c1)
 
Leichtgewichtiges, produktiv erprobtes DeathBan-Plugin für Purpur/Paper-Server. Nach dem Tod wird ein Spieler für eine konfigurierbare Dauer gebannt und (optional) ein schönes Discord‑Embed verschickt. Moderatoren/Streamer werden nicht dauerhaft gebannt, sondern in den Zuschauermodus versetzt – mit sicherem Rückteleport zum Spawn nach Ablauf.

## Features
- 🔒 **DeathBan**: Ban direkt nach Tod für eine Dauer wie `24h`, `1d2h30m`, `90m` (persistiert in `bans.yml`).
- 🧵 **Eigene Bann‑Nachrichten**: In‑Game via MiniMessage (`prefix`, `%time%`, `%until%`, `%dimension_phrase%`, Koordinaten u. a.).
- 💬 **Discord‑Webhook (optional)**: Rotes Embed, 3D‑Spielerkopf, Felder (Coords, Welt, Todesgrund), `<t:%timestamp%>` für relative Zeitdarstellung. Konfigurierbar in `discord.yml`.
- 🛡️ **Moderator‑Flow** (`deathban.moderator`): Statt Ban → Spectator bis Ablauf; GameMode‑Wechsel wird blockiert; nach Ablauf: Teleport zum sicheren Respawn (Bett/Anker, sonst Weltspawn) und Wechsel zurück nach SURVIVAL.
- 📹 **Streamer‑Flow** (`deathban.streamer`): Wie Moderator, zusätzlich einmaliger **Kick direkt nach dem Tod** (sauberer Stream‑Cut), danach automatisch Spectator bis Ablauf.
- 🧭 **Sicherer Respawn**: Ende der Sperre → Teleport auf höchste sichere Y über Spawn und Wechsel zu SURVIVAL, auch wenn der Spieler erst später joint.
- 🧩 **PlaceholderAPI‑Integration**: `%deathban_players%`, `%deathban_mods%`, `%deathban_total%`.
- 🧰 **Kommandos & Autocomplete**: Komfortables Tab‑Completion (nur relevante Ziele bei `unban`/`modunban`).
- 🧾 **Persistenz**: `bans.yml` (Spieler‑Bans) und `mod_spectate.yml` (Moderator/Streamer‑Sperren).

## Anforderungen
- **Server**: Purpur/Paper 1.21.10 oder neuer
- **Java**: 21
- **Optional**: 
  - [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) – zusätzliche Platzhalter
  - [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) – Discord‑Mentions `<@id>` im Todesgrund
  - (Soft‑Depend: `PlaceholderAPI`, `AdvancedBan`, `DiscordSRV`)

## Installation
1. JAR in `server/plugins/` kopieren.
2. Server starten → `plugins/DeathBan/` mit `config.yml`, `discord.yml` etc. wird angelegt.
3. Optional: `discord.yml` mit Webhook‐URL ausfüllen.

## Build (Windows)
- `build.bat` im Projektordner `DeathBan/` ausführen. Das Skript erzeugt automatisch den Gradle‑Wrapper (8.10.2), baut `shadowJar` und kopiert die JAR nach `plugins/DeathBan.jar`.
- Alternativ: `gradlew clean shadowJar` und `build/libs/DeathBan-<version>.jar` manuell kopieren.

## Konfiguration (Auszug)
`src/main/resources/config.yml`
```yml
# DeathBan Konfiguration
banDuration: "24h"
prefix: "<gradient:#9483ff:#fb9af2>DeathBan</gradient>"
dateTimeFormat: "dd.MM.yyyy HH:mm:ss"

# Nachricht beim Kick direkt nach dem Tod
banMessage: |
  <gray>Du bist gestorben und für <yellow>%duration%</yellow> gebannt.</gray>
  <gray>Koordinaten: <white>X: %X%, Y: %Y%, Z: %Z%</white> (<white>%dimension_phrase%</white>)</gray>
  <gray>Zeit: <white>%time%</white> | Ende: <white>%until%</white></gray>

# Nachricht beim Join während aktiver Sperre
joinDenyMessage: |
  <red>Du bist noch für <yellow>%remaining%</yellow> gesperrt.</red>
  <gray>Ende: <white>%until%</white></gray>

# Moderatoren/Streamer
moderator:
  enabled: true
  enterMessage: "<yellow>Du bist gestorben. Du wirst bis <white>%until%</white> in den Zuschauermodus versetzt."
  restoreMessage: "<green>Deine Todeszeit ist vorbei. Du bist nun wieder im Survival."
  # Zusätzlich nur für Streamer (einmaliger Kick direkt nach Tod):
  streamerKickMessage: |
    <gray>Stream-Übergang:</gray> <white>Du wurdest kurz getrennt.</white>
    <gray>Du bist bis <white>%until%</white> im Zuschauermodus.</gray>

# Weltbezeichnungen für %dimension_phrase%
worldNames:
  normal: "Oberwelt"
  nether: "Nether"
  the_end: "End"

# Spieler mit dieser Permission werden nie gebannt
respectExemptPermission: true
```

`src/main/resources/discord.yml`
```yml
enabled: true
webhookUrl: ""
useBreed: true
mentions: ""               # z. B. @here
avatarUrl: "https://mc-heads.net/avatar/%spielername%/64"
username: "DeathBan"
embed:
  title: "DeathBan"
  description: "**%spielername%** ist <t:%timestamp%> gestorben."
  color: 0xFF0000
  footer: "DeathBan"
  thumbnail: "https://mc-heads.net/head/%spielername%/64"
  fields:
    - name: "Coords:"
      value: "X: %X%, Y: %Y%, Z: %Z%"
      inline: true
    - name: "Welt:"
      value: "%s%"   # z. B. „in der Oberwelt“
      inline: true
    - name: "Todesgrund"
      value: "%todesgrund%"
      inline: false
```

## Platzhalter
- `%spielername%`, `%X%`, `%Y%`, `%Z%`
- `%dimension_phrase%` – „in der Oberwelt | im Nether | im End“
- `%time%` / `%until%` – formatiert gemäß `dateTimeFormat`
- `%remaining%` – verbleibende Dauer (z. B. `1d2h`)
- `%timestamp%` – Unix‑Zeitstempel (für Discord `<t:%timestamp%>`)
- `%totesgrund%` – humorvoller Grund abhängig von der Todesursache (inkl. DiscordSRV‑Mention)

**PlaceholderAPI‑Expansion** (`deathban`):
- `%deathban_players%` – Anzahl aktiver DeathBans
- `%deathban_mods%` – Anzahl aktiver Moderator/Streamer‑Spectates
- `%deathban_total%` – Summe aus beidem

## Befehle & Rechte
- `/deathban` – ohne Rechte: kurze Plugin‑Info; mit Rechten: Hilfe
- `/deathban reload` – Konfiguration neu laden (`deathban.admin`)
- `/deathban unban <spieler>` – Spieler entbannen; meldet Fehler, wenn nicht gebannt (`deathban.admin`)
- `/deathban remaining <spieler>` – Restzeit & Ende anzeigen (`deathban.admin`)
- `/deathban list` – aktive DeathBans auflisten (`deathban.admin`)
- `/deathban listmods` – aktive Moderator/Streamer‑Sperren auflisten (`deathban.admin`)
- `/deathban modunban <spieler>` – Mod/Streamer freigeben; online: sofort Teleport+SURVIVAL, offline: beim nächsten Join (`deathban.admin`)

**Permissions**
- `deathban.admin` – Admin‑Befehle erlauben
- `deathban.moderator` – Moderator‑Flow (Spectator statt Ban)
- `deathban.streamer` – Moderator‑Flow + einmaliger Kick nach Tod
- `deathban.exempt` – komplett von DeathBan ausgenommen

## Hinweise
- `mod_spectate.yml`/`bans.yml` werden automatisch bereinigt, wenn Sperren ablaufen.
- GameMode‑Wechsel von aktiven Mods/Streamern wird bis zum Ablauf blockiert (Schutz vor „Rauscheaten“).

---
Fragen/Ideen? Pull Requests & Issues sind willkommen! 😊
