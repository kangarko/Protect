package org.mineacademy.protect.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.db.Transaction;

import com.bluecode.blueshop.events.ShopSignEvent;

public final class BlueShop3Hook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final ShopSignEvent event) {
		final Location location = event.getLocation();
		final Transaction.Type type = Transaction.Type.fromBoolean(event.isBuying());
		final Player buyer = Remain.getPlayerByUUID(event.getUuid());
		final double price = event.getPrice();
		final ItemStack item = event.getItem();
		final int amount = event.getAmount();

		Transaction.logServer(location, type, buyer, "BlueShop", price, item, amount);
	}
}