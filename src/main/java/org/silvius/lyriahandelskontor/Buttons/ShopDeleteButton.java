package org.silvius.lyriahandelskontor.Buttons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.maxgamer.quickshop.api.shop.Shop;
import org.silvius.lyriahandelskontor.LyriaHandelskontor;
import org.silvius.lyriahandelskontor.ShopControlPanelMenu;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;

import static org.silvius.lyriahandelskontor.Utils.formatDouble;

public class ShopDeleteButton<P extends Plugin> extends ItemButton<MenuHolder<P>> {
    public ShopDeleteButton() {
        //super((new ItemBuilder(material)).name(new String[]{"+", "-"}[changeType]+changeAmount).lore("Stückpreis: "+price).build());
        super(getShopDeleteItem());

    }

    private static ItemStack getShopDeleteItem() {
        ItemStack itemStack = new ItemStack(Material.BARRIER);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Shop löschen").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.GRAY+"Shift-Linksklick: "+ChatColor.WHITE+"Entfernt den "+ChatColor.getByChar("b")+"Shop."));
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    public final void onClick(MenuHolder<P> holder, InventoryClickEvent event) {
        if(!(event.getClick()== ClickType.SHIFT_LEFT)){return;}
        Player player =(Player) event.getWhoClicked();
        ShopControlPanelMenu<P> shopHolder = (ShopControlPanelMenu<P>) holder;
        Shop shop = shopHolder.shop;
        shop.delete();
        LyriaHandelskontor.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(LyriaHandelskontor.getPlugin(),
                player::closeInventory, 1L);

        player.sendMessage(Component.text(ChatColor.getByChar("b")+"Shop"+ChatColor.WHITE+" erfolgreich entfernt."));
    }

}
