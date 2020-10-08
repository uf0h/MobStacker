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
  ZOMBIE,
  SKELETON,
  CREEPER,
  PIG_ZOMBIE,
  BLAZE,
  IRON_GOLEM,
  VILLAGER,
  ENDERMAN;

  private List<ItemStack> drops;
  private int lowXp;
  private int maxXp;

  public List<ItemStack> getDrops() {
    return drops;
  }

  private void setDrops(final List<ItemStack> drops) {
    this.drops = drops;
  }

  public int getLowXp() {
    return lowXp;
  }

  public void setLowXp(final int lowXp) {
    this.lowXp = lowXp;
  }

  public int getMaxXp() {
    return maxXp;
  }

  public void setMaxXp(final int maxXp) {
    this.maxXp = maxXp;
  }

  public static void init() {
    for (final StackedMobDrops entity : StackedMobDrops.values()) {
      entity.setDrops(getDropsForEntity(entity));
      entity.setLowXp(getLowXpForEntity(entity));
      entity.setMaxXp(getMaxXpForEntity(entity));
    }
  }

  public static StackedMobDrops getFromEntity(final EntityType type) {
    switch (type) {
      default:
        return null;

      case ZOMBIE:
        return ZOMBIE;

      case SKELETON:
        return SKELETON;

      case CREEPER:
        return CREEPER;

      case PIG_ZOMBIE:
        return PIG_ZOMBIE;

      case BLAZE:
        return BLAZE;

      case IRON_GOLEM:
        return IRON_GOLEM;

      case VILLAGER:
        return VILLAGER;

      case ENDERMAN:
        return ENDERMAN;
    }
  }

  private static int getLowXpForEntity(final StackedMobDrops type) {
    switch (type) {
      default:
        return 0;

      case ZOMBIE: {
        return 1;
      }

      case SKELETON: {
        return 1;
      }

      case CREEPER: {
        return 0;
      }

      case PIG_ZOMBIE: {
        return 5;
      }

      case BLAZE: {
        return 10;
      }

      case IRON_GOLEM: {
        return 15;
      }

      case VILLAGER: {
        return 15;
      }

      case ENDERMAN: {
        return 15;
      }
    }
  }

  private static int getMaxXpForEntity(final StackedMobDrops type) {
    switch (type) {
      default:
        return 0;

      case ZOMBIE: {
        return 3;
      }

      case SKELETON: {
        return 3;
      }

      case CREEPER: {
        return 0;
      }

      case PIG_ZOMBIE: {
        return 10;
      }

      case BLAZE: {
        return 15;
      }

      case IRON_GOLEM: {
        return 20;
      }

      case VILLAGER: {
        return 25;
      }

      case ENDERMAN: {
        return 20;
      }
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
