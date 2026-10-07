package ru.dualupa.clicker;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ClickerListener implements Listener {
    private final DuaLupaClicker plugin;
    private final ClickerManager manager;
    private final ClickerGUI gui;

    public ClickerListener(DuaLupaClicker plugin, ClickerManager manager, ClickerGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        ClickerPlayer cp = manager.load(p);
        if (cp == null) { p.sendMessage("§cЗарегистрируйся на сайте DUA LUPA чтобы играть."); return; }
        p.teleport(p.getWorld().getSpawnLocation().add(0, 500, 0));
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.showTitle(net.kyori.adventure.title.Title.title(
            net.kyori.adventure.text.Component.text("DUA LUPA"),
            net.kyori.adventure.text.Component.text("Шахта"),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofMillis(1500),
                java.time.Duration.ofMillis(500)
            )
        ));
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) gui.open(p); }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { manager.unload(e.getPlayer()); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!title.equals(ClickerGUI.TITLE) && !title.equals(ClickerGUI.TOP_TITLE)) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        if (title.equals(ClickerGUI.TOP_TITLE)) {
            if (e.getSlot() == 22) gui.open(p);
            return;
        }

        int slot = e.getSlot();
        ClickerPlayer cp = manager.get(p);
        if (cp == null) return;

        switch (slot) {
            case 13: {
                long gain = manager.handleClick(p);
                p.playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 1.5f);
                p.sendActionBar(net.kyori.adventure.text.Component.text("§a+" + gain));
                gui.open(p);
                break;
            }
            case 10: { if (manager.upgrade(p, "pickaxe")) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f); gui.open(p); break; }
            case 19: { if (manager.upgrade(p, "auto"))    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f); gui.open(p); break; }
            case 16: { if (manager.upgrade(p, "luck"))    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f); gui.open(p); break; }
            case 25: { if (manager.upgrade(p, "chest"))   p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f); gui.open(p); break; }
            case 28: {
                if (manager.prestige(p)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
                    p.sendMessage("§dПрестиж повышен! Бонус: §a+" + (cp.prestige * 10) + "%");
                } else p.sendMessage("§cНужно " + ClickerPlayer.PRESTIGE_THRESHOLD + " монет");
                gui.open(p);
                break;
            }
            case 37: {
                int res = manager.claimDaily(p);
                if (res == -2) {
                    p.sendMessage("§d§l🏆 7 дней! РЕДКИЙ КЕЙС на сайте!");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                } else if (res > 0) {
                    p.sendMessage("§aЕжедневка: §e" + res + " монет");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.2f);
                } else p.sendMessage("§cУже получено сегодня");
                gui.open(p);
                break;
            }
            case 43: p.closeInventory(); break;
            case 7: gui.openTop(p); break;
            case 22: {
                if (manager.bossActive) {
                    manager.bossHits.merge(p.getUniqueId(), 1, Integer::sum);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 1.5f);
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§cУдар: " + manager.bossHits.get(p.getUniqueId())));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getView().getTitle().equals(ClickerGUI.TITLE)) {
            p.sendMessage("§7Открыть снова: §f/clicker");
        }
    }
}
