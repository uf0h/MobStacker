package me.ufo.mobstacker.listeners;

import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.events.StackedMobDeathEvent;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.mobstacker.mob.StackedMobDeathCause;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class StackedMobListener implements Listener {

  private final MSPlugin plugin;

  public StackedMobListener(final MSPlugin plugin) {
    this.plugin = plugin;
  }

//  @EventHandler(priority = EventPriority.LOWEST)
//  public void onStackedMobDeathEvent(final StackedMobDeathEvent event) {
//    if (event.getCause() == StackedMobDeathCause.FALL) {
//      final StackedMob sm = event.getStackedMob();
//
//      sm.destroyEntity();
//
//      StackedMob.getStackedMobs().remove(sm.getUniqueId());
//    } else if (event.getCause() == StackedMobDeathCause.PLAYER) {
//      final StackedMob sm = event.getStackedMob();
//      final int decremented = sm.decrementAndGet();
//
//      if (decremented <= 0) {
//        sm.destroyEntity();
//
//        StackedMob.getStackedMobs().remove(sm.getUniqueId());
//        return;
//      }
//
//      sm.setCustomName();
//    }
//  }

  /*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

    // TODO: NOTE remove
    player.giveExp(player.hasPermission("vicious.booster.doubleexp") ? xp * 2 : xp);
  }*/

}
