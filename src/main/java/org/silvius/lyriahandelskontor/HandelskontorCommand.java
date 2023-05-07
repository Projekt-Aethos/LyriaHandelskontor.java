package org.silvius.lyriahandelskontor;


import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopType;

import java.util.*;
import java.util.regex.Pattern;

import static org.silvius.lyriahandelskontor.Utils.formatDouble;

public class HandelskontorCommand implements CommandExecutor, Listener {
    private static final HashMap<UUID, Integer> playerPage = new HashMap<>();

    //1: Nach Namen, 2: Item-Name, 3: Preis, 4: Wie viel da ist
    private static final HashMap<UUID, Integer> playerSort = new HashMap<>();
    private static final HashMap<UUID, Boolean> sortAscending = new HashMap<>();

    private static final HashMap<UUID, String> playerFilter = new HashMap<>();
    private static final ArrayList<UUID> waitingForInput = new ArrayList<>();
    static FileConfiguration config;
    private static QuickShopAPI api;
    public static double tax;


    public HandelskontorCommand() {
        config = LyriaHandelskontor.getPlugin().getConfig();
        tax = config.getDouble("HandelskontorTax");
    }

    private static void changeItemNameAndLoreBuyMenu(ItemStack item, String name, Integer lore, NamespacedKey namespacedKey, int amount, String price) {
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> itemLore = new ArrayList<>();
        itemLore.add(Component.text(ChatColor.GRAY + "Anzahl: " + ChatColor.BLUE + lore).decoration(TextDecoration.ITALIC, false));
        itemLore.add(Component.text(ChatColor.GRAY + "Kosten total: " + ChatColor.GOLD + price).decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(itemLore);
        itemMeta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));

        PersistentDataContainer dataItem = itemMeta.getPersistentDataContainer();
        dataItem.set(namespacedKey, PersistentDataType.INTEGER, amount);

        item.setItemMeta(itemMeta);
    }
    private static void changeItemNameAndLoreSellMenu(ItemStack item, String name, Integer lore, NamespacedKey namespacedKey, int amount, String price) {
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> itemLore = new ArrayList<>();
        itemLore.add(Component.text(ChatColor.GRAY + "Anzahl: " + ChatColor.BLUE + lore).decoration(TextDecoration.ITALIC, false));
        itemLore.add(Component.text(ChatColor.GRAY + "Einnahmen total: " + ChatColor.GOLD + price).decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(itemLore);
        itemMeta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));

        PersistentDataContainer dataItem = itemMeta.getPersistentDataContainer();
        dataItem.set(namespacedKey, PersistentDataType.INTEGER, amount);

        item.setItemMeta(itemMeta);
    }

    private static void openHandelskontorGUI(Player player) {
        Inventory gui = Bukkit.createInventory(new HandelskontorGUIHolder(), 54, Component.text(ChatColor.DARK_GREEN + "Handelskontor"));
        QuickShopAPI api = LyriaHandelskontor.getQuickshopAPI();
        List<Shop> shops = api.getShopManager().getAllShops();

        prepareHandleskontorGUI(player, gui, shops);
        player.openInventory(gui);

    }

    private static void prepareHandleskontorGUI(Player player, Inventory gui, List<Shop> shops) {
        int page = playerPage.get(player.getUniqueId());
        int sortType = playerSort.get(player.getUniqueId());
        String filterString = playerFilter.get(player.getUniqueId());

        ItemStack rand = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta rand_meta = rand.getItemMeta();
        rand_meta.displayName(Component.text(ChatColor.BLACK + "."));
        rand.setItemMeta(rand_meta);

        NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
        NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");


        ItemStack leftArrow = new ItemStack(Material.ARROW); //Kills the player
        ItemMeta leftArrow_meta = leftArrow.getItemMeta();
        leftArrow_meta.displayName(Component.text("Seite zurück"));
        leftArrow.setItemMeta(leftArrow_meta);

        ItemStack rightArrow = new ItemStack(Material.ARROW); //Kills the player
        ItemMeta rightArrow_meta = rightArrow.getItemMeta();
        rightArrow_meta.displayName(Component.text("Seite vor"));
        rightArrow.setItemMeta(rightArrow_meta);

        //1: Nach Namen, 2: Item-Name, 3: Preis, 4: Wie viel da ist
        ItemStack sortHopper = new ItemStack(Material.HOPPER); //Kills the player
        ItemMeta sortHopper_meta = sortHopper.getItemMeta();
        sortHopper_meta.displayName(Component.text("Sortieren").decoration(TextDecoration.ITALIC, false));
        List<Component> sortHopper_lore = new ArrayList<>();
        String[] sortTypes;
        if (sortAscending.get(player.getUniqueId())) {
            sortTypes = new String[]{"Nach Spielername ↑", "Nach Itemname ↑", "Nach Preis ↑", "Nach Lager ↑"};
        } else {
            sortTypes = new String[]{"Nach Spielername ↓", "Nach Itemname ↓", "Nach Preis ↓", "Nach Lager ↓"};
        }
        sortHopper_lore.add(Component.text(sortTypes[0]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
        sortHopper_lore.add(Component.text(sortTypes[1]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
        sortHopper_lore.add(Component.text(sortTypes[2]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
        sortHopper_lore.add(Component.text(sortTypes[3]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
        sortHopper_lore.set(sortType, Component.text("-> " + sortTypes[sortType]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
        sortHopper_meta.lore(sortHopper_lore);
        sortHopper.setItemMeta(sortHopper_meta);


        ItemStack filterSign = new ItemStack(Material.NAME_TAG); //Kills the player
        ItemMeta filterSign_meta = filterSign.getItemMeta();
        filterSign_meta.displayName(Component.text("Nach Text filtern").decoration(TextDecoration.ITALIC, false));
        List<Component> filterSign_lore = new ArrayList<>();
        filterSign_lore.add(Component.text("Hier kannst du die Itemnamen, Lores usw. nach Text filtern.").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
        if (playerFilter.containsKey(player.getUniqueId())) {
            sortHopper_lore.add(Component.text(playerFilter.get(player.getUniqueId())).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
        }
        filterSign_meta.lore(filterSign_lore);
        filterSign.setItemMeta(filterSign_meta);

        ItemStack taxInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) taxInfo.getItemMeta();
        skullMeta.setOwningPlayer(player.getServer().getPlayer(UUID.randomUUID()));
        PlayerProfile skin8c4b8256 = Bukkit.createProfile(UUID.fromString("0f362505-e4f8-4230-979e-1a0cf69674ad"), "skin8c4b8256");
        skin8c4b8256.setProperty(new ProfileProperty("textures", "ewogICJ0aW1lc3RhbXAiIDogMTYzMjkwMzA2OTM5NywKICAicHJvZmlsZUlkIiA6ICI2MjM5ZWRhM2ExY2Y0YjJiYWMyODk2NGQ0NmNlOWVhOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGYXRGYXRHb2QiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTdjNTdlY2M2ZjM0ZmMzNGNkMzUyNGNlMGI3YzFkZDFjNDA1ZjEzMTBlYTI1NDI1ZTcyYzhhNTAyZTk5YWQ1MiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "WQjhjggRDKhclwt85tPRtT7OtdzMdPJUukQqY/jjU5dtQBpIry+AnlVmeqWqAYIOK3a5SvwERlfvzBE7DU0o3k6WS1EAaJQMMfMMkSf5k5mGilzIthuFu2MiOKCMpI8whxYPFri/7wfnWalLvbcZ0UzliKOGWMHIGDkxpyDfqwq+qRRVmjgVOxuGv1T5sZAQwVC0GxoYzF+kL/JiAs8D2zu7a90cyEPAHTF+MoMBnDFcNBaZIgng/bATkZ5ZoypZFn9yGrnk9eSTtkz31pDg5hWLO0vKmq8gMzfdmqHzx0fU3ujemg0MeRfuxXQiyBLfxHbb0HMAZgsSn93f+M3tjNydYiJ2Oy+jNQCaHo8rOe1NWHdEoxEdkp82FFR4mVQncUURKv3OI7T4+7aADSJgJDOUVBtz1fUWoW7fSavNYaujXy3lz6KbdxK/V8CRMv7qViY5yiBgHjQTbGeEMAoygrFx8cyx4hKZJdhyOVWqUs0wZIJk2tGUKSAAuRxP5umlO4Pc26Cqp/aBFo6cmPTvU1HIQkTbQRub/dAJfSwUdZ9idWw1obMEgXqYCCANVBqrwCS7AkGZXhxkDPsZECqVDETkV8Yu/lTyFyE4WWADn4b27X4spCPZLSvvMn0lHITLtqwEEacjc0VbpROG8WS94nhAYd/tk0LcfN64E2QORas="));
        skullMeta.setPlayerProfile(skin8c4b8256);

        skullMeta.displayName(Component.text(ChatColor.GRAY + "Steuer"));
        List<Component> taxLore = new ArrayList<>();
        taxLore.add(Component.text(ChatColor.GRAY + "Auf die Items wird eine Steuer in Höhe von " + ChatColor.BLUE + tax * 100 + "%" + ChatColor.GRAY + " erhoben.").decoration(TextDecoration.ITALIC, false));
        skullMeta.lore(taxLore);
        taxInfo.setItemMeta(skullMeta);

        ItemStack returnItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta returnItemMeta = returnItem.getItemMeta();
        returnItemMeta.displayName(Component.text("Inventar schließen"));
        List<Component> returnItemLore = new ArrayList<>();
        returnItemLore.add(Component.text(ChatColor.BLACK+"(CIT) ablehnen"));
        returnItemMeta.lore(returnItemLore);
        returnItem.setItemMeta(returnItemMeta);


        if (!filterString.equals("")) {
            shops = filterShops(shops, filterString);
        }
        ArrayList<ItemStack> messageItems = new ArrayList<>();
        switch (sortType) {
            case 0:
                shops.sort((a, b) -> a.ownerName().compareToIgnoreCase(b.ownerName()));
                break;
            case 1:
                shops.sort((a, b) -> a.getItem().getType().toString().compareToIgnoreCase(b.getItem().getType().toString()));
                break;
            case 2:
                shops.sort(Comparator.comparingDouble(Shop::getPrice).reversed());
                break;
            case 3:
                shops.sort(Comparator.comparingInt(Shop::getRemainingStock).reversed());
                break;

        }

        if (sortAscending.get(player.getUniqueId())) {
            Collections.reverse(shops);
        }
        for (Shop shop : shops) {
            boolean inHandelskontor = false;
            for (Sign sign : shop.getSigns()) {
                if (shop.isShopSign(sign)) {
                    PersistentDataContainer persistentDataContainer = ((TileState) sign.getBlock().getState()).getPersistentDataContainer();
                    NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "registeredHandelskontor");
                    if (persistentDataContainer.has(namespacedKey)) {
                        boolean registered = persistentDataContainer.get(namespacedKey, PersistentDataType.INTEGER) == 1;
                        if (registered) {
                            inHandelskontor = true;
                        }
                    }
                }
            }
            if (!inHandelskontor) {
                continue;
            }
            if (shop.getShopType() == ShopType.BUYING) {
                continue;
            }
            ItemStack messageItem = getItemStack(player, namespacedKeyLocation, namespacedKeyWorld, shop, true);


            messageItems.add(messageItem);
        }
        ArrayList<ItemStack> menuItems = new ArrayList<>();
        int pages = messageItems.size() / 28;
        if (page < 0) {
            page = 0;
            playerPage.put(player.getUniqueId(), page);
        }
        if (page > pages) {
            page = pages;
            playerPage.put(player.getUniqueId(), page);
        }

        for (int i = 0; i < 54; i++) {
            if (i % 9 == 0 || i % 9 == 8) {

                if (i == 9 * 5) {
                    menuItems.add(leftArrow);
                } else if (i == 9 * 5 + 8) {
                    menuItems.add(rightArrow);
                } else {
                    menuItems.add(rand);
                }
            } else if (i > 53 - 9) {
                if (i % 9 == 4) {
                    menuItems.add(rand);
                } else {
                    menuItems.add(rand);
                }
            } else if (i < 9) {
                menuItems.add(rand);
            } else {
                if (messageItems.size() > page * 28) {
                    menuItems.add(messageItems.get(page * 28));
                    messageItems.remove(page * 28);
                } else {
                    menuItems.add(null);
                }
            }
        }


        menuItems.set(50, sortHopper);
        menuItems.set(51, filterSign);
        menuItems.set(8, returnItem);
        menuItems.set(0, taxInfo);
        ItemStack[] arr = new ItemStack[menuItems.size()];
        menuItems.toArray(arr);
        gui.setContents(arr);
    }

    @NotNull
    protected static ItemStack getItemStack(Player player, NamespacedKey namespacedKeyLocation, NamespacedKey namespacedKeyWorld, Shop shop, boolean fromHandelskontor) {
        List<Component> message_lore = new ArrayList<>();
        ItemStack messageItem = shop.getItem().clone();
        ItemMeta message_meta = messageItem.getItemMeta();
        if (message_meta.hasLore()) {
            message_lore.addAll(Objects.requireNonNull(message_meta.lore()));
        }
        double price = shop.getPrice();
        String seller = shop.ownerName();
        int amount = shop.getRemainingStock();
        Location shopLocation = shop.getLocation();
        int xLocation = shopLocation.getBlockX();
        int zLocation = shopLocation.getBlockZ();
        message_lore.add(Component.text(ChatColor.GRAY + "———————————").decoration(TextDecoration.ITALIC, false));
        if(fromHandelskontor) {
            message_lore.add(Component.text(ChatColor.GRAY + "Verkaufspreis: " + ChatColor.GOLD + formatDouble(price * (1 + tax)) + ChatColor.GOLD + "Đ").decoration(TextDecoration.ITALIC, false));
        } else{
            message_lore.add(Component.text(ChatColor.GRAY + "Verkaufspreis: " + ChatColor.GOLD + formatDouble(price) + ChatColor.GOLD + "Đ").decoration(TextDecoration.ITALIC, false));

        }message_lore.add(Component.text(ChatColor.GRAY + "Verkäufer: " + getRangColor(player) + seller).decoration(TextDecoration.ITALIC, false));
        message_lore.add(Component.text(ChatColor.GRAY + "Auf Lager: " + ChatColor.BLUE + amount).decoration(TextDecoration.ITALIC, false));
        message_lore.add(Component.text(ChatColor.GRAY+"Shopstandort: "+ChatColor.BLUE+"X: "+xLocation+",Z: "+zLocation));
        message_meta.lore(message_lore);


        PersistentDataContainer dataMessage = message_meta.getPersistentDataContainer();
        dataMessage.set(namespacedKeyLocation, PersistentDataType.INTEGER_ARRAY, new int[]{shop.getLocation().getBlockX(), shop.getLocation().getBlockY(), shop.getLocation().getBlockZ()});
        dataMessage.set(namespacedKeyWorld, PersistentDataType.STRING, shop.getLocation().getWorld().getName());


        messageItem.setItemMeta(message_meta);
        return messageItem;
    }

    protected static void openHandelskontorBuyGUI(Player player, ItemStack currentItem, int amount) {

        Inventory gui = Bukkit.createInventory(new BuyGUIHolder(true), 9, Component.text(ChatColor.DARK_GREEN + "Handelskontor"));

        NamespacedKey namespacedKeyAmount = new NamespacedKey(LyriaHandelskontor.getPlugin(), "BuyAmount");
        List<Component> lore = currentItem.getItemMeta().lore();
        assert lore != null;
        Collections.reverse(lore);
        String stockString = ((TextComponent) lore.get(1)).content();
        int stock = Integer.parseInt(stockString.split(" ")[2].substring(2));
        if (amount > stock) amount = stock;
        String priceString = ((TextComponent) lore.get(3)).content();
        double price = Double.parseDouble(priceString.split(" ")[1].substring(2).replace("§6Đ", "")) * amount;

        String priceS = formatDouble(price);


        ItemStack mainItem = currentItem.clone();

        ItemStack add1 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreBuyMenu(add1, "+1", amount, namespacedKeyAmount, amount, priceS);


        ItemStack add10 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreBuyMenu(add10, "+10", amount, namespacedKeyAmount, amount, priceS);


        ItemStack add100 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreBuyMenu(add100, "+100", amount, namespacedKeyAmount, amount, priceS);

        ItemStack subtract1 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreBuyMenu(subtract1, "-1", amount, namespacedKeyAmount, amount, priceS);


        ItemStack subtract10 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreBuyMenu(subtract10, "-10", amount, namespacedKeyAmount, amount, priceS);


        ItemStack subtract100 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreBuyMenu(subtract100, "-100", amount, namespacedKeyAmount, amount, priceS);

        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta= confirm.getItemMeta();
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text(ChatColor.WHITE+"Weiter...").decoration(TextDecoration.ITALIC, false));
        confirmMeta.displayName(Component.text(ChatColor.GREEN+"JA!").decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);

        ItemStack returnItem = new ItemStack(Material.RED_WOOL);
        ItemMeta returnItemMeta= confirm.getItemMeta();
        List<Component> returnItemLore = new ArrayList<>();
        returnItemLore.add(Component.text(ChatColor.WHITE+"Zurück zum Handelskontor").decoration(TextDecoration.ITALIC, false));
        returnItemLore.add(Component.text(ChatColor.BLACK+"Lieber nicht einpacken!").decoration(TextDecoration.ITALIC, false));
        returnItemMeta.displayName(Component.text(ChatColor.RED+"ABBRECHEN").decoration(TextDecoration.ITALIC, false));
        returnItemMeta.lore(returnItemLore);
        returnItem.setItemMeta(returnItemMeta);



        gui.setContents(new ItemStack[]{returnItem, subtract100, subtract10, subtract1, mainItem, add1, add10, add100, confirm});
        player.openInventory(gui);


    }
    protected static void openRegularBuyGUI(Player player, ItemStack currentItem, int amount) {

        Inventory gui = Bukkit.createInventory(new BuyGUIHolder(false), 9, Component.text(ChatColor.DARK_GREEN + "Handelskontor"));

        NamespacedKey namespacedKeyAmount = new NamespacedKey(LyriaHandelskontor.getPlugin(), "BuyAmount");
        List<Component> lore = currentItem.getItemMeta().lore();
        assert lore != null;
        Collections.reverse(lore);
        String stockString = ((TextComponent) lore.get(1)).content();
        int stock = Integer.parseInt(stockString.split(" ")[2].substring(2));
        if (amount > stock) amount = stock;
        String priceString = ((TextComponent) lore.get(3)).content();
        double price = Double.parseDouble(priceString.split(" ")[1].substring(2).replace("§6Đ", "")) * amount;

        String priceS = formatDouble(price);


        ItemStack mainItem = currentItem.clone();

        ItemStack add1 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreBuyMenu(add1, "+1", amount, namespacedKeyAmount, amount, priceS);


        ItemStack add10 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreBuyMenu(add10, "+10", amount, namespacedKeyAmount, amount, priceS);


        ItemStack add100 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreBuyMenu(add100, "+100", amount, namespacedKeyAmount, amount, priceS);

        ItemStack subtract1 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreBuyMenu(subtract1, "-1", amount, namespacedKeyAmount, amount, priceS);


        ItemStack subtract10 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreBuyMenu(subtract10, "-10", amount, namespacedKeyAmount, amount, priceS);


        ItemStack subtract100 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreBuyMenu(subtract100, "-100", amount, namespacedKeyAmount, amount, priceS);

        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta= confirm.getItemMeta();
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text(ChatColor.WHITE+"Weiter...").decoration(TextDecoration.ITALIC, false));
        confirmMeta.displayName(Component.text(ChatColor.GREEN+"JA!").decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);

        ItemStack returnItem = new ItemStack(Material.RED_WOOL);
        ItemMeta returnItemMeta= confirm.getItemMeta();
        List<Component> returnItemLore = new ArrayList<>();
        returnItemLore.add(Component.text(ChatColor.WHITE+"Menü schließen").decoration(TextDecoration.ITALIC, false));
        returnItemLore.add(Component.text(ChatColor.BLACK+"Lieber nicht einpacken!").decoration(TextDecoration.ITALIC, false));
        returnItemMeta.displayName(Component.text(ChatColor.RED+"ABBRECHEN").decoration(TextDecoration.ITALIC, false));
        returnItemMeta.lore(returnItemLore);
        returnItem.setItemMeta(returnItemMeta);



        gui.setContents(new ItemStack[]{returnItem, subtract100, subtract10, subtract1, mainItem, add1, add10, add100, confirm});
        player.openInventory(gui);


    }
    protected static void openRegularSellGUI(Player player, ItemStack currentItem, int amount) {

        Inventory gui = Bukkit.createInventory(new SellGUIHolder(), 9, Component.text(ChatColor.DARK_GREEN + "Handelskontor"));

        NamespacedKey namespacedKeyAmount = new NamespacedKey(LyriaHandelskontor.getPlugin(), "SellAmount");
        List<Component> lore = currentItem.getItemMeta().lore();
        assert lore != null;
        Collections.reverse(lore);
        String stockString = ((TextComponent) lore.get(1)).content();
        int stock = Integer.parseInt(stockString.split(" ")[2].substring(2));
        if (amount > stock) amount = stock;
        String priceString = ((TextComponent) lore.get(3)).content();
        double price = Double.parseDouble(priceString.split(" ")[1].substring(2).replace("§6Đ", "")) * amount;

        String priceS = formatDouble(price);


        ItemStack mainItem = currentItem.clone();

        ItemStack add1 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreSellMenu(add1, "+1", amount, namespacedKeyAmount, amount, priceS);


        ItemStack add10 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreSellMenu(add10, "+10", amount, namespacedKeyAmount, amount, priceS);


        ItemStack add100 = new ItemStack(Material.GREEN_WOOL);
        changeItemNameAndLoreSellMenu(add100, "+100", amount, namespacedKeyAmount, amount, priceS);

        ItemStack subtract1 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreSellMenu(subtract1, "-1", amount, namespacedKeyAmount, amount, priceS);


        ItemStack subtract10 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreSellMenu(subtract10, "-10", amount, namespacedKeyAmount, amount, priceS);


        ItemStack subtract100 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLoreSellMenu(subtract100, "-100", amount, namespacedKeyAmount, amount, priceS);

        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta= confirm.getItemMeta();
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text(ChatColor.WHITE+"Weiter...").decoration(TextDecoration.ITALIC, false));
        confirmMeta.displayName(Component.text(ChatColor.GREEN+"JA!").decoration(TextDecoration.ITALIC, false));
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);

        ItemStack returnItem = new ItemStack(Material.RED_WOOL);
        ItemMeta returnItemMeta= confirm.getItemMeta();
        List<Component> returnItemLore = new ArrayList<>();
        returnItemLore.add(Component.text(ChatColor.WHITE+"Menü schließen").decoration(TextDecoration.ITALIC, false));
        returnItemLore.add(Component.text(ChatColor.BLACK+"Lieber nicht einpacken!").decoration(TextDecoration.ITALIC, false));
        returnItemMeta.displayName(Component.text(ChatColor.RED+"ABBRECHEN").decoration(TextDecoration.ITALIC, false));
        returnItemMeta.lore(returnItemLore);
        returnItem.setItemMeta(returnItemMeta);



        gui.setContents(new ItemStack[]{returnItem, subtract100, subtract10, subtract1, mainItem, add1, add10, add100, confirm});
        player.openInventory(gui);


    }

    private static ArrayList<Shop> filterShops(List<Shop> shops, String filter) {
        ArrayList<Shop> filteredShops = new ArrayList<>();
        for (Shop shop : shops) {
            if (filteredShops.contains(shop)) {
                continue;
            }
            ItemStack item = shop.getItem();
            String materialString = item.getType().toString();
            String customNameString = null;
            if (item.getItemMeta().hasDisplayName()) {
                customNameString = ((TextComponent) Objects.requireNonNull(item.getItemMeta().displayName())).content();
            }
            if (Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(materialString).find() || customNameString != null && Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(customNameString).find() || Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(item.getItemMeta().toString()).find()) {
                filteredShops.add(shop);
            }

        }
        return filteredShops;
    }

    public static boolean isPlayerInGroup(Player player, String group) {
        return player.hasPermission("group." + group);
    }

    public static ChatColor getRangColor(Player player) {
        final HashMap<String, ChatColor> rangcolors = new HashMap<>();
        ChatColor playerColor = ChatColor.GRAY;
        String[] ranks = new String[]{"noob", "member", "vip", "vip2", "vip3", "barde", "helfer", "supporter", "mod", "admins"};
        rangcolors.put(ranks[0], ChatColor.GREEN);
        rangcolors.put(ranks[1], ChatColor.DARK_GREEN);
        rangcolors.put(ranks[2], ChatColor.getByChar("9"));
        rangcolors.put(ranks[3], ChatColor.getByChar("3"));
        rangcolors.put(ranks[4], ChatColor.getByChar("b"));
        rangcolors.put(ranks[5], ChatColor.getByChar("d"));
        rangcolors.put(ranks[6], ChatColor.getByChar("5"));
        rangcolors.put(ranks[7], ChatColor.getByChar("e"));
        rangcolors.put(ranks[8], ChatColor.getByChar("6"));
        rangcolors.put(ranks[9], ChatColor.getByChar("4"));


        for (String s : ranks) {
            if (isPlayerInGroup(player, s)) {
                playerColor = rangcolors.get(s);
            }
        }

        return playerColor;
    }

    @EventHandler
    public void clickEvent(InventoryClickEvent event) {
        //Check to see if its the GUI menu
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().getHolder() == null) {
            return;
        }
        if (event.getClickedInventory().getHolder() instanceof HandelskontorGUIHolder) {
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem() == null) {
                return;
            }
            //Determine what they selected and what to do
            if (event.getSlot()==53 || event.getSlot()==53-8) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                TextComponent name = (TextComponent) meta.displayName();
                assert name != null;
                int currentPage = playerPage.get(player.getUniqueId());
                if (name.content().equals("Seite zurück")) {
                    playerPage.put(player.getUniqueId(), currentPage - 1);
                    openHandelskontorGUI(player);
                } else {
                    playerPage.put(player.getUniqueId(), currentPage + 1);
                    openHandelskontorGUI(player);

                }
            } else if (event.getSlot()==50) {
                if (event.isLeftClick()) {
                    int currentSort = playerSort.get(player.getUniqueId());
                    playerSort.put(player.getUniqueId(), (currentSort + 1) % 4);
                } else {
                    boolean currentSortType = sortAscending.get(player.getUniqueId());
                    sortAscending.put(player.getUniqueId(), !currentSortType);
                }
                updateHandelskontorGUI(player);

            } else if (event.getSlot()==51) {
                ItemStack itemLeft = new ItemStack(Material.NAME_TAG);
                ItemMeta itemMeta = itemLeft.getItemMeta();
                itemMeta.displayName(Component.text(""));
                itemLeft.setItemMeta(itemMeta);

                new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> {
                                    playerFilter.put(player.getUniqueId(), stateSnapshot.getText());
                                    openHandelskontorGUI(stateSnapshot.getPlayer());
                                                                    })
                        );
                })
                        .interactableSlots(AnvilGUI.Slot.OUTPUT)
                        .itemLeft(itemLeft)
                        .plugin(LyriaHandelskontor.getPlugin())
                        .open(player);

            } else if (event.getSlot() % 9 != 0 && event.getSlot() % 9 != 8 && event.getSlot() / 9 != 0 && event.getSlot() / 9 != 5) {
                openHandelskontorBuyGUI(player, event.getCurrentItem(), 1);
            }
            else if(event.getSlot()==8){
                player.closeInventory();
            }


            event.setCancelled(true); //So they cant take the items
        }
        if (event.getClickedInventory().getHolder() instanceof BuyGUIHolder) {
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem() == null) {
                return;
            }
            //Determine what they selected and what to do
            NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "BuyAmount");
            BuyGUIHolder holder = (BuyGUIHolder) event.getClickedInventory().getHolder();
            if (event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(namespacedKey)) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                TextComponent name = (TextComponent) meta.displayName();
                assert name != null;
                String nameString = name.content();

                int change = 1;
                if (nameString.contains("-")) change = -1;
                String[] changeAmount;
                if (change == 1) {
                    changeAmount = nameString.split("\\+");
                } else {
                    changeAmount = nameString.split("-");
                }
                int changedAmount = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER) + change * Integer.parseInt(changeAmount[1]);
                if (changedAmount < 1) {
                    changedAmount = 1;
                }
                if(holder.fromHandelskontor){
                openHandelskontorBuyGUI(player, Objects.requireNonNull(event.getClickedInventory().getItem(4)), changedAmount);}
                else{
                    openRegularBuyGUI(player, Objects.requireNonNull(event.getClickedInventory().getItem(4)), changedAmount);}

            }
            else if(event.getSlot()==0){
                if(!playerPage.containsKey(player.getUniqueId())){
                playerPage.put(player.getUniqueId(), 0);
                playerSort.put(player.getUniqueId(), 0);
                playerFilter.put(player.getUniqueId(), "");
                sortAscending.put(player.getUniqueId(), false);}
                if(holder.fromHandelskontor){
                openHandelskontorGUI(player);}
                else{
                    player.closeInventory();
                }
            }
            else {
                ItemMeta meta = Objects.requireNonNull(event.getClickedInventory().getItem(3)).getItemMeta();
                int amount = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
                buyItem(player, Objects.requireNonNull(event.getClickedInventory().getItem(4)), amount, holder.fromHandelskontor);

            }


            event.setCancelled(true); //So they cant take the items
        }
        if (event.getClickedInventory().getHolder() instanceof SellGUIHolder) {
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem() == null) {
                return;
            }
            //Determine what they selected and what to do
            NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "SellAmount");
            if (event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(namespacedKey)) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                TextComponent name = (TextComponent) meta.displayName();
                assert name != null;
                String nameString = name.content();

                int change = 1;
                if (nameString.contains("-")) change = -1;
                String[] changeAmount;
                if (change == 1) {
                    changeAmount = nameString.split("\\+");
                } else {
                    changeAmount = nameString.split("-");
                }
                int changedAmount = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER) + change * Integer.parseInt(changeAmount[1]);
                if (changedAmount < 1) {
                    changedAmount = 1;
                }
                openRegularSellGUI(player, Objects.requireNonNull(event.getClickedInventory().getItem(4)), changedAmount);
            }
            else if(event.getSlot()==0){
                    player.closeInventory();

            }
            else {
                ItemMeta meta = Objects.requireNonNull(event.getClickedInventory().getItem(3)).getItemMeta();
                int amount = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
                sellItem(player, Objects.requireNonNull(event.getClickedInventory().getItem(4)), amount);

            }


            event.setCancelled(true); //So they cant take the items
        }



    }

    private void updateHandelskontorGUI(Player player) {
        Inventory gui = player.getOpenInventory().getTopInventory();


        List<Shop> shops = api.getShopManager().getAllShops();
        prepareHandleskontorGUI(player, gui, shops);

    }

    @EventHandler
    public void onMessage(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (waitingForInput.contains(player.getUniqueId())) {
            event.setCancelled(true);
            TextComponent message = (TextComponent) event.message();
            waitingForInput.remove(player.getUniqueId());
            player.sendMessage(message);
            playerFilter.put(player.getUniqueId(), message.content());
            LyriaHandelskontor.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(LyriaHandelskontor.getPlugin(), () ->
                    openHandelskontorGUI(player), 1L);


        }
    }

    private void buyItem(Player player, ItemStack currentItem, int amount, boolean fromHandelskontor) {
        QuickShopAPI api = LyriaHandelskontor.getQuickshopAPI();
        NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
        NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");

        ItemMeta currentItemMeta = currentItem.getItemMeta();
        int[] coordinates = currentItemMeta.getPersistentDataContainer().get(namespacedKeyLocation, PersistentDataType.INTEGER_ARRAY);
        String world = currentItemMeta.getPersistentDataContainer().get(namespacedKeyWorld, PersistentDataType.STRING);

        assert coordinates != null;
        assert world != null;
        Location shopLocation = new Location(player.getServer().getWorld(world), coordinates[0], coordinates[1], coordinates[2]);
        Shop shop = api.getShopManager().getShop(shopLocation);
        assert shop != null;
        double price;
        if(fromHandelskontor){
        price = shop.getPrice() * (1 + tax);}
        else{price = shop.getPrice();}

        UUID ownerUUID = shop.getOwner();
        double bal = LyriaHandelskontor.getEconomy().getBalance(player);
        if (amount == 0) {
            player.sendMessage(ChatColor.RED + "Der Shop hat keine Items");
            return;
        }
        if (amount > shop.getRemainingStock()) {
            amount = shop.getRemainingStock();
        }
        if (amount * price > bal) {
            amount = (int) (bal / price);
        }
        if (amount == 0) {
            player.sendMessage(ChatColor.RED + "Du hast zu wenig Geld");
            return;
        }
        double total = amount * price;
        LyriaHandelskontor.getEconomy().withdrawPlayer(player, total);
        LyriaHandelskontor.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(ownerUUID), total);

        //LyriaHandelskontor.getEconomy().getBalance()
        shop.sell(player.getUniqueId(), player.getInventory(), player.getLocation(), amount);
        api.getShopManager().sendSellSuccess(ownerUUID, shop, amount * currentItem.getAmount(), total);
        api.getShopManager().sendPurchaseSuccess(player.getUniqueId(), shop, amount, total);
    }
    private void sellItem(Player player, ItemStack currentItem, int amount) {
        QuickShopAPI api = LyriaHandelskontor.getQuickshopAPI();
        NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
        NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");
        ItemMeta currentItemMeta = currentItem.getItemMeta();
        int[] coordinates = currentItemMeta.getPersistentDataContainer().get(namespacedKeyLocation, PersistentDataType.INTEGER_ARRAY);
        String world = currentItemMeta.getPersistentDataContainer().get(namespacedKeyWorld, PersistentDataType.STRING);

        assert coordinates != null;
        assert world != null;
        Location shopLocation = new Location(player.getServer().getWorld(world), coordinates[0], coordinates[1], coordinates[2]);
        Shop shop = api.getShopManager().getShop(shopLocation);
        assert shop != null;
        double price = shop.getPrice();

        UUID ownerUUID = shop.getOwner();
        double bal = LyriaHandelskontor.getEconomy().getBalance(Bukkit.getOfflinePlayer(shop.getOwner()));
        int playerItemAmount = 0;
        for(ItemStack itemStack: player.getInventory().getContents()){
            if(itemStack==null){continue;}
            if(itemStack.isSimilar(shop.getItem())){
                playerItemAmount+=itemStack.getAmount();
            }
        }
        player.sendMessage(String.valueOf(playerItemAmount));
        if (amount > playerItemAmount) {
            amount = playerItemAmount;
        }
        if (amount == 0) {
            player.sendMessage(ChatColor.RED + "Du hast keine passenden Items");
            return;
        }
        if (amount * price > bal) {
            amount = (int) (bal / price);
        }
        if (amount == 0) {
            player.sendMessage(ChatColor.RED + "Der Shopbesitzer hat zu wenig Geld");
            return;
        }
        double total = amount * price;
        LyriaHandelskontor.getEconomy().depositPlayer(player, total);
        LyriaHandelskontor.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(ownerUUID), total);

        //LyriaHandelskontor.getEconomy().getBalance()
        shop.buy(player.getUniqueId(), player.getInventory(), player.getLocation(), amount);
        api.getShopManager().sendSellSuccess(player.getUniqueId(), shop, amount * currentItem.getAmount(), total);
        api.getShopManager().sendPurchaseSuccess(ownerUUID, shop, amount, total);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!commandSender.hasPermission("lyriahandelskontor.handelskontor")) {
            commandSender.sendMessage(ChatColor.RED + "Keine Berechtigung");
            return true;
        }
        if (strings.length == 0) {
            commandSender.sendMessage(ChatColor.RED + "Fehlende Argumente");
            return true;
        }
        if (strings.length == 2) {
            commandSender.sendMessage(ChatColor.RED + "Zu viele Argumente");
            return true;
        }
        String playername = strings[0];
        Player player = commandSender.getServer().getPlayer(playername);
        if (player == null) {
            commandSender.sendMessage(ChatColor.RED + "Der Spieler existiert nicht");
            return true;
        }
        api = LyriaHandelskontor.getQuickshopAPI();

        playerPage.put(player.getUniqueId(), 0);
        playerSort.put(player.getUniqueId(), 0);
        playerFilter.put(player.getUniqueId(), "");
        sortAscending.put(player.getUniqueId(), false);
        openHandelskontorGUI(player);

        return false;
    }

    public static class HandelskontorGUIHolder implements InventoryHolder {

        public HandelskontorGUIHolder() {
        }

        @Override
        @Nullable
        public Inventory getInventory() {
            return null;
        }
    }

    public static class BuyGUIHolder implements InventoryHolder {
        boolean fromHandelskontor;

        public BuyGUIHolder(boolean fromHandelskontor) {
            this.fromHandelskontor = fromHandelskontor;
        }

        @Override
        @Nullable
        public Inventory getInventory() {
            return null;
        }
    }

    public static class SellGUIHolder implements InventoryHolder {

        public SellGUIHolder() {

        }

        @Override
        @Nullable
        public Inventory getInventory() {
            return null;
        }
    }

}
