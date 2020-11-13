package me.ufo.mobstacker.tasks;

import me.ufo.mobstacker.MSPlugin;

public final class ClearTask implements Runnable {

  private final MSPlugin plugin;

  public ClearTask(final MSPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    if (plugin.isDebugging()) {
      plugin.getLogger().info("== CLEARING STACKEDMOBS ==");
    }

    plugin.clearMobs(true);
  }

}
