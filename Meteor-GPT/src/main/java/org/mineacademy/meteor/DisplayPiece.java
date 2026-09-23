package org.mineacademy.meteor;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

final class DisplayPiece {

    final ArmorStand entity;
    final Location origin;
    final Vector direction;
    final double phase;
    private final Location position;
    private final ItemStack item;
    private boolean visible;
    boolean removed;

    DisplayPiece(MeteorPlugin plugin, Location origin, Location target, ItemStack item, int index) {
        this.origin    = origin.clone();
        this.position  = origin.clone();
        this.item      = item;
        this.phase     = index * 2.399963229728653;
        this.direction = origin.toVector().subtract(target.toVector());

        if (this.direction.lengthSquared() < 0.01)
            this.direction.setX(Math.cos(this.phase)).setZ(Math.sin(this.phase));

        this.direction.normalize();
        this.entity = (ArmorStand) origin.getWorld().spawnEntity(origin.clone().subtract(0, 1.44, 0), EntityType.ARMOR_STAND);
        this.entity.setMetadata("MeteorGPT_Visual", new FixedMetadataValue(plugin, true));
        this.entity.setVisible(false);
        this.entity.setGravity(false);
        this.entity.setMarker(true);
        this.entity.setBasePlate(false);
        this.entity.setRemoveWhenFarAway(false);
    }

    void reveal() {
        if (!this.removed && !this.visible) {
            this.entity.setHelmet(this.item);
            this.visible = true;
        }
    }

    void hide() {
        if (!this.removed)
            this.entity.setHelmet(null);

        this.visible = false;
    }

    void move(double x, double y, double z, double spin) {
        if (this.removed)
            return;

        this.position.setX(x);
        this.position.setY(y - 1.44);
        this.position.setZ(z);
        this.position.setYaw((float) (spin * 40));

        if (!this.entity.teleport(this.position))
            throw new IllegalStateException("Cannot move meteor visual " + this.entity.getUniqueId());

        this.entity.setHeadPose(new EulerAngle(spin * 0.25, 0, spin * 0.17));
    }

    Location location() {
        return this.position.clone().add(0, 1.44, 0);
    }

    void remove() {
        if (this.removed)
            return;

        this.entity.setHelmet(null);
        this.entity.remove();
        this.removed = true;
    }
}
