package me.ufo.mobstacker.tasks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;

public final class ClearTask implements Runnable {

  private final MSPlugin instance;

  public ClearTask(final MSPlugin instance) {
    this.instance = instance;
  }

  @Override
  public void run() {
    final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
      StackedMob.getStackedMobs().entrySet().iterator();

    while (mobIterator.hasNext()) {
      final StackedMob current = mobIterator.next().getValue();
      current.destroyEntity();
      mobIterator.remove();
    }
  }

}
