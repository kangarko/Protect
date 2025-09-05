package org.mineacademy.protect.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;

import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import net.brcdev.shopgui.shop.ShopTransactionResult;

public final class ShopGUIHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final ShopPostTransactionEvent event) {
		final ShopTransactionResult result = event.getResult();

		final Location location = event.getResult().getPlayer().getLocation();
		final Type type = Type.fromBoolean(result.getShopAction().toString().toLowerCase().contains("buy"));
		final String sellerName = event.getResult().getShopItem().getShop().getId();
		final Player buyer = result.getPlayer();
		final double price = result.getPrice();
		final ItemStack item = result.getShopItem().getItem().clone();
		final int amount = result.getAmount();

		Transaction.logPlayer(location, type, buyer, "ShopGUI", price, sellerName, null, item, amount);
	}
}
