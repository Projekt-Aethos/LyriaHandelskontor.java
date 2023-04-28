package org.silvius.lyriahandelskontor;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;

public class FilterSign implements Sign {
    @Override
    public @NotNull List<Component> lines() {
        return null;
    }

    @Override
    public @NotNull Component line(int i) throws IndexOutOfBoundsException {
        return null;
    }

    @Override
    public void line(int i, @NotNull Component component) throws IndexOutOfBoundsException {

    }

    @Override
    public @NotNull String[] getLines() {
        return new String[0];
    }

    @Override
    public @NotNull String getLine(int i) throws IndexOutOfBoundsException {
        return null;
    }

    @Override
    public void setLine(int i, @NotNull String s) throws IndexOutOfBoundsException {

    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public void setEditable(boolean b) {

    }

    @Override
    public boolean isGlowingText() {
        return false;
    }

    @Override
    public void setGlowingText(boolean b) {

    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return null;
    }

    @Override
    public boolean isSnapshot() {
        return false;
    }

    @Override
    public @NotNull Block getBlock() {
        return null;
    }

    @Override
    public @NotNull MaterialData getData() {
        return null;
    }

    @Override
    public @NotNull BlockData getBlockData() {
        return null;
    }

    @Override
    public @NotNull Material getType() {
        return null;
    }

    @Override
    public byte getLightLevel() {
        return 0;
    }

    @Override
    public @NotNull World getWorld() {
        return LyriaHandelskontor.getPlugin().getServer().getWorlds().get(0);
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getZ() {
        return 0;
    }

    @Override
    public @NotNull Location getLocation() {
        return null;
    }

    @Override
    public @Nullable Location getLocation(@Nullable Location location) {
        return null;
    }

    @Override
    public @NotNull Chunk getChunk() {
        return null;
    }

    @Override
    public void setData(@NotNull MaterialData materialData) {

    }

    @Override
    public void setBlockData(@NotNull BlockData blockData) {

    }

    @Override
    public void setType(@NotNull Material material) {

    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public boolean update(boolean b) {
        return false;
    }

    @Override
    public boolean update(boolean b, boolean b1) {
        return false;
    }

    @Override
    public byte getRawData() {
        return 0;
    }

    @Override
    public void setRawData(byte b) {

    }

    @Override
    public boolean isPlaced() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public @Unmodifiable @NotNull Collection<ItemStack> getDrops() {
        return null;
    }

    @Override
    public @Unmodifiable @NotNull Collection<ItemStack> getDrops(@Nullable ItemStack itemStack) {
        return null;
    }

    @Override
    public @Unmodifiable @NotNull Collection<ItemStack> getDrops(@NotNull ItemStack itemStack, @Nullable Entity entity) {
        return null;
    }

    @Override
    public @Nullable DyeColor getColor() {
        return null;
    }

    @Override
    public void setColor(DyeColor dyeColor) {

    }

    @Override
    public void setMetadata(@NotNull String s, @NotNull MetadataValue metadataValue) {

    }

    @Override
    public @NotNull List<MetadataValue> getMetadata(@NotNull String s) {
        return null;
    }

    @Override
    public boolean hasMetadata(@NotNull String s) {
        return false;
    }

    @Override
    public void removeMetadata(@NotNull String s, @NotNull Plugin plugin) {

    }
}
