package org.silvius.lyriahandelskontor.Buttons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopType;
import org.silvius.lyriahandelskontor.ShopControlPanelMenu;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static org.silvius.lyriahandelskontor.ShopInteraction.isRegisterdHandelskontor;

public class ShopTypeToggleButton<P extends Plugin> extends ItemButton<MenuHolder<P>> {
    public ShopTypeToggleButton(ShopType shopType) {
        super(getShopTypeToggleItem(shopType));
    }

    @NotNull
    private static ItemStack getShopTypeToggleItem(ShopType shopType) {
        ItemStack itemStack = new ItemStack(Material.EMERALD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if(shopType==ShopType.BUYING){
        itemMeta.displayName(Component.text("Shoptyp: "+ ChatColor.GREEN+"Ankauf").decoration(TextDecoration.ITALIC, false));}
        else{
            itemMeta.displayName(Component.text("Shoptyp: "+ChatColor.RED+"Verkauf").decoration(TextDecoration.ITALIC, false));
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.GRAY+"Linksklick: "+ChatColor.WHITE+"Ändert den "+ChatColor.getByChar("b")+"Shoptyp"));
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public final void onClick(MenuHolder<P> holder, InventoryClickEvent event) {
        if(!(event.getClick()== ClickType.LEFT)){return;}
        ShopControlPanelMenu<P> shopHolder = (ShopControlPanelMenu<P>) holder;

        Shop shop = shopHolder.shop;
        ShopType shopType = shop.getShopType();
        if(shopType == ShopType.BUYING){
            shopType = ShopType.SELLING;
            shopHolder.setButton(7, new HandelskontorToggleButton<>(isRegisterdHandelskontor(shopHolder.sign)));
        }
        else {
            shopType = ShopType.BUYING;
            shopHolder.setButton(7, new PlaceHolderButton<>(Material.BLACK_STAINED_GLASS_PANE));
        }
        shop.setShopType(shopType);
        SortedMap<Integer, MenuButton<?>> buttons = holder.getButtons();
        for(int i: buttons.keySet()){
            MenuButton<?> button =  buttons.get(i);
            if(!(button instanceof ShopTypeToggleButton)){continue;}
            ShopTypeToggleButton<?> shopTypeButton = (ShopTypeToggleButton<?>) button;
            ItemStack itemStack = getShopTypeToggleItem(shopType);
            shopTypeButton.setIcon(itemStack);
        }
    }



}
