package org.silvius.lyriahandelskontor.Buttons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

public class PlaceHolderButton<MH extends MenuHolder<?>> extends ItemButton<MH> {
    public PlaceHolderButton(Material material){
        super(new ItemStack(material));
        ItemStack itemStack = this.getIcon();
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(".").color(NamedTextColor.BLACK));
        itemStack.setItemMeta(itemMeta);
        this.setIcon(itemStack);
    }



}
