package org.silvius.lyriahandelskontor.Buttons;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.janboerman.guilib.api.ItemBuilder;

public class CloseButton<P extends Plugin> extends xyz.janboerman.guilib.api.menu.CloseButton<P> {
    public CloseButton() {
        this(Material.BARRIER);
    }

    public CloseButton(Material material) {
        this(material, "Schließen");
    }

    public CloseButton(ItemStack icon) {
        super(icon);
    }

    public CloseButton(Material material, String displayName) {
        super((new ItemBuilder(material)).name(displayName).build());
    }
}
