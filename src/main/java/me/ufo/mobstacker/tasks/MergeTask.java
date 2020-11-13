package me.ufo.mobstacker.tasks;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public final class MergeTask implements Runnable {

  private final MSPlugin plugin;

  public MergeTask(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    // if spawning is enabled/disabled
    final boolean after = plugin.isSpawning();
    plugin.setSpawning(false);

    if (plugin.isDebugging()) {
      plugin.getLogger().info("== MERGING START ==");
    }

    int failed = 0;
    int merge = 0;
    int dead = 0;

    final Map<UUID, StackedMob> stackedMobs = StackedMob.getStackedMobs();
    final ObjectOpenHashSet<UUID> toRemove = new ObjectOpenHashSet<>();
    final Iterator<Map.Entry<UUID, StackedMob>> mobIterator = stackedMobs.entrySet().iterator();

    try {
      while (mobIterator.hasNext()) {
        final Map.Entry<UUID, StackedMob> entry = mobIterator.next();
        /*try {
          entry = mobIterator.next();
        } catch (final IndexOutOfBoundsException | NoSuchElementException | NullPointerException e) {
          if (plugin.isDebugging()) {
            plugin.getLogger().info("NON-FATAL ERROR (IGNORE): [MergeTask L38] " +
                                    e.getClass().getSimpleName());
            //e.printStackTrace();
          }
          failed++;
          continue;
        }*/

        final StackedMob current = entry.getValue();

        if (current == null) {
          failed++;
          mobIterator.remove();
          continue;
        }

        if (toRemove.remove(entry.getKey())) {
          merge++;
          mobIterator.remove();
          continue;
        }

        if (current.getEntity().isDead()) {
          dead++;
          mobIterator.remove();
          continue;
        }

        final ObjectOpenHashSet<StackedMob> nearby;
        try {
          nearby = StackedMob.getAllByDistance(current, 8);
        } catch (final IndexOutOfBoundsException | NoSuchElementException e) {
          if (plugin.isDebugging()) {
            plugin.getLogger().info("NON-FATAL ERROR (IGNORE): [MergeTask L72] " +
                                    e.getClass().getSimpleName());
          }
          failed++;
          continue;
        } catch (final NullPointerException e) {
          failed++;
          continue;
        }

        if (!nearby.isEmpty()) {
          for (final StackedMob near : nearby) {
            current.addAndGet(near.getStackedAmount());
            toRemove.add(near.getUniqueId());
            //near.destroyEntity();
            merge++;
          }

          plugin.syncTask(() -> {
            current.setCustomName();

            for (final StackedMob near : nearby) {
              near.destroyEntity();
            }
          });
        }
      }

      // clean up
      for (final UUID uuid : toRemove) {
        final StackedMob stackedMob = stackedMobs.get(uuid);

        if (stackedMob == null) {
          continue;
        }

        stackedMob.destroyEntity();
        stackedMobs.remove(uuid);
      }
    } catch (final Throwable throwable) {
      if (plugin.isDebugging()) {
        plugin.getLogger().info("NON-CAUGHT ERROR");
        throwable.printStackTrace();
      }
    }

    if (plugin.isDebugging()) {
      plugin.getLogger().info("failed: " + failed);
      plugin.getLogger().info("merge: " + merge);
      plugin.getLogger().info("dead: " + dead);
      plugin.getLogger().info("current: " + stackedMobs.size());
      plugin.getLogger().info("== MERGING END ==");
    }

    plugin.setSpawning(after);
  }

}
