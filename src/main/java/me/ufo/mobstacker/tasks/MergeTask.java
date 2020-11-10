package me.ufo.mobstacker.tasks;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public final class MergeTask implements Runnable {

  private final MSPlugin instance;

  public MergeTask(final MSPlugin instance) {
    this.instance = instance;
  }

  @Override
  public void run() {
    if (instance.isDebug()) {
      instance.getLogger().info("== MERGING START ==");
    }

    int failed = 0;
    int merge = 0;
    int dead = 0;

    try {
      final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
        StackedMob.getStackedMobs().entrySet().iterator();

      final ObjectOpenHashSet<UUID> toRemove = new ObjectOpenHashSet<>();

      while (mobIterator.hasNext()) {
        final Map.Entry<UUID, StackedMob> entry;
        try {
          entry = mobIterator.next();
        } catch (final IndexOutOfBoundsException | NoSuchElementException | NullPointerException e) {
          if (instance.isDebug()) {
            instance.getLogger()
              .info("NON-FATAL ERROR (IGNORE): [MergeTask L38] " + e.getClass().getSimpleName());
            //e.printStackTrace();
          }
          failed++;
          continue;
        }

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

        //final long now = System.nanoTime();
        final ObjectOpenHashSet<StackedMob> nearby;
        try {
          nearby = StackedMob.getAllByDistance(current, 8);
        } catch (final IndexOutOfBoundsException | NoSuchElementException e) {
          if (instance.isDebug()) {
            instance.getLogger()
              .info("NON-FATAL ERROR (IGNORE): [MergeTask L72] " + e.getClass().getSimpleName());
            //e.printStackTrace();
          }
          failed++;
          continue;
        } catch (final NullPointerException e) {
          failed++;
          continue;
        }
        //final long complete = System.nanoTime();

        //instance.getLogger().info("StackedMob#getAllByDistance took " + new DecimalFormat("#.##########")
        // .format((double) (complete - now) / 1_000_000_000.0) + " seconds.");

        if (!nearby.isEmpty()) {
          for (final StackedMob near : nearby) {
            current.addAndGet(near.getStackedAmount());
            toRemove.add(near.getUniqueId());
          }

          instance.getScheduler().runTask(instance, () -> {
            current.setCustomName();

            for (final StackedMob near : nearby) {
              near.destroyEntity();
            }
          });
        }
      }

      for (final UUID uuid : toRemove) {
        final StackedMob stackedMob = StackedMob.getByEntityId(uuid);

        if (stackedMob == null) {
          continue;
        }

        stackedMob.destroyEntity();
        StackedMob.getStackedMobs().remove(uuid);
      }
    } catch (final Throwable throwable) {
      if (instance.isDebug()) {
        instance.getLogger().info("NON-CAUGHT ERROR");
        throwable.printStackTrace();
      }
    }

    if (instance.isDebug()) {
      instance.getLogger().info("failed: " + failed);
      instance.getLogger().info("merge: " + merge);
      instance.getLogger().info("dead: " + dead);
      instance.getLogger().info("current: " + StackedMob.getStackedMobs().size());
      instance.getLogger().info("== MERGING END ==");
    }
  }

}
