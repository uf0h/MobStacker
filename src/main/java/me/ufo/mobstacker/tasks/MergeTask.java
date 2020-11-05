package me.ufo.mobstacker.tasks;

import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public final class MergeTask implements Runnable {

  private final MSPlugin instance;

  public MergeTask(final MSPlugin instance) {
    this.instance = instance;
  }

  @Override
  public void run() {
    final ObjectIterator<Object2ObjectMap.Entry<UUID, StackedMob>> mobIterator =
      StackedMob.getStackedMobs().object2ObjectEntrySet().iterator();

    final ObjectOpenHashSet<UUID> toRemove = new ObjectOpenHashSet<>();

    while (mobIterator.hasNext()) {
      final Object2ObjectMap.Entry<UUID, StackedMob> entry = mobIterator.next();

      final StackedMob current = entry.getValue();

      if (toRemove.remove(entry.getKey()) || current.getEntity() == null || current.getEntity().isDead()) {
        mobIterator.remove();
        continue;
      }

      final ObjectOpenHashSet<StackedMob> nearby = StackedMob.getAllByDistance(current, 8);

      for (final StackedMob near : nearby) {
        current.addAndGet(near.getStackedAmount());
        toRemove.add(near.getUniqueId());
      }

      if (!nearby.isEmpty()) {
        instance.getScheduler().runTask(instance, () -> {
          for (final StackedMob near : nearby) {
            near.destroyEntity();
          }

          current.getEntity().setCustomName(
            instance.getStackedMobName(current.getStackedAmount(), current.getEntity())
          );
        });
      }
    }
  }

}
