package org.silvius.lyriahandelskontor.Buttons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.silvius.lyriahandelskontor.ShopControlPanelMenu;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static org.silvius.lyriahandelskontor.Utils.formatDouble;

public class PriceChangeButton<P extends Plugin> extends ItemButton<MenuHolder<P>> {
    private final double changeAmount;
    private final int changeType;

    public PriceChangeButton(Material material, double changeAmount, int changeType, String price) {
        //super((new ItemBuilder(material)).name(new String[]{"+", "-"}[changeType]+changeAmount).lore("Stückpreis: "+price).build());
        super(getPriceChangeItem(price, material, changeAmount, new String[]{"+", "-"}[changeType]));
        this.changeAmount = changeAmount;
        this.changeType = changeType;
    }

    @NotNull
    private static ItemStack getPriceChangeItem(String price, Material material, double changeAmount, String changeTypeString) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if(Objects.equals(changeTypeString, "+")){
        itemMeta.displayName(Component.text(changeTypeString + changeAmount).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GREEN));}
        if(Objects.equals(changeTypeString, "-")){
            itemMeta.displayName(Component.text(changeTypeString + changeAmount).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED));}
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.GRAY+"Stückpreis: "+ ChatColor.GOLD+price));
        lore.add(Component.text(ChatColor.GRAY+"Linksklick: "+ChatColor.WHITE+"Ändert den "+ChatColor.getByChar("b")+"Preis "+ChatColor.WHITE+"um den gegebenen Wert"));
        lore.add(Component.text(ChatColor.GRAY+"Shift-Linksklick: "+ChatColor.WHITE+"Ändert den "+ChatColor.getByChar("b")+"Preis "+ChatColor.WHITE+"um 5x den gegebenen Wert"));

        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public final void onClick(MenuHolder<P> holder, InventoryClickEvent event) {
        if(!(event.getClick()== ClickType.LEFT || event.getClick()== ClickType.SHIFT_LEFT)){return;}
        ShopControlPanelMenu<P> shopHolder = (ShopControlPanelMenu<P>) holder;
        Shop shop = shopHolder.shop;
        double currentPrice = shop.getPrice();
        double newPrice;
        if(event.getClick()== ClickType.SHIFT_LEFT){newPrice = currentPrice-5*this.changeAmount*(this.changeType*2-1);}
        else{newPrice = currentPrice-this.changeAmount*(this.changeType*2-1);}

        if(newPrice<0.01) newPrice=0.01;
        String newPriceString = formatDouble(newPrice);
        shop.setPrice(Double.parseDouble(newPriceString));

        SortedMap<Integer, MenuButton<?>> buttons = holder.getButtons();
        for(int i: buttons.keySet()){
            MenuButton<?> button =  buttons.get(i);
            if(!(button instanceof PriceChangeButton)){continue;}
            PriceChangeButton<?> priceButton = (PriceChangeButton<?>) button;
            ItemStack itemStack = getPriceChangeItem(newPriceString, priceButton.stack.getType(), priceButton.changeAmount, new String[]{"+", "-"}[priceButton.changeType]);
            priceButton.setIcon(itemStack);
        }
    }
}
