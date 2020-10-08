package me.ufo.mobstacker.mob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public enum StackedMobDrops {

  // TODO: better method?...
  ZOMBIE(new ArrayList<>()),
  SKELETON(new ArrayList<>()),
  CREEPER(new ArrayList<>()),
  PIG_ZOMBIE(new ArrayList<>()),
  BLAZE(new ArrayList<>()),
  IRON_GOLEM(new ArrayList<>()),
  VILLAGER(new ArrayList<>()),
  ENDERMAN(new ArrayList<>());

  private List<ItemStack> drops;

  StackedMobDrops(final List<ItemStack> drops) {
    this.drops = drops;
  }

  public List<ItemStack> getDrops() {
    return drops;
  }

  private void setDrops(final List<ItemStack> drops) {
    this.drops = drops;
  }

  public static void init() {
    for (final StackedMobDrops entity : StackedMobDrops.values()) {
      entity.setDrops(getDropsForEntity(entity));
    }
  }

  public static List<ItemStack> getDropsForEntity(final EntityType type) {
    switch (type) {
      default:
        return new ArrayList<>(0);

      case ZOMBIE: {
        return ZOMBIE.getDrops();
      }

      case SKELETON: {
        return SKELETON.getDrops();
      }

      case CREEPER: {
        return CREEPER.getDrops();
      }

      case PIG_ZOMBIE: {
        return PIG_ZOMBIE.getDrops();
      }

      case BLAZE: {
        return BLAZE.getDrops();
      }

      case IRON_GOLEM: {
        return IRON_GOLEM.getDrops();
      }

      case VILLAGER: {
        return VILLAGER.getDrops();
      }

      case ENDERMAN: {
        return ENDERMAN.getDrops();
      }
    }
  }

  private static List<ItemStack> getDropsForEntity(final StackedMobDrops type) {
    switch (type) {
      default:
        return new ArrayList<>(0);

      case ZOMBIE: {
        return new ArrayList<>(Arrays.asList(
          new ItemStack(Material.ROTTEN_FLESH, ThreadLocalRandom.current().nextInt(1, 10))
        ));
      }

      case SKELETON: {
        return new ArrayList<>(Arrays.asList(
          new ItemStack(Material.BONE, ThreadLocalRandom.current().nextInt(1, 10))
        ));
      }

      case CREEPER: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.TNT, 1)));
      }

      case PIG_ZOMBIE: {
        return new ArrayList<>(Arrays.asList(
          new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(1, 5))
        ));
      }

      case BLAZE: {
        return new ArrayList<>(Arrays.asList(
          new ItemStack(Material.BLAZE_ROD, ThreadLocalRandom.current().nextInt(1, 5))
        ));
      }

      case IRON_GOLEM: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.IRON_INGOT, 1)));
      }

      case VILLAGER: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.EMERALD, 1)));
      }

      case ENDERMAN: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.ENDER_PEARL)));
      }
    }
  }

}
