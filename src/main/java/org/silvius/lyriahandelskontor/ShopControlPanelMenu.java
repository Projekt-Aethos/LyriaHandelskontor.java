package org.silvius.lyriahandelskontor;

import org.bukkit.block.TileState;
import org.bukkit.plugin.Plugin;
import org.maxgamer.quickshop.api.shop.Shop;
import xyz.janboerman.guilib.api.GuiListener;
import xyz.janboerman.guilib.api.menu.MenuHolder;

public class ShopControlPanelMenu<P extends Plugin> extends MenuHolder<P> {
    public Shop shop;
    public TileState sign;

    public ShopControlPanelMenu(P plugin, int size, String title, Shop shop, TileState sign) {
        super(GuiListener.getInstance(), plugin, size, title);
        this.shop = shop;
        this.sign = sign;
    }
}
