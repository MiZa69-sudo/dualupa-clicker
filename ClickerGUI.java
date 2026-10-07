package ru.dualupa.clicker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClickerGUI {
    private final DuaLupaClicker plugin;
    private final ClickerManager manager;
    public static final String TITLE = "§5✦ §dШахта DUA LUPA §5✦";
    public static final String TOP_TITLE = "§e✦ Топ недели ✦";

    public ClickerGUI(DuaLupaClicker plugin, ClickerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player p) {
        ClickerPlayer cp = manager.get(p);
        if (cp == null) { p.sendMessage("§cСначала зарегистрируйся на сайте DUA LUPA."); return; }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack glass = item(Material.BLACK_STAINED_GLASS_PANE, "§0", null);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        inv.setItem(1, item(Material.CHEST, "§6§l💰 БАЛАНС", list(
            "§7Монет: §e" + cp.coins,
            "§7Всего: §e" + cp.totalEarned,
            "§7Престиж: §d" + cp.prestige
        )));
        inv.setItem(3, item(Material.ENCHANTED_BOOK, "§d§l✦ ШАХТА ✦", list(
            "§7Мир: §fПодземелье Бездны",
            "§7Неделя с: §f" + cp.weekStart
        )));
        inv.setItem(5, item(Material.EMERALD, "§b§l💎 РУБЛИ", list(
            "§7Донат-баланс: §a" + getRubles(p.getName()) + " ₽",
            "§7Трать на оффлайн 24/48ч"
        )));
        inv.setItem(7, item(Material.COMPASS, "§e§l📈 ТОП НЕДЕЛИ", list("§7Нажми, чтобы открыть")));

        inv.setItem(10, item(Material.IRON_PICKAXE, "§f§l⛏ КИРКА", list(
            "§7Уровень: §e" + cp.pickaxeLevel + "/" + ClickerPlayer.MAX_PICKAXE,
            "§7Монет за клик: §6" + cp.clickReward(),
            "§7Цена: §e" + cp.pricePickaxe() + " монет",
            "§7[Клик — прокачать]"
        )));
        inv.setItem(13, item(Material.valueOf(cp.oreMaterial()), "§6§l★ РУДА ★", list(
            "§7Кликай, чтобы копать",
            "§7Награда: §6" + cp.clickReward() + " монет",
            cp.critChancePercent() > 0 ? "§7Шанс крита: §a" + cp.critChancePercent() + "%" : null
        )));
        inv.setItem(16, item(Material.LIME_DYE, "§a§l🍀 УДАЧА", list(
            "§7Уровень: §e" + cp.luckLevel + "/" + ClickerPlayer.MAX_LUCK,
            "§7Крит ×2: §a" + cp.critChancePercent() + "%",
            "§7Цена: §e" + cp.priceLuck() + " монет"
        )));

        inv.setItem(19, item(Material.DISPENSER, "§b§l🤖 АВТОКИРКА", list(
            "§7Уровень: §e" + cp.autoLevel + "/" + ClickerPlayer.MAX_AUTO,
            "§7Монет/сек: §6" + Math.round(cp.autoLevel * cp.bonus()),
            "§7Оффлайн до §f" + cp.offlineHours + "ч",
            "§7Цена: §e" + cp.priceAuto() + " монет"
        )));
        inv.setItem(25, item(Material.CHEST, "§6§l🎁 СУНДУК", list(
            "§7Уровень: §e" + cp.chestLevel + "/" + ClickerPlayer.MAX_CHEST,
            "§7Награда: §6×10 монет",
            "§7Цена: §e" + cp.priceChest() + " монет"
        )));

        inv.setItem(28, item(Material.NETHER_STAR, "§d§l⭐ ПРЕСТИЖ", list(
            "§7Текущий: §e" + cp.prestige,
            "§7Бонус: §a+" + (cp.prestige * 10) + "%",
            "§7Цена: §e" + ClickerPlayer.PRESTIGE_THRESHOLD + " монет",
            cp.coins >= ClickerPlayer.PRESTIGE_THRESHOLD ? "§a[Клик — сброс и бонус]" : "§cНедостаточно монет"
        )));
        inv.setItem(34, item(Material.CLOCK, "§e§l⏳ ОФФЛАЙН", list(
            "§7Максимум: §f" + cp.offlineHours + "ч",
            "§7Накопит: §6" + cp.offlineReward(cp.offlineHours * 3600L),
            "§7Апгрейд за рубли: §a24ч / 48ч"
        )));

        boolean dailyReady = manager.claimDailyAvailable(p);
        inv.setItem(37, item(Material.PAPER, "§d§l📅 ЕЖЕДНЕВКА", list(
            "§7Стрик: §e" + cp.dailyStreak + "/7",
            dailyReady ? "§a[Забрать награду]" : "§cУже получено сегодня",
            cp.dailyStreak == 6 ? "§dЗавтра — редкий кейс!" : null
        )));
        inv.setItem(43, item(Material.BARRIER, "§c§l🚪 ВЫХОД", list("§7Или ESC")));

        if (manager.bossActive) {
            inv.setItem(22, item(Material.WITHER_SKELETON_SKULL, "§4§l☠ ГОЛЕМ", list(
                "§cКликай!",
                "§7Ты нанёс: §f" + manager.bossHits.getOrDefault(p.getUniqueId(), 0)
            )));
        }

        p.openInventory(inv);
    }

    public void openTop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TOP_TITLE);
        ItemStack glass = item(Material.BLACK_STAINED_GLASS_PANE, "§0", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        inv.setItem(13, item(Material.WRITTEN_BOOK, "§e§l🏆 ТОП НЕДЕЛИ", split(manager.buildTopText())));
        inv.setItem(22, item(Material.ARROW, "§fНазад", null));
        p.openInventory(inv);
    }

    public static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null) m.setLore(lore);
            is.setItemMeta(m);
        }
        return is;
    }

    public static List<String> list(String... lines) {
        List<String> l = new ArrayList<>();
        for (String s : lines) if (s != null) l.add(s);
        return l;
    }

    public static List<String> split(String text) {
        List<String> l = new ArrayList<>();
        for (String s : text.split("\n")) l.add(s);
        return l;
    }

    private int getRubles(String nickname) {
        try (var ps = plugin.getDatabase().prepare("SELECT donate_balance FROM users WHERE LOWER(nickname)=LOWER(?)")) {
            ps.setString(1, nickname);
            var rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }
}
