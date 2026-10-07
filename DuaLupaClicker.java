package ru.dualupa.clicker;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class DuaLupaClicker extends JavaPlugin {

    private static DuaLupaClicker instance;
    private Database db;
    private ClickerManager manager;
    private ClickerGUI gui;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        try {
            db = new Database(this);
            db.connect();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Не удалось подключиться к БД", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        manager = new ClickerManager(this, db);
        gui = new ClickerGUI(this, manager);

        getServer().getPluginManager().registerEvents(new ClickerListener(this, manager, gui), this);
        getCommand("clicker").setExecutor(new ClickerCommand(this, gui));
        getCommand("cadmin").setExecutor(new ClickerAdminCommand(this, manager));

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> manager.saveAll(), 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, () -> manager.startBossEvent(), 20L * 60 * 60, 20L * 60 * 60);
        Bukkit.getScheduler().runTaskTimer(this, () -> manager.checkWeeklyReward(), 20L * 60, 20L * 60);

        getLogger().info("DUA LUPA Clicker запущен");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.saveAll();
        if (db != null) db.disconnect();
    }

    public static DuaLupaClicker get() { return instance; }
    public ClickerManager getManager() { return manager; }
    public ClickerGUI getGui() { return gui; }
    public Database getDatabase() { return db; }
}
