package org.mineacademy.protect.hook;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopType;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.protect.model.db.Transaction;
import org.mineacademy.protect.model.db.Transaction.Type;

public final class QuickShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTransaction(final ShopPurchaseEvent event) {
		final Shop shop = event.getShop();

		final Location location = shop.getLocation();
		final Type type = shop.getShopType() == ShopType.BUYING ? Type.BUY : Type.SELL;
		final Player buyer = event.getPlayer();
		final double price = event.getTotal();
		final ItemStack item = shop.getItem();
		final int amount = event.getAmount();

		final UUID ownerUid = event.getShop().getOwner();

		Platform.runTaskAsync(0, () -> {
			final OfflinePlayer owner = Remain.getOfflinePlayerByUniqueId(ownerUid);

			Platform.runTask(() -> Transaction.logPlayer(location, type, buyer, "QuickShop", price, owner.getName(), ownerUid, item, amount));
		});

	}
}