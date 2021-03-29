package me.ufo.mobstacker;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import me.ufo.architect.util.Style;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

public final class Config {

  public static final Random RANDOM = new Random();

  public static int MAX_DEATHS = 100;
  public static int HIT_DELAY = 100;
  private static int MIN_SPAWN = 2;
  private static int MAX_SPAWN = 5;
  private static String MOB_NAME = "&6&lx{amount} &e&l{mob}";

  private Config() {
    throw new UnsupportedOperationException("This class cannot be instantiated.");
  }

  private static FileConfiguration config(final MSPlugin plugin) {
    final File configFile = new File(plugin.getDataFolder().getPath() + "/config.yml");

    if (!configFile.exists()) {
      plugin.saveResource("config.yml", false);
    }

    final FileConfiguration config = new YamlConfiguration();

    try {
      config.load(configFile);
    } catch (final IOException | InvalidConfigurationException e) {
      e.printStackTrace();
      return null;
    }

    return config;
  }

  public static void load(final MSPlugin plugin) {
    final FileConfiguration config = config(plugin);

    if (config == null) {
      plugin.getServer().getPluginManager().disablePlugin(plugin);
      return;
    }

    MIN_SPAWN = config.getInt("spawner.min-spawn", 2);
    MAX_SPAWN = config.getInt("spawner.max-spawn", 5);

    MOB_NAME = Style.translate(config.getString("stacked-mob.name", "&6&lx{amount} &e&l{mob}"));
    MAX_DEATHS = config.getInt("stacked-mob.max-death-on-fall", 100);
    HIT_DELAY = config.getInt("stacked-mob.hit-delay", 100);
  }

  public static int getRandomSpawnAmount() {
    if (MIN_SPAWN == MAX_SPAWN || MIN_SPAWN < MAX_SPAWN) {
      return MIN_SPAWN;
    }

    return getRandomIntegerBetweenBounds(MIN_SPAWN, MAX_SPAWN);
  }

  public static String getStackedMobName(final int amount, final Entity entity) {
    String out = Style.replace(MOB_NAME, "{amount}", "" + amount);
    out = Style.replace(out, "{mob}", entity.getType().name());

    return out;
  }

  public static int getRandomIntegerBetweenBounds(final int min, final int max) {
    return RANDOM.nextInt(max - min + 1) + min;
  }

}
