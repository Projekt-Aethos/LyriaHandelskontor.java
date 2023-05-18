package org.silvius.lyriahandelskontor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.event.PlayerShopClickEvent;
import org.maxgamer.quickshop.api.event.ShopControlPanelOpenEvent;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopType;
import org.silvius.lyriahandelskontor.Buttons.*;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.silvius.lyriahandelskontor.HandelskontorCommand.*;
import static org.silvius.lyriahandelskontor.Utils.formatDouble;

public class ShopInteraction implements Listener {
    private static final String[] permissions = new String[]{"quickshop.other.price", "quickshop.other.control"};
    private static QuickShopAPI api;

    public static boolean isRegisterdHandelskontor(TileState shopSign) {
        NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "registeredHandelskontor");
        PersistentDataContainer persistentDataContainer = shopSign.getPersistentDataContainer();
        boolean registerdHandelskontor = false;
        if (!(persistentDataContainer.has(namespacedKey))) {
            persistentDataContainer.set(namespacedKey, PersistentDataType.INTEGER, 0);
        } else {
            registerdHandelskontor = (persistentDataContainer.get(namespacedKey, PersistentDataType.INTEGER) == 1);
        }
        shopSign.update();
        return registerdHandelskontor;
    }

    public static void fillEmptySlots(MenuHolder<LyriaHandelskontor> menu) {
        for (int i = 0; i < menu.getInventory().getSize(); i++) {
            if (menu.getButton(i) == null) {
                menu.setButton(i, new PlaceHolderButton<>(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }

    @EventHandler
    public void onShopRightClick(ShopControlPanelOpenEvent event) {

        event.setCancelled(true);
        api = LyriaHandelskontor.getQuickshopAPI();
        Player player = (Player) event.getSender();
        Shop shop = event.getShop();
        boolean owner = false;
        if (!(player.getUniqueId() == shop.getOwner() || player.hasPermission("quickshop.other.price") || player.hasPermission("quickshop.other.control"))) {
            return;
        }
        if (player.getUniqueId() == shop.getOwner()) {
            owner = true;
        }
        TileState shopSign = (TileState) Objects.requireNonNull(player.getTargetBlock(5)).getState();
        boolean registerdHandelskontor = isRegisterdHandelskontor(shopSign);
        MenuHolder<LyriaHandelskontor> menu1 = new ShopControlPanelMenu<>(LyriaHandelskontor.getPlugin(), 4 * 9, "Kramladen Editor", shop, shopSign);
        menu1.setButton(4*9-1, new CloseButton<>());
        String price = formatDouble(shop.getPrice());

        if (player.hasPermission("quickshop.other.price") || owner) {
            Material[] materials = new Material[]{Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD};
            Integer[] itemSlots = new Integer[]{40, 39, 38, 37, 36};
            double[] changeAmounts = new double[]{0.01, 0.1, 1, 10, 100};
//            for (int i = 0; i < materials.length; i++) {
//                Material material = materials[i];
//                double changeAmount = changeAmounts[i];
//                menu1.setButton(itemSlots[i], new PriceChangeButton<>(material, changeAmount, 1, price));
//            }

            for (int i = 0; i < materials.length; i++) {
                Material material = materials[i];
                double changeAmount = changeAmounts[i];
                menu1.setButton(itemSlots[i] - 9, new PriceChangeButton<>(material, changeAmount,  price));
            }
        }
        List<Component> message_lore = new ArrayList<>();
        ItemStack messageItem = shop.getItem().clone();
        ItemMeta message_meta = messageItem.getItemMeta();
        if (message_meta.hasLore()) {
            message_lore.addAll(Objects.requireNonNull(message_meta.lore()));
        }
        message_meta.lore(message_lore);


        messageItem.setItemMeta(message_meta);
        menu1.setButton(13, new ItemButton<>(messageItem));
        for (int i : new int[]{3, 4, 5, 12, 14, 21, 22, 23}) {

            menu1.setButton(i, new PlaceHolderButton<>(Material.PURPLE_STAINED_GLASS_PANE));
        }
        if (player.hasPermission("quickshop.other.control") || owner) {
            menu1.setButton(0, new ShopTypeToggleButton<>(shop.getShopType()));
        }

        if (shop.getShopType() == ShopType.SELLING) {
            menu1.setButton(7, new HandelskontorToggleButton<>(registerdHandelskontor));
        }
        if (player.hasPermission("quickshop.other.destroy") || owner) {
            menu1.setButton(2, new ShopDeleteButton<>());
        }


        fillEmptySlots(menu1);
        player.openInventory(menu1.getInventory());
    }

    @EventHandler
    public void onShopLeftClick(PlayerShopClickEvent event) {
        api = LyriaHandelskontor.getQuickshopAPI();
        NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
        NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");

        Shop shop = event.getShop();
        Player player = event.getPlayer();

        ItemStack messageItem = getItemStack(player, namespacedKeyLocation, namespacedKeyWorld, shop, false);
        if (shop.getShopType() == ShopType.SELLING) {
            openRegularBuyGUI(event.getPlayer(), messageItem, 1);
        } else {
            openRegularSellGUI(event.getPlayer(), messageItem, 1);
        }
        event.setCancelled(true);
    }

}
