package org.mineacademy.protect.hook;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;
import org.wargamer2010.signshop.events.SSPostTransactionEvent;

public final class SignShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopTransaction(final SSPostTransactionEvent event) {
		final Type type = Type.fromBoolean(event.getOperation().toLowerCase().contains("buy"));
		final String sellerName = event.getOwner().getName();
		final UUID sellerUid = event.getOwner().GetIdentifier().getOfflinePlayer().getUniqueId();
		final Player buyer = event.getPlayer().getPlayer();
		final double price = event.getPrice();
		final Location location = event.getShop().getSignLocation();

		for (final ItemStack item : event.getItems())
			Transaction.logPlayer(location, type, buyer, "SignShop", price, sellerName, sellerUid, item, 1);
	}
}
