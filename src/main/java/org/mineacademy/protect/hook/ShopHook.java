package org.mineacademy.protect.hook;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;

import com.snowgears.shop.event.PlayerExchangeShopEvent;

public final class ShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final PlayerExchangeShopEvent event) {
		final Object shop = event.getShop();

		final Type type = Type.fromBoolean(event.getType().toString().toLowerCase().contains("buy"));
		final String sellerName = ReflectionUtil.invoke("getOwnerName", shop);
		final UUID sellerUid = ReflectionUtil.invoke("getOwnerUUID", shop);
		final Player buyer = event.getPlayer();
		final double price = ReflectionUtil.invoke("getPrice", shop);
		final ItemStack item = ReflectionUtil.invoke("getItemStack", shop);
		final Location location = buyer.getLocation();
		final int amount = ReflectionUtil.invoke("getAmount", shop);

		Transaction.logPlayer(location, type, buyer, "Shop", price, sellerName, sellerUid, item, amount);
	}
}
