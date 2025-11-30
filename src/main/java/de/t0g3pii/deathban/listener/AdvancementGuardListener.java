package de.t0g3pii.deathban.listener;

import de.t0g3pii.deathban.store.ModSpectateStore;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent; // Paper API

public class AdvancementGuardListener implements Listener {
	private final ModSpectateStore store;

	public AdvancementGuardListener(ModSpectateStore store) {
		this.store = store;
	}

	// Paper: Kriteriumsvergabe verhindern (zeigt keinen Toast und vergibt nichts)
	@EventHandler
	public void onCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
		Player p = event.getPlayer();
		if (!store.isActive(p.getUniqueId())) return;
		event.setCancelled(true);
	}

	// Fallback für andere Servervarianten: kurz nach Vergabe alles wieder entziehen
	@EventHandler
	public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
		Player p = event.getPlayer();
		if (!store.isActive(p.getUniqueId())) return;
		Advancement adv = event.getAdvancement();
		Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("DeathBan"), () -> {
			AdvancementProgress prog = p.getAdvancementProgress(adv);
			for (String c : prog.getAwardedCriteria()) {
				prog.revokeCriteria(c);
			}
		});
	}
}
