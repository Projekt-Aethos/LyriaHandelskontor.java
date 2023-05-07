package org.silvius.lyriahandelskontor.Buttons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.NotNull;
import org.silvius.lyriahandelskontor.LyriaHandelskontor;
import org.silvius.lyriahandelskontor.ShopControlPanelMenu;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import xyz.janboerman.guilib.api.menu.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class HandelskontorToggleButton<P extends Plugin> extends ToggleButton<MenuHolder<P>> {
    static FileConfiguration config;
    private static double connectCost;

    public HandelskontorToggleButton(boolean isRegistered) {
        super(HandelskontorToggleItem(isRegistered));
        config = LyriaHandelskontor.getPlugin().getConfig();
        connectCost = config.getDouble("HandelskontorCost");
    }

    @NotNull
    private static ItemStack HandelskontorToggleItem(boolean isRegistered) {
        ItemStack itemStack = new ItemStack(Material.ENDER_CHEST);
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if(isRegistered){
            itemMeta.displayName(Component.text("Verbunden: "+ ChatColor.GREEN+"✓").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(ChatColor.GRAY+"Linksklick: "+ChatColor.WHITE+"Löscht die Verbindung zum "+ChatColor.getByChar("b")+"Globalen Handelskontor"));}
        else{
            itemMeta.displayName(Component.text("Verbunden: "+ChatColor.RED+"✗").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(ChatColor.GRAY+"Linksklick: "+ChatColor.WHITE+"Erstellt eine Verbindung zum "+ChatColor.getByChar("b")+"Globalen Handelskontor"));
            lore.add(Component.text(ChatColor.GRAY+"Kosten: "+ChatColor.GOLD+connectCost+"Đ"));

        }
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack updateIcon(MenuHolder<P> holder, InventoryClickEvent event) {
        if(!(event.getClick()== ClickType.LEFT)){return this.getIcon();}
        Player player = (Player) event.getWhoClicked();
        double bal = LyriaHandelskontor.getEconomy().getBalance(player);

        ShopControlPanelMenu<P> shopHolder = (ShopControlPanelMenu<P>) holder;
        NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "registeredHandelskontor");
        TileState sign = shopHolder.sign;
        PersistentDataContainer persistentDataContainer = sign.getPersistentDataContainer();
        boolean registerdHandelskontor = persistentDataContainer.get(namespacedKey, PersistentDataType.INTEGER)==1;

        ItemStack itemStack = HandelskontorToggleItem(!registerdHandelskontor);
        if(registerdHandelskontor){
            persistentDataContainer.set(namespacedKey, PersistentDataType.INTEGER, 0);
        }
        else{
            persistentDataContainer.set(namespacedKey, PersistentDataType.INTEGER, 1);
            if(bal<connectCost){
                player.sendMessage(ChatColor.RED+"Nicht genug Geld!");
                return HandelskontorToggleItem(false);
            }
            LyriaHandelskontor.getEconomy().withdrawPlayer(player, connectCost);
        }
        sign.update();
        return itemStack;
    }

}
