package org.mineacademy.protect.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;

import su.nightexpress.nexshop.api.shop.event.ShopTransactionEvent;

public final class ExcellentShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final ShopTransactionEvent event) {
		final su.nightexpress.nexshop.api.shop.Transaction transaction = event.getTransaction();

		final Location location = event.getPlayer().getLocation();
		final Type type = Type.fromEnum(transaction.getTradeType());
		final Player buyer = event.getPlayer();
		final double price = transaction.getPrice();
		final String sellerName = transaction.getProduct().getShop().getId();
		final ItemStack item = transaction.getProduct().getPreview().clone();
		final int amount = transaction.getUnits();

		Transaction.logPlayer(location, type, buyer, "ExcellentShop", price, sellerName, null, item, amount);
	}
}