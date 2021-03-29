package me.ufo.mobstacker.mob;

import me.ufo.mobstacker.Config;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum StackedMobDrops {

  // TODO: better method?...
  PIG,
  COW,
  ZOMBIE,
  SKELETON,
  CREEPER,
  PIG_ZOMBIE,
  BLAZE,
  IRON_GOLEM,
  VILLAGER,
  ENDERMAN,
  SILVERFISH,
  ENDERMITE;

  private int minXp;
  private int maxXp;

  public int getMinXp() {
    return minXp;
  }

  public void setMinXp(final int minXp) {
    this.minXp = minXp;
  }

  public int getMaxXp() {
    return maxXp;
  }

  public void setMaxXp(final int maxXp) {
    this.maxXp = maxXp;
  }

  public static void init() {
    for (final StackedMobDrops entity : StackedMobDrops.values()) {
      entity.setMinXp(getLowXpForEntity(entity));
      entity.setMaxXp(getMaxXpForEntity(entity));
    }
  }

  public static StackedMobDrops getFromEntity(final EntityType type) {
    switch (type) {
      default:
        return null;

      case PIG:
        return PIG;

      case COW:
        return COW;

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

      case SILVERFISH:
        return SILVERFISH;

      case ENDERMITE:
        return ENDERMITE;
    }
  }

  private static int getLowXpForEntity(final StackedMobDrops type) {
    switch (type) {
      default:
        return 0;

      case PIG: {
        return 1;
      }

      case COW: {
        return 1;
      }

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

      case SILVERFISH: {
        return 10;
      }

      case ENDERMITE: {
        return 10;
      }
    }
  }

  private static int getMaxXpForEntity(final StackedMobDrops type) {
    switch (type) {
      default:
        return 0;

      case PIG: {
        return 1;
      }

      case COW: {
        return 1;
      }

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

      case SILVERFISH: {
        return 20;
      }

      case ENDERMITE: {
        return 20;
      }
    }
  }

  public List<ItemStack> getDrops() {
    return this.getDrops(1);
  }

  public List<ItemStack> getDrops(final int deaths) {
    switch (this) {
      default:
        return new ArrayList<>(0);

      case PIG: {
        return new ArrayList<>(Arrays.asList(
            new ItemStack(Material.GRILLED_PORK, Config.getRandomIntegerBetweenBounds(1, 3) * deaths)
        ));
      }

      case COW: {
        return new ArrayList<>(Arrays.asList(
            new ItemStack(Material.COOKED_BEEF, Config.getRandomIntegerBetweenBounds(1, 3) * deaths)
        ));
      }

      case ZOMBIE: {
        return new ArrayList<>(Arrays.asList(
            new ItemStack(Material.ROTTEN_FLESH, Config.getRandomIntegerBetweenBounds(1, 10) * deaths)
        ));
      }

      case SKELETON: {
        return new ArrayList<>(Arrays.asList(
            new ItemStack(Material.BONE, Config.getRandomIntegerBetweenBounds(1, 10) * deaths)
        ));
      }

      case CREEPER: {
        final int chance = Config.getRandomIntegerBetweenBounds(1, 3); // 1 in 2

        if (chance != 1) {
          //chance = ThreadLocalRandom.current().nextInt(1, 3);

          //if (chance != 1) {
          return new ArrayList<>(0);
          //}
        }

        final int half = deaths / 2;

        if (half <= 1) {
          return new ArrayList<>(Arrays.asList(new ItemStack(Material.TNT, deaths)));
        } else {
          return new ArrayList<>(Arrays.asList(new ItemStack(Material.TNT, half)));
        }
      }

      case PIG_ZOMBIE: {
        return new ArrayList<>(Arrays.asList(
            new ItemStack(Material.GOLD_INGOT, Config.getRandomIntegerBetweenBounds(1, 5) * deaths)
        ));
      }

      case BLAZE: {
        return new ArrayList<>(Arrays.asList(
            new ItemStack(Material.BLAZE_ROD, Config.getRandomIntegerBetweenBounds(1, 5) * deaths)
        ));
      }

      case IRON_GOLEM: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.IRON_INGOT, 1 * deaths)));
      }

      case VILLAGER: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.EMERALD, 1 * deaths)));
      }

      case ENDERMAN: {
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.ENDER_PEARL, 1 * deaths)));
      }

      case SILVERFISH:
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.NETHER_STAR, 1 * deaths)));

      case ENDERMITE:
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.TNT, 48 * deaths)));
    }
  }

  /*public static List<ItemStack> getDropsForEntity(final StackedMobDrops type) {
    switch (type) {
      default:
        return new ArrayList<>(0);

      case PIG: {
        return new ArrayList<>(Arrays.asList(
          new ItemStack(Material.GRILLED_PORK, ThreadLocalRandom.current().nextInt(1, 3))
        ));
      }

      case COW: {
        return new ArrayList<>(Arrays.asList(
          new ItemStack(Material.COOKED_BEEF, ThreadLocalRandom.current().nextInt(1, 3))
        ));
      }

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
        final int chance = ThreadLocalRandom.current().nextInt(1, 5); // 1 in 4

        if (chance == 1) {
          return new ArrayList<>(Arrays.asList(new ItemStack(Material.TNT, 1)));
        } else {
          return new ArrayList<>(0);
        }
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

      case SILVERFISH:
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.NETHER_STAR)));

      case ENDERMITE:
        return new ArrayList<>(Arrays.asList(new ItemStack(Material.TNT, 48)));
    }
  }*/

}
