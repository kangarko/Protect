package org.mineacademy.protect;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.filter.FilterWorld;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.platform.BukkitPlugin;
import org.mineacademy.fo.platform.Platform;
import org.mineacademy.fo.region.DiskRegion;
import org.mineacademy.fo.visual.VisualizedRegion;
import org.mineacademy.protect.filter.FilterServer;
import org.mineacademy.protect.hook.BlueShop3Hook;
import org.mineacademy.protect.hook.ChestShopHook;
import org.mineacademy.protect.hook.EconomyShopGUIHook;
import org.mineacademy.protect.hook.ExcellentShopHook;
import org.mineacademy.protect.hook.McMMOHook;
import org.mineacademy.protect.hook.QuickShopHook;
import org.mineacademy.protect.hook.ShopGUIHook;
import org.mineacademy.protect.hook.ShopHook;
import org.mineacademy.protect.hook.SignShopHook;
import org.mineacademy.protect.hook.WorldEditHook;
import org.mineacademy.protect.model.TemporaryStorage;
import org.mineacademy.protect.model.db.Database;
import org.mineacademy.protect.operator.Groups;
import org.mineacademy.protect.operator.Rules;
import org.mineacademy.protect.settings.Settings;
import org.mineacademy.protect.task.PeriodScanTask;

/**
 * The main plugin class.
 */
public final class Protect extends BukkitPlugin {

	/**
	 * @see org.mineacademy.fo.platform.BukkitPlugin#getStartupLogo()
	 */
	@Override
	public String[] getStartupLogo() {
		return new String[] {
				"&b ___   ___   ___  _____  ____  __   _____ ",
				"&3| |_) | |_) / / \\  | |  | |_  / /`   | |  ",
				"&3|_|   |_| \\ \\_\\_/  |_|  |_|__ \\_\\_,  |_|  ",
				" ",
		};
	}

	@Override
	protected void onPluginLoad() {

		// Set regions
		DiskRegion.setCreatedPlayerRegionGetter(player -> PlayerCache.from(player).getCreatedRegion());
		DiskRegion.setCreatedPlayerRegionResetter(player -> PlayerCache.from(player).setCreatedRegion(new VisualizedRegion()));

		FilterServer.setNetworkServersSupplier(TemporaryStorage::getServerNames);
		FilterWorld.setNetworkWorldsSupplier(TemporaryStorage::getWorldNames);
	}

	@Override
	public void onPluginStart() {
		FileUtil.extractFolderFromJar("rules/", "rules/");

		if (Platform.isPluginInstalled("mcMMO"))
			McMMOHook.hook();

		if (Settings.WorldEdit.ENABLED && HookManager.isWorldEditLoaded())
			WorldEditHook.getInstance().register();

		Groups.getInstance().load();
		Rules.getInstance().load();

		if (Settings.TransactionLog.ENABLED) {
			if (Platform.isPluginInstalled("ChestShop"))
				this.registerEvents(new ChestShopHook());

			if (Platform.isPluginInstalled("SignShop"))
				this.registerEvents(new SignShopHook());

			if (Platform.isPluginInstalled("ShopGUIPlus"))
				this.registerEvents(new ShopGUIHook());

			if (Platform.isPluginInstalled("ExcellentShop"))
				this.registerEvents(new ExcellentShopHook());

			if (Platform.isPluginInstalled("BlueShop"))
				this.registerEvents(new BlueShop3Hook());

			if (Platform.isPluginInstalled("EconomyShopGUI"))
				this.registerEvents(new EconomyShopGUIHook());

			// do the same for QuickShop
			if (Platform.isPluginInstalled("QuickShop"))
				this.registerEvents(new QuickShopHook());

			final Plugin shop = Bukkit.getPluginManager().getPlugin("Shop");

			if (shop != null && shop.getDescription().getMain().startsWith("com.snowgears"))
				try {
					this.registerEvents(new ShopHook());

				} catch (final NoClassDefFoundError ex) {
					Common.warning("Unable to hook into Shop plugin, ignore this on old MC versions, otherwise report this problem.");
				}
		}

		if (Settings.Scan.PERIODIC.isEnabled())
			Platform.runTaskTimer(Settings.Scan.PERIODIC.getTimeTicks(), new PeriodScanTask());

		Platform.runTaskAsync(() -> Database.getInstance().purgeOldEntries());
	}

	@Override
	protected void onPluginReload() {
		Groups.getInstance().load();
		Rules.getInstance().load();
	}

	@Override
	protected void onPluginStop() {
		PlayerCache.clearCaches();
	}

	@Override
	public int getFoundedYear() {
		return 2024;
	}

	@Override
	public String getSentryDsn() {
		return "https://07dbef71fae765d2f336a17faaf1e441@o4508048573661184.ingest.us.sentry.io/4508052475674624";
	}

	@Override
	public int getBuiltByBitId() {
		return 44847;
	}

	@Override
	public String getBuiltByBitSharedToken() {
		return "UYl2VzxD9xaAiDVCnLTxTCDjD4vDuPr7";
	}
}
