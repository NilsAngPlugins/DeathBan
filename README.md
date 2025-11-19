## NilsANG DeathBan (Purpur 1.21.10, Java 21)

Ein leichtgewichtiges DeathBan-Plugin für Purpur/Paper: nach dem Tod wird der Spieler für eine konfigurierbare Dauer gebannt und optional ein Discord-Embed an einen Webhook gesendet. Moderatoren können stattdessen automatisch in den Zuschauermodus versetzt werden.

### Features
- **DeathBan**: Ban direkt nach Tod für eine Dauer wie `24h`, `1d2h30m`, `90m` (persistiert in `bans.yml`).
- **Discord-Webhook**: Schönes Embed mit Titel, Farbe (Rot), 3D-Spielerkopf, Feldern (Coords, Welt, Todesgrund) und Zeitstempel; konfigurierbar über `discord.yml`.
- **MiniMessage**: Farbige Nachrichten ingame (Prefix: `<gradient:#9483ff:#fb9af2>NilsANG</gradient>`).
- **Moderator-Flow**: Mit Permission werden Moderatoren nicht gebannt, sondern bis zum Ablauf in Spectator gesetzt; GameMode-Wechsel blockiert; nach Ablauf Teleport zum Respawnpunkt (Bett/Anker, sonst Welt-Spawn) und **dann** SURVIVAL (persistiert in `mod_spectate.yml`).
- **Reload & Verwaltung**: Befehle für Reload, Unban, Restzeit-Anzeige und Listen für alle aktiven Bans/Mod-Sperren.

### Anforderungen
- **Server**: Purpur/Paper 1.21.10+
- **Java**: 21
- **Optional**: PlaceholderAPI (für eigene Placeholder), DiscordSRV (für automatische Mentions `<@id>` im Todesgrund)

### Installation
- Release-JAR in den Ordner `plugins/` legen und Server starten.
- Alternativ: selbst bauen (siehe Abschnitt Build).

### Konfiguration (Auszug)
`config.yml`
```yml
banDuration: "24h"
prefix: "<gradient:#9483ff:#fb9af2>NilsANG</gradient>"
dateTimeFormat: "dd.MM.yyyy HH:mm:ss"
banMessage: |
  <gray>Du bist gestorben und für <yellow>%duration%</yellow> gebannt.</gray>
  <gray>Koordinaten: <white>X: %X%, Y: %Y%, Z: %Z%</white> (<white>%dimension_phrase%</white>)</gray>
  <gray>Zeit: <white>%time%</white> | Ende: <white>%until%</white></gray>
joinDenyMessage: |
  <red>Du bist noch für <yellow>%remaining%</yellow> gesperrt.</red>
  <gray>Ende: <white>%until%</white></gray>
moderator:
  enabled: true
  enterMessage: "<yellow>Du bist gestorben. Du wirst bis <white>%until%</white> in den Zuschauermodus versetzt."
  restoreMessage: "<green>Deine Todeszeit ist vorbei. Du bist nun wieder im Survival."
worldNames:
  normal: "Oberwelt"
  nether: "Nether"
  the_end: "End"
```

`discord.yml`
```yml
enabled: true
webhookUrl: ""
useEmbed: true
mentions: ""
avatarUrl: "https://mc-heads.net/avatar/%spielername%/64"
username: "NilsANG DeathBan"
embed:
  title: "DeathBan"
  description: "**%spielername%** ist <t:%timestamp%> gestorben."
  color: 0xFF0000
  footer: "NilsANG"
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

### Platzhalter (Auswahl)
- **%spielername%**: Spielername
- **%X% %Y% %Z%**: Block-Koordinaten
- **%dimension_phrase%**: "in der Oberwelt" / "im Nether" / "im End"
- **%time%**: formatierte Todeszeit gemäß `dateTimeFormat`
- **%until%**: formatiertes Ban-Ende gemäß `dateTimeFormat`
- **%remaining%**: verbleibende Dauer (z. B. `1d2h`)
- **%timestamp%**: Unix-Zeit (für Discord `<t:%timestamp%>`) 
- **%todesgrund%**: humorvoller Text je nach Ursache, inkl. DiscordSRV-Ping (falls verlinkt)

### Befehle
- `/deathban reload` – Konfiguration neu laden
- `/deathban unban <spieler>` – Spieler entbannen
- `/deathban remaining <spieler>` – Restzeit anzeigen
- `/deathban list` – Aktive DeathBans auflisten
- `/deathban listmods` – Aktive Moderator-Sperren auflisten
- `/deathban modunban <spieler>` – Moderator-Spectator-Sperre aufheben

### Berechtigungen
- `nilsang.deathban.admin` – Zugriff auf Admin-Befehle (default: op)
- `nilsang.deathban.exempt` – Spieler ist vom DeathBan ausgenommen
- `nilsang.deathban.moderator` – Moderator-Spectator-Flow statt Ban

### Build
- Windows: `build.bat` im Projekt ausführen (Gradle-Wrapper wird automatisch erzeugt, Artefakt wird nach `plugins/NilsANG-DeathBan.jar` kopiert).
- Alternativ: `gradlew clean shadowJar` und JAR aus `build/libs/` manuell nach `plugins/` kopieren.

### Hinweise
- **Kompatibilität**: Purpur/Paper 1.21.10+, Java 21.
- **Optional**: DiscordSRV (Mentions), PlaceholderAPI (weitere Placeholder), LuckPerms (Permissions-Management).

### Lizenz
MIT (oder anpassen, je nach Repository-Richtlinie).
