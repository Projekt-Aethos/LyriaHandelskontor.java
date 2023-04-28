package org.silvius.lyriahandelskontor;


import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.event.ShopCreateEvent;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopType;

import java.util.*;
import java.util.regex.Pattern;

public class HandelskontorCommand implements CommandExecutor, Listener {
    private static final HashMap<UUID, Integer> playerPage = new HashMap<>();

    //1: Nach Namen, 2: Item-Name, 3: Preis, 4: Wie viel da ist
    private static final HashMap<UUID, Integer> playerSort = new HashMap<>();
    private static final HashMap<UUID, String> playerFilter = new HashMap<>();
    private static final ArrayList<UUID> waitingForInput = new ArrayList<>();


    public HandelskontorCommand(){

    }

    private void changeItemNameAndLore(ItemStack item, Component name, Component lore, NamespacedKey namespacedKey, int amount){
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> itemLore = new ArrayList<>();
        itemLore.add(lore);
        itemMeta.lore(itemLore);
        itemMeta.displayName(name);

        PersistentDataContainer dataItem = itemMeta.getPersistentDataContainer();
        dataItem.set(namespacedKey, PersistentDataType.INTEGER, amount);

        item.setItemMeta(itemMeta);
    }


    @EventHandler
    public void clickEvent(InventoryClickEvent event) {
        //Check to see if its the GUI menu
        if(event.getClickedInventory()==null){return;}
        if (event.getClickedInventory().getHolder() == null){return;}
        if (event.getClickedInventory().getHolder() instanceof HandelskontorGUIHolder) {
            Player player = (Player) event.getWhoClicked();
            if(event.getCurrentItem()==null){return;}
            //Determine what they selected and what to do
            if (event.getCurrentItem().getType() == Material.ARROW) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                TextComponent name = (TextComponent) meta.displayName();
                assert name != null;
                int currentPage = playerPage.get(player.getUniqueId());
                if (name.content().equals("Seite zurück")) {
                    playerPage.put(player.getUniqueId(), currentPage-1);
                    openHandelskontorGUI(player);
                } else {
                    playerPage.put(player.getUniqueId(), currentPage+1);
                    openHandelskontorGUI(player);

                }
            }
            else if(event.getCurrentItem().getType()==Material.HOPPER){
                int currentSort = playerSort.get(player.getUniqueId());
                playerSort.put(player.getUniqueId(), (currentSort+1)%4);
                openHandelskontorGUI(player);

            }

            else if(event.getCurrentItem().getType()==Material.OAK_SIGN){
                player.closeInventory();
                waitingForInput.add(player.getUniqueId());
                player.sendMessage("Filter eingeben:");
            }
            else if (event.getSlot()%9!=0 && event.getSlot()%9!=8 && event.getSlot()/9!=0 && event.getSlot()/9!=5){
                openBuyGUI(player, event.getCurrentItem(), 1);
            }


            event.setCancelled(true); //So they cant take the items
        }

        if (event.getClickedInventory().getHolder() instanceof BuyGUIHolder) {
            Player player = (Player) event.getWhoClicked();
            if(event.getCurrentItem()==null){return;}
            //Determine what they selected and what to do
            if (event.getCurrentItem().getType() == Material.RED_WOOL) {
                NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "BuyAmount");
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                TextComponent name = (TextComponent) meta.displayName();
                assert name != null;
                String nameString= name.content();

                int change = 1;
                if(nameString.contains("-")) change=-1;

                String[] changeAmount = nameString.split("\\+");

                int changedAmount = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER)+change*Integer.parseInt(changeAmount[1]);
                if(changedAmount < 0){changedAmount = 0;}
                openBuyGUI(player, Objects.requireNonNull(event.getClickedInventory().getItem(4)),changedAmount);
            }
            else {
                NamespacedKey namespacedKey = new NamespacedKey(LyriaHandelskontor.getPlugin(), "BuyAmount");
                ItemMeta meta = Objects.requireNonNull(event.getClickedInventory().getItem(3)).getItemMeta();
                int amount = meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
                buyItem(player, event.getCurrentItem(), amount);

            }



            event.setCancelled(true); //So they cant take the items
        }

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


    private void buyItem(Player player, ItemStack currentItem, int amount) {
        QuickShopAPI api = LyriaHandelskontor.getQuickshopAPI();
        NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
        NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");

        ItemMeta currentItemMeta = currentItem.getItemMeta();
        int[] coordinates = currentItemMeta.getPersistentDataContainer().get(namespacedKeyLocation, PersistentDataType.INTEGER_ARRAY);
        String world = currentItemMeta.getPersistentDataContainer().get(namespacedKeyWorld, PersistentDataType.STRING);

        assert coordinates != null;
        assert world != null;
        Location shopLocation = new Location(player.getServer().getWorld(world), coordinates[0],  coordinates[1],  coordinates[2]);
        Shop shop = api.getShopManager().getShop(shopLocation);
        assert shop != null;
        UUID ownerUUID = shop.getOwner();
        double bal = LyriaHandelskontor.getEconomy().getBalance(player);
        if(amount==0){
            player.sendMessage(ChatColor.RED+"Der Shop hat keine Items");
        }
        if(amount> shop.getRemainingStock()){amount = shop.getRemainingStock();}
        if(amount*shop.getPrice()>bal){
            amount = (int) (bal/shop.getPrice());
        }
        if(amount==0){
            player.sendMessage(ChatColor.RED+"Du hast zu wenig Geld");
        }
        double total = amount*shop.getPrice();
        LyriaHandelskontor.getEconomy().withdrawPlayer(player, total);
        LyriaHandelskontor.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(ownerUUID), total);

        //LyriaHandelskontor.getEconomy().getBalance()
        shop.sell(player.getUniqueId(), player.getInventory(), player.getLocation(), amount);
        api.getShopManager().sendSellSuccess(ownerUUID, shop, amount, total);
        api.getShopManager().sendPurchaseSuccess(ownerUUID, shop, amount, total);
    }

    private void openBuyGUI(Player player, ItemStack currentItem, int amount) {
        NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
        NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");

        Inventory gui = Bukkit.createInventory(new BuyGUIHolder(), 9, Component.text(ChatColor.DARK_GREEN + "Handelskontor" ));

        NamespacedKey namespacedKeyAmount = new NamespacedKey(LyriaHandelskontor.getPlugin(), "BuyAmount");


        ItemStack mainItem = currentItem.clone();

        ItemStack add1 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLore(add1, Component.text("+1"), Component.text(amount), namespacedKeyAmount, amount);


        ItemStack add10 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLore(add10, Component.text("+10"), Component.text(amount), namespacedKeyAmount, amount);


        ItemStack add100 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLore(add100, Component.text("+100"), Component.text(amount), namespacedKeyAmount, amount);

        ItemStack subtract1 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLore(subtract1, Component.text("-1"), Component.text(amount), namespacedKeyAmount, amount);


        ItemStack subtract10 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLore(subtract10, Component.text("-10"), Component.text(amount), namespacedKeyAmount, amount);


        ItemStack subtract100 = new ItemStack(Material.RED_WOOL);
        changeItemNameAndLore(subtract100, Component.text("-100"), Component.text(amount), namespacedKeyAmount, amount);

        gui.setContents(new ItemStack[]{null, subtract100, subtract10, subtract1, mainItem, add1, add10, add100, null});
        player.openInventory(gui);



    }

    private static void openHandelskontorGUI(Player player){
            int page = playerPage.get(player.getUniqueId());
            int sortType = playerSort.get(player.getUniqueId());
            String filterString = playerFilter.get(player.getUniqueId());

            Inventory gui = Bukkit.createInventory(new HandelskontorGUIHolder(), 54, Component.text(ChatColor.DARK_GREEN + "Handelskontor" ));
            ItemStack rand = new ItemStack(Material.GRAY_STAINED_GLASS_PANE); //Kills the player
            ItemMeta rand_meta = rand.getItemMeta();
            rand_meta.displayName(Component.text(ChatColor.BLACK + "."));
            rand.setItemMeta(rand_meta);

            NamespacedKey namespacedKeyLocation = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopLocation");
            NamespacedKey namespacedKeyWorld = new NamespacedKey(LyriaHandelskontor.getPlugin(), "shopWorld");


            ItemStack leftArrow = new ItemStack(Material.BLUE_STAINED_GLASS_PANE); //Kills the player
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
            sortHopper_meta.displayName(Component.text("Sortieren"));
            List<Component> sortHopper_lore = new ArrayList<>();
            String[] sortTypes = new String[]{"Nach Spielername", "Nach Itemname", "Nach Preis", "Nach Lager"};
            sortHopper_lore.add(Component.text(sortTypes[0]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            sortHopper_lore.add(Component.text(sortTypes[1]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            sortHopper_lore.add(Component.text(sortTypes[2]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            sortHopper_lore.add(Component.text(sortTypes[3]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            sortHopper_lore.set(sortType, Component.text("-> "+sortTypes[sortType]).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
            sortHopper_meta.lore(sortHopper_lore);
            sortHopper.setItemMeta(sortHopper_meta);


            ItemStack filterSign = new ItemStack(Material.OAK_SIGN); //Kills the player
            ItemMeta filterSign_meta = filterSign.getItemMeta();
            filterSign_meta.displayName(Component.text("Nach Text filtern"));
            List<Component> filterSign_lore = new ArrayList<>();
            filterSign_lore.add(Component.text("Hier kannst du die Itemnamen, Lores usw. nach Text filtern.").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY));
            if(playerFilter.containsKey(player.getUniqueId())){
                sortHopper_lore.add(Component.text(playerFilter.get(player.getUniqueId())).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
            }
            filterSign_meta.lore(filterSign_lore);
            filterSign.setItemMeta(filterSign_meta);



            QuickShopAPI api = LyriaHandelskontor.getQuickshopAPI();

            List<Shop> shops = api.getShopManager().getAllShops();
            if(!filterString.equals("")){shops = filterShops(shops, filterString);}
            ArrayList<ItemStack> messageItems = new ArrayList<>();
            switch(sortType){
                case 0:
                    shops.sort((a, b) -> a.ownerName().compareToIgnoreCase(b.ownerName()));
                    break;
                case 1:
                    shops.sort((a, b) -> a.getItem().getType().toString().compareToIgnoreCase( b.getItem().getType().toString()));
                    break;
                case 2:
                    shops.sort(Comparator.comparingDouble(Shop::getPrice));
                    break;
                case 3:
                    shops.sort(Comparator.comparingInt(Shop::getRemainingStock));
                    break;

            }
            for (Shop shop : shops) {
                if(shop.getShopType()== ShopType.BUYING){continue;}
                List<Component> message_lore = new ArrayList<>();
                ItemStack messageItem = shop.getItem().clone();
                ItemMeta message_meta = messageItem.getItemMeta();
                if(message_meta.hasLore()){
                message_lore.addAll(Objects.requireNonNull(message_meta.lore()));}

                double price = shop.getPrice();
                String seller = shop.ownerName();
                int amount = shop.getRemainingStock();

                message_lore.add(Component.text("Verkaufspreis: "+price).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
                message_lore.add(Component.text("Verkäufer: "+seller).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));
                message_lore.add(Component.text("Auf Lager: "+amount).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.BLUE));

                message_meta.lore(message_lore);


                PersistentDataContainer dataMessage = message_meta.getPersistentDataContainer();
                dataMessage.set(namespacedKeyLocation, PersistentDataType.INTEGER_ARRAY, new int[]{shop.getLocation().getBlockX(), shop.getLocation().getBlockY(), shop.getLocation().getBlockZ()});
                dataMessage.set(namespacedKeyWorld, PersistentDataType.STRING, shop.getLocation().getWorld().getName());


                messageItem.setItemMeta(message_meta);


                messageItems.add(messageItem);
            }
        ArrayList<ItemStack> menuItems = new ArrayList<>();
        int pages = messageItems.size()/28;


        for (int i = 0; i < 54; i++) {
                if (i % 9 == 0 || i % 9 == 8) {

                    if (i == 9 * 5 && page > 0) {
                        menuItems.add(leftArrow);
                    } else if (i == 9 * 5 + 8 && page < pages) {
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

            ItemStack[] arr = new ItemStack[menuItems.size()];
            menuItems.toArray(arr);
            gui.setContents(arr);
            player.openInventory(gui);

    }

    private static ArrayList<Shop> filterShops(List<Shop> shops, String filter){
        ArrayList<Shop> filteredShops = new ArrayList<>();
        for(Shop shop: shops){
            if(filteredShops.contains(shop)){continue;}
            ItemStack item = shop.getItem();
            String materialString = item.getType().toString();
            String customNameString = null;
            if( item.getItemMeta().hasDisplayName()){
                customNameString =((TextComponent) item.getItemMeta().displayName()).content();
            }
            if(Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(materialString).find() || customNameString!=null && Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(customNameString).find() || Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE).matcher(item.getItemMeta().toString()).find()){
                filteredShops.add(shop);
            }

        }
        return filteredShops;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission("lyriahandelskontor.handelskontor")){
            commandSender.sendMessage(ChatColor.RED+"Keine Berechtigung");
            return true;
        }
        if(strings.length==0){
            commandSender.sendMessage(ChatColor.RED+"Fehlende Argumente");
            return true;
        }
        if(strings.length==2){
            commandSender.sendMessage(ChatColor.RED+"Zu viele Argumente");
            return true;
        }
        String playername = strings[0];
        Player player = commandSender.getServer().getPlayer(playername);
        if(player==null){
            commandSender.sendMessage(ChatColor.RED+"Der Spieler existiert nicht");
            return true;
        }
        playerPage.put(player.getUniqueId(), 0);
        playerSort.put(player.getUniqueId(), 0);
        playerFilter.put(player.getUniqueId(), "");
        openHandelskontorGUI(player);

        return false;
    }

    public static class HandelskontorGUIHolder implements InventoryHolder {

        public HandelskontorGUIHolder() {
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class BuyGUIHolder implements InventoryHolder {

        public BuyGUIHolder() {
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
