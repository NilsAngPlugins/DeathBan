package dev.nilsang.deathban.listener;

import dev.nilsang.deathban.store.ModSpectateStore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeGuardListener implements Listener {
	private final ModSpectateStore store;
	private final String prefix;
	private final MiniMessage mm = MiniMessage.miniMessage();

	public GameModeGuardListener(ModSpectateStore store, String prefix) {
		this.store = store;
		this.prefix = prefix;
	}

	@EventHandler
	public void onChange(PlayerGameModeChangeEvent event) {
		if (!store.isActive(event.getPlayer().getUniqueId())) return;
		if (event.getNewGameMode() != GameMode.SPECTATOR) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(mm.deserialize(prefix + " <red>Du kannst den Zuschauermodus noch nicht verlassen.</red>"));
		}
	}
}
