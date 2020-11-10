package me.ufo.mobstacker.tasks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ClearTask implements Runnable {

  private final MSPlugin instance;

  public ClearTask(final MSPlugin instance) {
    this.instance = instance;
  }

  @Override
  public void run() {
    if (instance.isDebug()) {
      instance.getLogger().info("== CLEAR START ==");
    }

    instance.getRemoving().set(true);

    final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
      StackedMob.getStackedMobs().entrySet().iterator();

    while (mobIterator.hasNext()) {
      final StackedMob current = mobIterator.next().getValue();
      current.destroyEntity();
      mobIterator.remove();
    }

    for (final World world : instance.getServer().getWorlds()) {
      for (final LivingEntity entity : world.getLivingEntities()) {
        if (entity instanceof Player || entity instanceof ArmorStand || entity.hasMetadata("NPC")) {
          continue;
        }

        if (entity.hasMetadata(StackedMob.METADATA_KEY)) {
          entity.removeMetadata(StackedMob.METADATA_KEY, instance);
        }

        entity.remove();
      }
    }

    instance.getRemoving().set(false);

    if (instance.isDebug()) {
      instance.getLogger().info("== CLEAR END ==");
    }
  }

}
