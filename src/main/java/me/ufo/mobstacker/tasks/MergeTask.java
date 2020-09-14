package me.ufo.mobstacker.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import org.bukkit.ChatColor;

public final class MergeTask implements Runnable {

  private final MSPlugin instance;

  public MergeTask(final MSPlugin instance) {
    this.instance = instance;
  }

  @Override
  public void run() {
    final List<UUID> toRemove = new ArrayList<>();
    final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
      StackedMob.getStackedMobs().entrySet().iterator();

    int merged = 0;
    while (mobIterator.hasNext()) {
      final StackedMob current = mobIterator.next().getValue();

      if (toRemove.contains(current.getUniqueId())) {
        mobIterator.remove();
        continue;
      }

      final List<StackedMob> nearbyStackedMobs = StackedMob.getAllByDistance(current, 8);
      for (final StackedMob nearby : nearbyStackedMobs) {
        current.addAndGet(nearby.getStackedAmount());
        toRemove.add(nearby.getUniqueId());
        merged++;
      }

      if (!nearbyStackedMobs.isEmpty()) {
        instance.getScheduler().runTask(instance, () -> {
          for (final StackedMob nearby : nearbyStackedMobs) {
            nearby.destroyEntity();
          }

          current.getEntity().setCustomName(
            ChatColor.GOLD.toString() + ChatColor.BOLD.toString() + "x" + current.getStackedAmount() + " " +
            ChatColor.YELLOW.toString() + ChatColor.BOLD.toString()  + current.getEntity().getType().name()
          );
        });
      }
    }

    //Bukkit.getLogger().info("Merged: " + merged);
    //Bukkit.getLogger().info("After merge, mobs left: " + StackedMob.getStackedMobs().size());
  }

}
