package org.mineacademy.protect.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;

import me.gypopo.economyshopgui.api.events.PostTransactionEvent;
import me.gypopo.economyshopgui.util.Transaction.Result;

public final class EconomyShopGUIHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final PostTransactionEvent event) {
		final Location location = event.getPlayer().getLocation();
		final Type type = event.getTransactionType().toString().contains("BUY") ? Type.BUY : Type.SELL;
		final Player buyer = event.getPlayer();
		final double price = event.getPrice();
		final ItemStack item = event.getShopItem() != null ? event.getItemStack() : null;
		final int amount = event.getAmount();

		final Result result = event.getTransactionResult();

		if (item != null && (result == Result.SUCCESS || result == Result.SUCCESS_COMMANDS_EXECUTED || result == Result.NOT_ALL_ITEMS_ADDED || result == Result.NOT_ENOUGH_SPACE))
			Transaction.logPlayer(location, type, buyer, "EconomyShopGUI", price, "adminshop", null, item, amount);
	}
}