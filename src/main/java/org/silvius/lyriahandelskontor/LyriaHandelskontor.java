package org.silvius.lyriahandelskontor;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.maxgamer.quickshop.api.QuickShopAPI;

import java.util.logging.Logger;


public final class LyriaHandelskontor extends JavaPlugin {
    private static QuickShopAPI api;
    private static LyriaHandelskontor plugin;
    private static final Logger log = Logger.getLogger("Minecraft");
    public static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;
    @Override
    public void onEnable() {
        plugin = this;
        getCommand("handelskontor").setExecutor(new HandelskontorCommand());
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new HandelskontorCommand(), this);
        Plugin pluginQuickshop = Bukkit.getPluginManager().getPlugin("QuickShop");
        api = (QuickShopAPI)pluginQuickshop;
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static QuickShopAPI getQuickshopAPI(){
        return api;
    }

    public static LyriaHandelskontor getPlugin() {
        return plugin;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }

    public static Chat getChat() {
        return chat;
    }
}
