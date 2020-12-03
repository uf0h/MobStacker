package me.ufo.mobstacker.listeners;

import java.util.concurrent.ThreadLocalRandom;
import me.ufo.mobstacker.Config;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import me.ufo.mobstacker.mob.StackedMobDrops;
import me.ufo.shaded.org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public final class StackedMobListener implements Listener {

  private final MSPlugin plugin;

  public StackedMobListener(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onStackedMobDeathEvent(final StackedMobDeathEvent event) {
    if (event.getCause() == StackedMobDeathCause.FALL) {
      final StackedMob sm = event.getStackedMob();
      final int dropAmount = FastMath.min(event.getAmountDied(), Config.MAX_DEATHS);

      sm.destroyEntity();

      for (final ItemStack item : StackedMobDrops.getDropsForEntity(event.getEntityType())) {
        final ItemStack cloned = item.clone();
        cloned.setAmount(item.getAmount() * dropAmount);
        event.getDrops().add(cloned);
      }

      StackedMob.getStackedMobs().remove(sm.getUniqueId());
    } else if (event.getCause() == StackedMobDeathCause.PLAYER) {
      final StackedMob sm = event.getStackedMob();
      final int decremented = sm.decrementAndGet();

      if (decremented <= 0) {
        sm.destroyEntity();

        final StackedMobDrops drops = StackedMobDrops.getFromEntity(sm.getType());

        if (drops != null) {
          for (final ItemStack item : drops.getDrops()) {
            event.getDrops().add(item);
          }

          final int maxXp = drops.getMaxXp();
          if (maxXp == 0) {
            return;
          }

          final int lowXp = drops.getLowXp();

          final int xp;
          if (maxXp != lowXp) {
            xp = ThreadLocalRandom.current().nextInt(lowXp, maxXp);
          } else {
            xp = maxXp;
          }

          event.getPlayer().giveExp(xp);
        }

        StackedMob.getStackedMobs().remove(sm.getUniqueId());
        return;
      }

      sm.setCustomName();

      final StackedMobDrops drops = StackedMobDrops.getFromEntity(sm.getType());

      if (drops != null) {
        for (final ItemStack item : drops.getDrops()) {
          event.getDrops().add(item);
        }

        final int maxXp = drops.getMaxXp();
        if (maxXp == 0) {
          return;
        }

        final int lowXp = drops.getLowXp();

        final int xp;
        if (maxXp != lowXp) {
          xp = ThreadLocalRandom.current().nextInt(lowXp, maxXp);
        } else {
          xp = maxXp;
        }

        event.setXp(xp);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onStackedMobDeathEventItemsDrop(final StackedMobDeathEvent event) {
    final Location location = event.getLocation();

    for (final ItemStack item : event.getDrops()) {
      location.getWorld().dropItem(location, item);
    }

    final int xp = event.getXp();
    if (xp <= 0) {
      return;
    }

    final Player player = event.getPlayer();
    if (player == null) {
      return;
    }

    player.giveExp(xp);
  }

}
