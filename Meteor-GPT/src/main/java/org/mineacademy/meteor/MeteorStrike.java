package org.mineacademy.meteor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class MeteorStrike {

    final Location target;
    final World world;
    final Element element;
    final int radius;
    final List<SavedBlock> blocks         = new ArrayList<>();
    final List<DisplayPiece> pieces       = new ArrayList<>();
    final Map<UUID, Passenger> passengers = new LinkedHashMap<>();
    int tick;
    private final MeteorPlugin plugin;
    private final Player initiator;
    private final Location start;
    private final List<SavedBlock> occupied = new ArrayList<>();
    private final List<DisplayPiece> meteor = new ArrayList<>();
    private final List<Vector> shell        = new ArrayList<>();
    private final Map<UUID, Sky> skies      = new HashMap<>();
    private final Map<Long, Double> ground  = new HashMap<>();
    private final List<Chunk> heldChunks    = new ArrayList<>();
    private final List<Player> viewers      = new ArrayList<>();
    private int prepared;
    private int restored;
    private boolean started;
    private boolean finished;
    private long maxTickNanos;

    MeteorStrike(MeteorPlugin plugin, MeteorMenu menu) {
        this.plugin    = plugin;
        this.initiator = menu.player;
        this.target    = menu.target.clone().add(0.5, 0.5, 0.5);
        this.world     = this.target.getWorld();
        this.element   = menu.element;
        this.radius    = 2 + menu.power;
        Vector beyond  = this.target.toVector().subtract(menu.player.getLocation().toVector()).setY(0);

        if (beyond.lengthSquared() < 0.01)
            beyond = menu.player.getLocation().getDirection().setY(0);

        if (beyond.lengthSquared() < 0.01)
            beyond.setZ(1);

        this.start = this.target.clone().add(beyond.normalize().multiply(40)).add(0, 60, 0);
    }

    void start() {
        if (this.plugin.visuals.addChunkTicket != null || this.plugin.visuals.setForceLoaded != null)
            for (int x = (this.target.getBlockX() - 64) >> 4; x <= (this.target.getBlockX() + 64) >> 4; x++)
                for (int z = (this.target.getBlockZ() - 64) >> 4; z <= (this.target.getBlockZ() + 64) >> 4; z++) {
                    if (!this.world.isChunkLoaded(x, z))
                        continue;

                    Chunk chunk = this.world.getChunkAt(x, z);

                    if (this.plugin.visuals.addChunkTicket != null) {
                        BukkitVisuals.invoke(this.plugin.visuals.addChunkTicket, chunk, this.plugin);
                        this.heldChunks.add(chunk);
                    } else if (!(Boolean) BukkitVisuals.invoke(this.plugin.visuals.isForceLoaded, chunk)) {
                        BukkitVisuals.invoke(this.plugin.visuals.setForceLoaded, chunk, true);
                        this.heldChunks.add(chunk);
                    }
                }

        int min = this.plugin.visuals.minHeight(this.world);
        int max = this.world.getMaxHeight();

        for (int y = -this.radius; y <= this.radius; y++)
            for (int x = -this.radius; x <= this.radius; x++)
                for (int z = -this.radius; z <= this.radius; z++) {
                    if (x * x + y * y + z * z > this.radius * this.radius)
                        continue;

                    int blockY = this.target.getBlockY() + y;

                    if (blockY < min || blockY >= max)
                        continue;

                    SavedBlock saved = new SavedBlock(this.world.getBlockAt(this.target.getBlockX() + x, blockY, this.target.getBlockZ() + z), this.plugin.visuals);
                    this.blocks.add(saved);

                    if (saved.occupied)
                        this.occupied.add(saved);
                }

        for (int index = 0; index < 96; index++) {
            double y     = 1 - 2 * (index + 0.5) / 96;
            double angle = index * 2.399963229728653;
            double width = Math.sqrt(1 - y * y);
            this.shell.add(new Vector(Math.cos(angle) * width * 2.15, y * 2.15, Math.sin(angle) * width * 2.15));
        }

        this.started = true;
        this.updateViewers();
        this.countdown();
        this.plugin.getLogger().info("Strike started: " + this.element + ", power " + (this.radius - 2) + ", blocks " + this.occupied.size());
    }

    void advance() {
        if (this.finished)
            return;

        long began = System.nanoTime();

        if (!this.started) {
            this.start();
            this.maxTickNanos = Math.max(this.maxTickNanos, System.nanoTime() - began);

            return;
        }

        this.tick++;

        if (this.tick % 5 == 0)
            this.updateViewers();

        if (this.tick < 95)
            this.prepare();

        if (this.tick < 60) {
            if (this.tick % 20 == 0)
                this.countdown();

            if (this.tick % 3 == 0) {
                this.ring(this.radius, this.tick * 0.06, 0.1, 64);
                this.ring(Math.max(0.5, this.radius * (1 - this.tick % 20 / 20D)), 0, 0.15, 32);
            }
        } else if (this.tick < 100)
            this.approach();
        else {
            if (this.tick == 100)
                this.impact();

            if (this.tick >= 120 && this.tick < 260 && this.tick % 2 == 0)
                this.flyDebris();

            if (this.tick >= 120 && this.tick <= 178)
                this.shockwave();

            if (this.tick == 120) {
                this.title("Time breaks", "", 22);
                this.sound("entity.generic.explode", "random.explode", 1.6F, 0.55F);
            }

            if (this.tick >= 160 && this.tick < 260)
                this.finale();

            if (this.tick >= 260 && this.tick < 360)
                this.rebuild();
        }

        this.protectPassengers();

        if (this.tick == 360)
            this.finish(false);

        this.maxTickNanos = Math.max(this.maxTickNanos, System.nanoTime() - began);
    }

    private void prepare() {
        int budget = Math.max(8, (this.occupied.size() + this.shell.size() + 79) / 80);

        for (int count = 0; count < budget; count++) {
            if (this.prepared < this.shell.size()) {
                DisplayPiece piece = this.piece(this.target, this.element.rock(this.prepared), this.prepared);
                this.meteor.add(piece);
            } else {
                int index = this.prepared - this.shell.size();

                if (index >= this.occupied.size())
                    return;

                SavedBlock saved = this.occupied.get(index);
                saved.debris     = this.piece(saved.center, this.plugin.visuals.debris(saved.state, this.element), index);
            }

            this.prepared++;
        }
    }

    private DisplayPiece piece(Location location, org.bukkit.inventory.ItemStack item, int index) {
        DisplayPiece piece = new DisplayPiece(this.plugin, location, this.target, item, index);
        this.pieces.add(piece);

        return piece;
    }

    private void countdown() {
        int remaining = 3 - this.tick / 20;
        this.title(Integer.toString(remaining), this.element.color + this.element.label + " meteor incoming", 18);
        this.initiator.sendMessage(this.element.color + "Meteor in " + remaining + "...");
        this.sound("block.note_block.pling", "note.pling", 1.2F, 0.7F + this.tick / 100F);
        this.sound("block.portal.ambient", "portal.portal", 0.55F, 0.6F);
    }

    private void approach() {
        double progress = (this.tick - 60) / 40D;
        Location head   = this.start.clone().add(this.target.toVector().subtract(this.start.toVector()).multiply(progress));

        if (this.tick == 60) {
            this.title("Look up", this.element.color + "The sky is falling", 28);
            this.sound("entity.ender_dragon.growl", "mob.enderdragon.growl", 1.1F, 0.65F);

        }

        boolean loaded  = this.world.isChunkLoaded(head.getBlockX() >> 4, head.getBlockZ() >> 4);
        double rotation = this.tick * 0.075;

        for (int index = 0; index < this.shell.size(); index++) {
            Vector offset = this.shell.get(index);
            double x      = offset.getX() * Math.cos(rotation) - offset.getZ() * Math.sin(rotation);
            double z      = offset.getX() * Math.sin(rotation) + offset.getZ() * Math.cos(rotation);

            if (loaded && index < this.meteor.size()) {
                DisplayPiece piece = this.meteor.get(index);
                piece.move(head.getX() + x, head.getY() + offset.getY(), head.getZ() + z, rotation);
                piece.reveal();
            }

            if (index % 2 == this.tick % 2)
                this.emit(head.clone().add(x * 1.15, offset.getY() * 1.15, z * 1.15), BukkitVisuals.Dust.COLOR, 1, 0, 0);
        }

        Vector behind = this.start.toVector().subtract(this.target.toVector()).normalize();

        for (int index = 0; index < 12; index++) {
            double angle  = this.tick * 0.5 + index * 0.65;
            Location tail = head.clone().add(behind.clone().multiply(index * 0.95)).add(Math.cos(angle) * 0.8, Math.sin(angle) * 0.8, Math.sin(angle) * 0.8);
            this.emit(tail, this.elementDust(), 3, 0.3, 0.025);
            this.emit(tail, BukkitVisuals.Dust.COLOR, 1, 0, 0);
        }

        this.emit(head, BukkitVisuals.Dust.SMOKE, 8, 1, 0.01);

        if (this.tick % 2 == 0)
            this.ring(this.radius, rotation, 0.12, 72);

        if (this.tick % 8 == 0)
            this.sound("entity.firework_rocket.launch", "fireworks.launch", 1.2F, 0.6F + (float) progress);
    }

    private void impact() {
        for (DisplayPiece piece : this.meteor)
            if (this.element == Element.VOID)
                piece.hide();
            else
                piece.remove();

        for (LivingEntity entity : this.world.getLivingEntities())
            if (!(entity instanceof org.bukkit.entity.ArmorStand) && entity.getLocation().distanceSquared(this.target) <= 128 * 128)
                this.passengers.put(entity.getUniqueId(), new Passenger(entity));

        for (SavedBlock saved : this.occupied) {
            saved.change(Material.AIR);

            if (saved.debris == null)
                throw new IllegalStateException("Debris was not prepared before impact");

            DisplayPiece piece = saved.debris;
            piece.reveal();
            piece.move(saved.center.getX() + piece.direction.getX() * 1.4, saved.center.getY() + this.radius + 1.8, saved.center.getZ() + piece.direction.getZ() * 1.4, piece.phase);
        }

        for (Passenger passenger : this.passengers.values()) {
            LivingEntity entity = passenger.entity;

            if (!(entity instanceof Player) && !(entity instanceof Animals))
                continue;

            if (entity.getLocation().distanceSquared(this.target) > 4 * this.radius * this.radius)
                continue;

            Vector away = entity.getLocation().toVector().subtract(this.target.toVector()).setY(0);

            if (away.lengthSquared() < 0.01)
                away.setX(1);

            entity.setFallDistance(0);
            entity.setVelocity(away.normalize().multiply(1.35 + this.radius * 0.1).setY(1.1 + this.radius * 0.07));
            passenger.launched = true;
        }

        this.title("", ChatColor.WHITE + "Time stands still", 20);
        this.sound("entity.generic.explode", "random.explode", 2F, 0.55F);
        this.sound("entity.lightning_bolt.thunder", "ambient.weather.thunder", 1.4F, 0.65F);
        this.emit(this.target.clone().add(0, 1, 0), BukkitVisuals.Dust.EXPLOSION, 8, this.radius * 0.45, 0);

        for (int index = 0; index < 90; index++) {
            double angle   = index * Math.PI * 2 / 90;
            Location point = this.target.clone().add(Math.cos(angle) * this.radius, 0.4, Math.sin(angle) * this.radius);
            this.emit(point, BukkitVisuals.Dust.SPARK, 2, 0.3, 0.18);
        }
    }

    private void flyDebris() {
        double age = (this.tick - 120) / 20D;

        for (int index = 0; index < this.occupied.size(); index++) {
            DisplayPiece piece = this.occupied.get(index).debris;

            if (piece.removed)
                continue;

            double distance = 1.4 + (7 + this.radius * 0.65) * (1 - Math.exp(-age * 1.5));
            double height   = this.radius + 1.8 + 5 * Math.sin(Math.min(age, 2) * Math.PI / 4);
            double x        = piece.origin.getX() + piece.direction.getX() * distance;
            double y        = piece.origin.getY() + height;
            double z        = piece.origin.getZ() + piece.direction.getZ() * distance;

            if (this.tick >= 160) {
                double orbit = (this.tick - 160) * 0.007;
                double dx    = x - this.target.getX();
                double dz    = z - this.target.getZ();
                x            = this.target.getX() + dx * Math.cos(orbit) - dz * Math.sin(orbit);
                z            = this.target.getZ() + dx * Math.sin(orbit) + dz * Math.cos(orbit);
                y += Math.sin(piece.phase + orbit * 4) * 0.45;

                if (this.element == Element.VOID) {
                    double swallow  = Math.max(0, Math.min(1, (this.tick - 174 - index % 38) / 45D));
                    double collapse = (1 - swallow) * (1 - swallow);
                    x               = this.target.getX() + (x - this.target.getX()) * collapse;
                    z               = this.target.getZ() + (z - this.target.getZ()) * collapse;
                    y               = this.target.getY() + 2 + (y - this.target.getY() - 2) * collapse;

                    if (swallow >= 1) {
                        piece.remove();

                        continue;
                    }
                }
            }

            piece.move(x, y, z, piece.phase + age * 0.8);
        }
    }

    private void shockwave() {
        double age = (this.tick - 120) / 58D;

        for (int band = 0; band < 3; band++) {
            double distance = this.radius + age * (this.radius * 3 + 9) - band * 1.3;

            if (distance < this.radius)
                continue;

            this.ring(distance, this.tick * 0.03, Math.sin(age * Math.PI) * (1.7 - band * 0.35), 100);
        }

        if (this.tick % 12 == 0)
            this.sound("block.piston.extend", "tile.piston.out", 0.85F, 0.6F + (float) age);
    }

    private void finale() {
        if (this.tick == 160) {
            this.title(this.element.finale, "", 48);
            this.sound(this.element == Element.ICE ? "block.glass.break" : "block.portal.trigger", this.element == Element.ICE ? "dig.glass" : "portal.trigger", 1.1F, 0.65F);
        }

        double progress = (this.tick - 160) / 100D;

        if (this.element != Element.VOID) {
            int from = (this.tick - 160) * this.blocks.size() / 60;
            int to   = Math.min(this.blocks.size(), (this.tick - 159) * this.blocks.size() / 60);

            for (int index = from; index < to; index++) {
                SavedBlock saved  = this.blocks.get(index);
                double dx         = saved.center.getX() - this.target.getX();
                double dz         = saved.center.getZ() - this.target.getZ();
                double dy         = saved.center.getY() - this.target.getY();
                double horizontal = Math.sqrt(dx * dx + dz * dz);

                if (this.element == Element.FIRE) {
                    if (dy < -1.5 && dy >= -this.radius + 0.5) {
                        boolean crust = ((saved.state.getX() * 31 + saved.state.getZ() * 17) & 7) == 0;
                        saved.change(crust ? Material.OBSIDIAN : Material.LAVA);
                    }
                } else {
                    double ridge = this.radius * 0.8 - horizontal * 0.85 + Math.sin(dx * 1.7) * Math.cos(dz * 1.3) * 1.8;

                    if (dy <= ridge)
                        saved.change((saved.state.getX() + saved.state.getY() + saved.state.getZ()) % 5 == 0 ? Material.ICE : Material.PACKED_ICE);
                }
            }
        }

        if (this.tick % 2 != 0)
            return;

        if (this.element == Element.VOID) {
            for (int index = 0; index < this.meteor.size(); index++) {
                DisplayPiece piece = this.meteor.get(index);
                Vector offset      = this.shell.get(index).clone().multiply(0.72);

                if (this.tick == 160)
                    piece.reveal();

                piece.move(this.target.getX() + offset.getX(), this.target.getY() + 2 + offset.getY(), this.target.getZ() + offset.getZ(), 0);
            }

            for (int index = 0; index < 64; index++) {
                double angle   = index * Math.PI * 2 / 64 + this.tick * 0.12;
                double radius  = 2.8 + Math.sin(index * 0.6 + this.tick * 0.1) * 0.35;
                Location point = this.target.clone().add(Math.cos(angle) * radius, 2 + Math.sin(angle) * radius * 0.25, Math.sin(angle) * radius);
                this.emit(point, BukkitVisuals.Dust.COLOR, 1, 0, 0);
                this.emit(point, BukkitVisuals.Dust.PORTAL, 2, 0.1, 0.12);
            }

            for (int index = 0; index < 20; index++) {
                double angle = index * 2.399963229728653 + this.tick * 0.035;
                double y     = 1 - 2 * (index + 0.5) / 20;
                double width = Math.sqrt(1 - y * y) * 1.65;
                this.emit(this.target.clone().add(Math.cos(angle) * width, 2 + y * 1.65, Math.sin(angle) * width), BukkitVisuals.Dust.SMOKE, 5, 0.18, 0);
            }
        } else {
            for (int arm = 0; arm < 5; arm++)
                for (int point = 0; point < 10; point++) {
                    double angle      = arm * Math.PI * 2 / 5 + this.tick * 0.075 + point * 0.3;
                    double width      = this.radius * (1 - point / 13D);
                    double y          = this.element == Element.FIRE ? -1.4 + point * 0.28 : this.radius * 0.65 + point * 0.35;
                    Location location = this.target.clone().add(Math.cos(angle) * width, y + Math.sin(progress * Math.PI), Math.sin(angle) * width);
                    this.emit(location, this.elementDust(), 2, 0.1, 0.02);
                    this.emit(location, BukkitVisuals.Dust.COLOR, 1, 0, 0);
                }
        }

        if (this.tick % 20 == 0) {
            if (this.element == Element.FIRE)
                this.sound("block.lava.pop", "liquid.lavapop", 1.2F, 0.7F);
            else if (this.element == Element.ICE)
                this.sound("block.glass.break", "dig.glass", 0.85F, 0.75F + (float) progress);
            else
                this.sound("entity.enderman.teleport", "mob.endermen.portal", 0.75F, 0.55F + (float) progress * 0.4F);
        }
    }

    private void rebuild() {
        if (this.tick == 260) {
            for (DisplayPiece piece : this.meteor)
                piece.remove();

            this.title("Rewind", ChatColor.WHITE + "Everything returns", 35);
            this.sound("block.beacon.activate", "portal.travel", 1.1F, 1.45F);
        }

        int end = (this.tick - 259) * this.blocks.size() / 100;

        while (this.restored < end) {
            SavedBlock saved = this.blocks.get(this.restored++);
            saved.restore();

            if (saved.debris != null)
                saved.debris.remove();

            if (saved.occupied && this.restored % 3 == 0)
                this.emit(saved.center, BukkitVisuals.Dust.COLOR, 1, 0, 0);
        }

        if (this.tick % 2 == 0)
            for (int index = this.restored; index < this.blocks.size(); index++) {
                SavedBlock saved = this.blocks.get(index);

                if (saved.debris == null || saved.debris.removed)
                    continue;

                int due       = 260 + index * 100 / this.blocks.size();
                int remaining = due - this.tick;

                if (remaining > 20)
                    continue;

                Location current = saved.debris.location();
                Vector movement  = saved.center.toVector().subtract(current.toVector()).multiply(2D / Math.max(2, remaining));
                current.add(movement);
                saved.debris.move(current.getX(), current.getY(), current.getZ(), remaining * 0.05);
            }

        if (this.tick % 5 == 0)
            this.sound("block.note_block.harp", "note.harp", 0.65F, 0.6F + (this.tick - 260) / 85F);
    }

    private void updateViewers() {
        this.viewers.clear();

        for (Player player : this.world.getPlayers())
            if (player.getLocation().distanceSquared(this.target) <= 128 * 128) {
                this.viewers.add(player);

                if (!this.skies.containsKey(player.getUniqueId()))
                    this.skies.put(player.getUniqueId(), new Sky(player));

                player.setPlayerTime(18000, false);
            }

        if (this.tick >= 100)
            for (LivingEntity entity : this.world.getLivingEntities())
                if (!(entity instanceof org.bukkit.entity.ArmorStand) && this.world.equals(entity.getWorld()) && entity.getLocation().distanceSquared(this.target) <= 128 * 128 && !this.passengers.containsKey(entity.getUniqueId()))
                    this.passengers.put(entity.getUniqueId(), new Passenger(entity));

        Iterator<Map.Entry<UUID, Sky>> iterator = this.skies.entrySet().iterator();

        while (iterator.hasNext()) {
            Sky sky = iterator.next().getValue();

            if (!this.viewers.contains(sky.player)) {
                sky.restore();
                iterator.remove();
            }
        }
    }

    private void ring(double radius, double rotation, double height, int points) {
        for (int index = 0; index < points; index++) {
            double angle = index * Math.PI * 2 / points + rotation;
            double x     = this.target.getX() + Math.cos(angle) * radius;
            double z     = this.target.getZ() + Math.sin(angle) * radius;
            double y     = this.groundHeight((int) Math.floor(x), (int) Math.floor(z));
            this.emit(new Location(this.world, x, y + 0.15 + height, z), BukkitVisuals.Dust.COLOR, 1, 0, 0);

            if (index % 6 == 0)
                this.emit(new Location(this.world, x, y + 0.15 + height, z), this.elementDust(), 1, 0, 0);
        }
    }

    private double groundHeight(int x, int z) {
        long key      = ((long) x << 32) ^ (z & 0xffffffffL);
        Double cached = this.ground.get(key);

        if (cached != null)
            return cached;

        double height = this.target.getY();

        if (this.world.isChunkLoaded(x >> 4, z >> 4)) {
            int upper = Math.min(this.world.getMaxHeight() - 1, this.target.getBlockY() + this.radius + 3);
            int lower = Math.max(this.plugin.visuals.minHeight(this.world), this.target.getBlockY() - this.radius - 12);

            for (int y = upper; y >= lower; y--)
                if (this.world.getBlockAt(x, y, z).getType().isSolid()) {
                    height = y + 1;

                    break;
                }
        }

        this.ground.put(key, height);

        return height;
    }

    private BukkitVisuals.Dust elementDust() {
        if (this.element == Element.FIRE)
            return BukkitVisuals.Dust.FLAME;

        if (this.element == Element.ICE)
            return BukkitVisuals.Dust.SNOW;

        return BukkitVisuals.Dust.PORTAL;
    }

    private void emit(Location location, BukkitVisuals.Dust dust, int count, double spread, double speed) {
        for (Player player : this.viewers)
            this.plugin.visuals.particle(player, location, dust, this.element, count, spread, speed);
    }

    private void sound(String modern, String legacy, float volume, float pitch) {
        for (Player player : this.viewers)
            this.plugin.visuals.sound(player, player.getLocation(), modern, legacy, volume, pitch);
    }

    private void title(String heading, String subtitle, int duration) {
        for (Player player : this.viewers)
            this.plugin.visuals.title(player, this.element.color + heading, subtitle, duration);
    }

    boolean contains(Location location, int margin) {
        return this.world.equals(location.getWorld()) && Math.abs(location.getX() - this.target.getX()) <= this.radius + margin && Math.abs(location.getY() - this.target.getY()) <= this.radius + margin && Math.abs(location.getZ() - this.target.getZ()) <= this.radius + margin;
    }

    boolean protects(Entity entity) {
        return entity.hasMetadata("MeteorGPT_Visual") || this.passengers.containsKey(entity.getUniqueId()) || this.world.equals(entity.getWorld()) && entity.getLocation().distanceSquared(this.target) <= 128 * 128;
    }

    private void protectPassengers() {
        for (Passenger passenger : this.passengers.values()) {
            LivingEntity entity = passenger.entity;

            if (!entity.isValid())
                continue;

            entity.setFallDistance(0);
            passenger.protect();

            if (passenger.launched && entity.getLocation().getY() < this.plugin.visuals.minHeight(entity.getWorld()) + 1)
                passenger.returnSafely();

            if (this.tick >= 260 && this.contains(entity.getLocation(), 1) && entity.getLocation().getBlock().getType().isSolid())
                passenger.returnSafely();
        }
    }

    void forget(Player player) {
        Sky sky = this.skies.remove(player.getUniqueId());

        if (sky != null)
            sky.restore();

        Passenger passenger = this.passengers.remove(player.getUniqueId());

        if (passenger != null && passenger.launched)
            passenger.returnSafely();

        if (passenger != null)
            passenger.restoreProtection();
    }

    void finish(boolean interrupted) {
        if (this.finished)
            return;

        for (DisplayPiece piece : this.pieces)
            piece.remove();

        RuntimeException failure = null;

        for (SavedBlock block : this.blocks)
            try {
                block.restore();
            } catch (RuntimeException exception) {
                if (failure == null)
                    failure = exception;
                else
                    failure.addSuppressed(exception);
            }

        for (Passenger passenger : this.passengers.values()) {
            passenger.entity.setFallDistance(0);

            if (passenger.launched || this.contains(passenger.entity.getLocation(), 1))
                passenger.returnSafely();

            passenger.restoreProtection();
        }

        for (Sky sky : this.skies.values()) {
            sky.restore();
            sky.player.resetTitle();
        }

        this.skies.clear();

        for (Chunk chunk : this.heldChunks)
            if (this.plugin.visuals.removeChunkTicket != null)
                BukkitVisuals.invoke(this.plugin.visuals.removeChunkTicket, chunk, this.plugin);
            else
                BukkitVisuals.invoke(this.plugin.visuals.setForceLoaded, chunk, false);

        this.heldChunks.clear();

        if (!interrupted) {
            this.sound("entity.player.levelup", "random.levelup", 0.8F, 1.35F);
            this.initiator.sendMessage(this.element.color + "The meteor has passed. Everything is restored.");
        }

        if (failure != null)
            throw failure;

        this.finished = true;
        this.plugin.getLogger().info("Strike restored: " + this.element + ", blocks " + this.occupied.size() + ", maximum task time " + String.format(java.util.Locale.ROOT, "%.3f", this.maxTickNanos / 1000000D) + " ms");
        this.plugin.complete(this);
    }

    private static final class Sky {

        private final Player player;
        private final boolean relative;
        private final long offset;

        private Sky(Player player) {
            this.player   = player;
            this.relative = player.isPlayerTimeRelative();
            this.offset   = player.getPlayerTimeOffset();
        }

        private void restore() {
            if (this.relative && this.offset == 0)
                this.player.resetPlayerTime();
            else
                this.player.setPlayerTime(this.offset, this.relative);
        }
    }

    static final class Passenger {

        final LivingEntity entity;
        final Location origin;
        private final int noDamageTicks;
        private final double lastDamage;
        boolean launched;

        Passenger(LivingEntity entity) {
            this.entity        = entity;
            this.origin        = entity.getLocation();
            this.noDamageTicks = entity.getNoDamageTicks();
            this.lastDamage    = entity.getLastDamage();
            this.protect();
        }

        void protect() {
            this.entity.setNoDamageTicks(Integer.MAX_VALUE);
            this.entity.setLastDamage(Float.MAX_VALUE);
        }

        void restoreProtection() {
            this.entity.setNoDamageTicks(this.noDamageTicks);
            this.entity.setLastDamage(this.lastDamage);
        }

        void returnSafely() {
            if (!this.entity.isValid())
                return;

            Location safe = this.origin.clone();
            World world   = safe.getWorld();

            while (safe.getY() < world.getMaxHeight() - 2 && (safe.getBlock().getType().isSolid() || safe.clone().add(0, 1, 0).getBlock().getType().isSolid()))
                safe.add(0, 1, 0);

            this.entity.setVelocity(new Vector());
            this.entity.setFallDistance(0);
            this.entity.teleport(safe);
            this.launched = false;
        }
    }
}
