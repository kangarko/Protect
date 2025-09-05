package org.mineacademy.protect.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;

import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Events.TransactionEvent;

public final class ChestShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final TransactionEvent event) {
		final Location location = event.getSign().getLocation();
		final Type type = Transaction.Type.fromEnum(event.getTransactionType());
		final Player buyer = event.getClient();
		final Account seller = event.getOwnerAccount();
		final double price = event.getPrice();

		for (final ItemStack item : event.getStock()) {
			if (seller != null)
				Transaction.logPlayer(location, type, buyer, "ChestShop", price, seller.getName(), seller.getUuid(), item, 1);
			else
				Transaction.logServer(location, type, buyer, "ChestShop", price, item, 1);
		}
	}
}