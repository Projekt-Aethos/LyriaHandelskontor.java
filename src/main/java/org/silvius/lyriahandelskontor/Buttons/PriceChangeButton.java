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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static org.silvius.lyriahandelskontor.Utils.formatDouble;

public class PriceChangeButton<P extends Plugin> extends ItemButton<MenuHolder<P>> {
    private final double changeAmount;

    public PriceChangeButton(Material material, double changeAmount, String price) {
        //super((new ItemBuilder(material)).name(new String[]{"+", "-"}[changeType]+changeAmount).lore("Stückpreis: "+price).build());
        super(getPriceChangeItem(price, material, changeAmount));
        this.changeAmount = changeAmount;
    }

    @NotNull
    private static ItemStack getPriceChangeItem(String price, Material material, double changeAmount) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("+/-" + changeAmount).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.BLACK + "(CIT) muenze" + ((int) (Math.floor(Math.log10(changeAmount)) + 3))));
        lore.add(Component.text(ChatColor.GRAY + "Stückpreis: " + ChatColor.GOLD + price));
        lore.add(Component.text(ChatColor.GRAY + "Linksklick/Rechtsklick: " + ChatColor.WHITE + "Erhöht/senkt den " + ChatColor.getByChar("b") + "Preis " + ChatColor.WHITE + "um den gegebenen Wert"));
        lore.add(Component.text(ChatColor.GRAY + "Shift-Klick: " + ChatColor.WHITE + "Ändert den " + ChatColor.getByChar("b") + "Preis " + ChatColor.WHITE + "um 5x den gegebenen Wert"));

        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public final void onClick(MenuHolder<P> holder, InventoryClickEvent event) {
        if (!(event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            return;
        }
        ShopControlPanelMenu<P> shopHolder = (ShopControlPanelMenu<P>) holder;
        Shop shop = shopHolder.shop;
        double currentPrice = shop.getPrice();
        double newPrice;
        int changeType = 0;
        if (event.getClick() == ClickType.LEFT) {
            changeType = 1;
        }
        if (event.getClick() == ClickType.RIGHT) {
            changeType = -1;
        }
        if (event.getClick() == ClickType.SHIFT_LEFT) {
            changeType = 5;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            changeType = -5;
        }
        newPrice = currentPrice + this.changeAmount * changeType;

        if (newPrice < 0.01) newPrice = 0.01;
        String newPriceString = formatDouble(newPrice);
        shop.setPrice(Double.parseDouble(newPriceString));

        SortedMap<Integer, MenuButton<?>> buttons = holder.getButtons();
        for (int i : buttons.keySet()) {
            MenuButton<?> button = buttons.get(i);
            if (!(button instanceof PriceChangeButton)) {
                continue;
            }
            PriceChangeButton<?> priceButton = (PriceChangeButton<?>) button;
            ItemStack itemStack = getPriceChangeItem(newPriceString, priceButton.stack.getType(), priceButton.changeAmount);
            priceButton.setIcon(itemStack);
        }
    }
}
