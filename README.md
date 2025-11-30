# DeathBan (Purpur/Paper 1.21.10+, Java 21)
![banner-1280x640](https://github.com/user-attachments/assets/ca08173c-7636-4f0f-baa0-327a0e07b6c1)
 
Leichtgewichtiges, produktiv erprobtes DeathBan-Plugin für Purpur/Paper-Server. Nach dem Tod wird ein Spieler für eine konfigurierbare Dauer gebannt und (optional) ein schönes Discord‑Embed verschickt. Moderatoren/Streamer werden nicht dauerhaft gebannt, sondern in den Zuschauermodus versetzt – mit sicherem Rückteleport zum Spawn nach Ablauf.

## Features
- 🔒 **DeathBan**: Ban direkt nach Tod für eine Dauer wie `24h`, `1d2h30m`, `90m` (persistiert in `bans.yml`).
- 🧵 **Eigene Bann‑Nachrichten**: In‑Game via MiniMessage (`prefix`, `%time%`, `%until%`, `%dimension_phrase%`, Koordinaten u. a.). Die `moderator.streamerKickMessage` unterstützt dieselben Platzhalter wie `banMessage` (inkl. `%todesgrund%`).
- 💬 **Discord‑Webhook (optional)**: Rotes Embed, 3D‑Spielerkopf, Felder (Coords, Welt, Todesgrund), `<t:%timestamp%>` für relative Zeitstempel. Konfigurierbar in `discord.yml`.
- 🛡️ **Moderator‑Flow** (`deathban.moderator`): Statt Ban → Spectator bis Ablauf; GameMode‑Wechsel wird blockiert; nach Ablauf: Teleport zum sicheren Respawn (Bett/Anker, sonst Weltspawn) und Wechsel zurück nach SURVIVAL.
- 📹 **Streamer‑Flow** (`deathban.streamer`): Wie Moderator, zusätzlich ein einmaliger **Kick direkt nach dem Tod** (sauberer Stream‑Cut). `deathban.streamer` impliziert den Moderator‑Flow – die zusätzliche `deathban.moderator`‑Permission ist nicht nötig.
- 🧭 **Sicherer Respawn**: Ende der Sperre → Teleport auf höchste sichere Y über dem (Bett‑/Anker‑ oder Welt‑)Spawn und erst dann SURVIVAL, auch beim Rejoin.
- 🧩 **PlaceholderAPI‑Integration**: `%deathban_players%`, `%deathban_mods%`, `%deathban_total%`, `%deathban_isdead%`, `%deathban_isdead_<Name>%`, `%deathban_mod_remaining( _<Name>)%`, `%deathban_mod_until( _<Name>)%`, `%deathban_next*%`.
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
- `build.bat` im Projektordner `DeathBan/` ausführen (erzeugt Gradle‑Wrapper 8.10.2, baut `shadowJar`, kopiert nach `plugins/DeathBan.jar`).
- Alternativ: `gradlew clean shadowJar` und JAR aus `build/libs/` manuell kopieren.

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

# Nachricht, wenn ein gebannter Spieler versucht zu joinen
joinDenyMessage: |
  <red>Du bist noch für <yellow>%remaining%</yellow> gesperrt.</red>
  <gray>Ende: <white>%until%</white></gray>

# Moderatoren/Streamer
moderator:
  enabled: true
  enterMessage: "<yellow>Du bist gestorben. Du wirst bis <white>%until%</white> in den Zuschauermodus versetzt."
  restoreMessage: "<green>Deine Todeszeit ist vorbei. Du bist nun wieder im Survival."
  # Nur für Streamer (einmaliger Kick nach dem Tod). Unterstützt dieselben Platzhalter wie 'banMessage'.
  streamerKickMessage: |
    <gray>Stream-Übergang:</gray> <white>%spielername%</white> ist <white>%time%</white> gestorben (<gray>%todesgrund%</gray>).
    <gray>Du bist bis <white>%until%</white> im Zuschauermodus.</gray>

# Welt-Bezeichnungen für %dimension_phrase%
worldNames:
  normal: "Oberwelt"
  nether: "Nether"
  the_end: "End"

# Spieler mit Permission 'deathban.exempt' sind ausgenommen
respectExemptPermission: true
```

`src/main/resources/discord.yml`
```yml
enabled: true
webhookUrl: ""
useEmbed: true
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
      value: "%dimension_phrase%"
      inline: true
    - name: "Todesgrund"
      value: "%todesgrund%"
      inline: false
```

## Platzhalter (Nachrichten/Discord)
- `%spielername%` – Spielername (z. B. `t0g3pii`)
- `%X%` / `%Y%` / `%Z%` – Block‑Koordinaten (z. B. `123`, `71`, `-19`)
- `%dimension_phrase%` – „in der Oberwelt | im Nether | im End“ (z. B. `in der Oberwelt`)
- `%time%` – formatiertes Todes‑Datum (z. B. `11.10.2025 20:15:00`)
- `%until%` – formatiertes Ende (z. B. `12.10.2025 20:15:00`)
- `%remaining%` – verbleibende Dauer (z. B. `23h59m`)
- `%timestamp%` – Unix‑Zeit (z. B. `1760205888`)
- `%todesgrund%` – humorvoller Grund (z. B. `wollte fliegen lernen.`)

## PlaceholderAPI‑Expansion (`deathban`) – mit Beispielen
- `%deathban_players%` → `2`
- `%deathban_mods%` → `1`
- `%deathban_total%` → `3`
- `%deathban_isdead%` → `true` (wenn Aufrufer gebannt/Mod‑Spectate), sonst `false`
- `%deathban_isdead_Spielername%` → `false`
- `%deathban_mod_remaining%` → `3h12m`
- `%deathban_mod_until%` → `11.10.2025 20:15:00`
- `%deathban_mod_remaining_Spielername%` → `1d2h`
- `%deathban_mod_until_Spielername%` → `12.10.2025 08:00:00`
- `%deathban_next_mod%` → `ModName`
- `%deathban_next_mod_remaining%` → `45m`
- `%deathban_next_mod_until%` → `11.10.2025 21:00:00`
- `%deathban_next%` → `SpielerOderMod`
- `%deathban_next_remaining%` → `10m`
- `%deathban_next_until%` → `11.10.2025 20:30:00`

## Befehle & Rechte
- `/deathban` – ohne Rechte: kurze Plugin‑Info; mit Rechten: Hilfe
- `/deathban reload` – Konfiguration neu laden (`deathban.admin`)
- `/deathban unban <spieler>` – Spieler entbannen; meldet Fehler, wenn nicht gebannt (`deathban.admin`)
- `/deathban remaining <spieler>` – Restzeit & Ende anzeigen (für Bans und Mod‑Spectate) (`deathban.admin`)
- `/deathban list` – Aktive DeathBans auflisten (`deathban.admin`)
- `/deathban listmods` – Aktive Moderator‑/Streamer‑Sperren auflisten (`deathban.admin`)
- `/deathban modunban <spieler>` – Mod/Streamer freigeben; online: Teleport→SURVIVAL, offline: beim nächsten Join (`deathban.admin`)

**Permissions**
- `deathban.admin` – Admin‑Befehle erlauben
- `deathban.moderator` – Moderator‑Flow (Spectator statt Ban)
- `deathban.streamer` – Streamer‑Flow (inkl. einmaligem Kick); beinhaltet den Moderator‑Flow
- `deathban.exempt` – komplett vom DeathBan ausgenommen

## Hinweise
- `mod_spectate.yml`/`bans.yml` werden automatisch bereinigt, wenn Sperren ablaufen.
- GameMode‑Wechsel von aktiven Mods/Streamern wird bis zum Ablauf blockiert (Schutz vor „Rausdrücken“).

---
Fragen/Ideen? Pull Requests & Issues sind willkommen! 😊
