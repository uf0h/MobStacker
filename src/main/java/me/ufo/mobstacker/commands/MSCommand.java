package me.ufo.mobstacker.commands;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import me.ufo.mobstacker.MSPlugin;
import me.ufo.mobstacker.mob.StackedMob;
import me.ufo.shaded.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

// TODO: CLEANUP
public final class MSCommand implements CommandExecutor {

  private final MSPlugin plugin;

  public MSCommand(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String s,
                           final String[] args) {

    if (!sender.isOp()) {
      return false;
    }

    if ("debug".equalsIgnoreCase(args[0])) {
      if (StackedMob.getStackedMobs() == null) {
        sender.sendMessage("STACKEDMOBS MAP = NULL");
        return false;
      }

      int stacked = 0;

      final Iterator<Map.Entry<UUID, StackedMob>> i =
        StackedMob.getStackedMobs().entrySet().iterator();

      try {
        while (i.hasNext()) {
          stacked += i.next().getValue().getStackedAmount();
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }

      sender.sendMessage("StackedMobs size: " + StackedMob.getStackedMobs().size());
      sender.sendMessage("Combined StackedMobs: " + stacked);

      if (args.length == 2) {
        if ("-a".equalsIgnoreCase(args[1])) {
          final Iterator<Map.Entry<UUID, StackedMob>> iterator =
            StackedMob.getStackedMobs().entrySet().iterator();

          while (iterator.hasNext()) {
            final Map.Entry<UUID, StackedMob> entry = iterator.next();

            if (entry == null) {
              sender.sendMessage("null entry");
              continue;
            }

            sender.sendMessage(entry.getValue().toString());
          }
        }
      }
    } else if ("debugmode".equalsIgnoreCase(args[0])) {
      final boolean toggle = !plugin.isDebugging();
      plugin.setDebugging(toggle);

      sender.sendMessage((toggle ? "Enabled" : "Disabled") + " debug mode.");
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
        if (args.length == 2) {
          plugin.clearMobs(false);
        } else if (args.length == 3) {
          if ("-a".equalsIgnoreCase(args[2])) {
            plugin.clearMobs(true);
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

        if (args.length == 3) {
          if ("-a".equalsIgnoreCase(args[2])) {
            for (final World world : Bukkit.getServer().getWorlds()) {
              for (final LivingEntity entity : world.getLivingEntities()) {
                if (entity.getType() == entityType) {
                  entity.remove();
                }
              }
            }
          }
        }
      }
    } else if ("toggle".equalsIgnoreCase(args[0])) {
      sender.sendMessage("Spawners " + (plugin.toggleSpawning() ? "enabled." : "disabled."));
    } else if ("status".equalsIgnoreCase(args[0])) {
      sender.sendMessage("Spawning spawning: " + (plugin.isSpawning() ? "enabled" : "disabled"));
    } else if ("merge".equalsIgnoreCase(args[0])) {
      if (!(sender instanceof Player)) {
        return false;
      }

      final EntityType type = EntityType.fromName(args[1]);

      final Player player = (Player) sender;
      final Location location = player.getLocation();

      final ObjectOpenHashSet<StackedMob> mobs = new ObjectOpenHashSet<>();

      for (final Map.Entry<UUID, StackedMob> entry : StackedMob.getStackedMobs().entrySet()) {
        if (entry.getValue().getType() != type) {
          continue;
        }

        if (entry.getValue().getLocation().getChunk() == location.getChunk()) {
          mobs.add(entry.getValue());
        }
      }

      final StringBuilder sb = new StringBuilder("Merge-able [").append(type).append("] ").append(mobs.size()).append("):");

      for (final StackedMob mob : mobs) {
        final Location loc = mob.getLocation();

        sb.append("\n")
          .append("Type: ").append(mob.getType())
          .append(", Loc: ").append(loc.getBlockX()).append("|").append(loc.getBlockY()).append("|").append(loc.getBlockZ())
          .append(", Alive: ").append(!mob.getEntity().isDead() ? ChatColor.GREEN.toString() + "Yes" : ChatColor.RED.toString() + "No")
        .append(ChatColor.WHITE.toString());
      }

      player.sendMessage(sb.toString());

      /*int failed = 0;
      int merge = 0;
      int dead = 0;

      final Map<UUID, StackedMob> stackedMobs = StackedMob.getStackedMobs();
      final ObjectOpenHashSet<UUID> toRemove = new ObjectOpenHashSet<>();

      try {
        final Iterator<Map.Entry<UUID, StackedMob>> mobIterator = stackedMobs.entrySet().iterator();

        while (mobIterator.hasNext()) {
          final Map.Entry<UUID, StackedMob> entry;
          try {
            entry = mobIterator.next();
          } catch (final IndexOutOfBoundsException | NoSuchElementException | NullPointerException e) {
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

            plugin.syncTask(() -> {
              current.setCustomName();

              for (final StackedMob near : nearby) {
                near.destroyEntity();
              }
            });
          }
        }
      } catch (final Throwable throwable) {

      }

      player.sendMessage("failed: " + failed);
      player.sendMessage("merge: " + merge);
      player.sendMessage("dead: " + dead);
      player.sendMessage("current: " + StackedMob.getStackedMobs().size());
      player.sendMessage("== MERGING END ==");
      */
    } else if("cleartoggle".equalsIgnoreCase(args[0])) {
      plugin.clearing = !plugin.clearing;
      sender.sendMessage("Clearing " + (plugin.clearing ? "enabled" : "disabled"));
    } else if ("mergetoggle".equalsIgnoreCase(args[0])) {
      plugin.merging = !plugin.merging;
      sender.sendMessage("Merging " + (plugin.merging ? "enabled" : "disabled"));
    }

    return false;
  }

}
