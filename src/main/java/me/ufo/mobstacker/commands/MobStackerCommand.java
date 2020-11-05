package me.ufo.mobstacker.commands;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import me.ufo.mobstacker.mob.StackedMob;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class MobStackerCommand implements CommandExecutor {

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String s,
                           final String[] args) {

    if (!sender.isOp()) {
      return false;
    }

    if ("debug".equalsIgnoreCase(args[0])) {
      int stacked = 0;
      for (final Map.Entry<UUID, StackedMob> entry : StackedMob.getStackedMobs().entrySet()) {
        stacked += entry.getValue().getStackedAmount();
      }

      sender.sendMessage("StackedMobs size: " + StackedMob.getStackedMobs().size());
      sender.sendMessage("Combined StackedMobs: " + stacked);
    } else if ("entities".equalsIgnoreCase(args[0])) {
      int other = 0;
      int living = 0;

      final StringBuilder sb = new StringBuilder("\n");
      for (final World world : Bukkit.getWorlds()) {
        sb.append(world.getName()).append("\n");
        for (final Entity entity : world.getEntities()) {
          if (entity instanceof LivingEntity) {
            living++;
          } else {
            other++;
          }
        }
        sb.append(" Living: ").append(living).append("\n").append(" Other: ").append(other).append("\n");
      }
      sb.append(" Total: ").append(other + living);

      sender.sendMessage(sb.toString());
    } else if ("killall".equalsIgnoreCase(args[0])) {
      if ("all".equalsIgnoreCase(args[1])) {
        final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
          StackedMob.getStackedMobs().entrySet().iterator();
        while (mobIterator.hasNext()) {
          final StackedMob stackedMob = mobIterator.next().getValue();

          stackedMob.destroyEntity();
          mobIterator.remove();
        }

        if (args.length == 3) {
          if ("-a".equalsIgnoreCase(args[2])) {
            for (final World world : Bukkit.getServer().getWorlds()) {
              for (final LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player) {
                  continue;
                }

                if (entity.getCustomName() == null) {
                  entity.remove();
                }
              }
            }
          }
        }
      } else {
        final EntityType entityType = EntityType.valueOf(args[1].toUpperCase());

        final Iterator<Map.Entry<UUID, StackedMob>> mobIterator =
          StackedMob.getStackedMobs().entrySet().iterator();

        while (mobIterator.hasNext()) {
          final StackedMob stackedMob = mobIterator.next().getValue();

          if (stackedMob.getEntity().getType() == entityType) {
            stackedMob.destroyEntity();
            mobIterator.remove();
          }
        }
      }
    }

    return false;
  }

}
