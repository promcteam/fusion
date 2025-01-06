package studio.magemonkey.fusion.commands;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.sql.DatabaseType;
import studio.magemonkey.fusion.cfg.sql.SQLManager;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.Profession;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.gui.BrowseGUI;
import studio.magemonkey.fusion.gui.IngredientUsage;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;
import studio.magemonkey.fusion.util.LevelFunction;
import studio.magemonkey.fusion.util.Utils;

import java.util.*;

public class Commands implements CommandExecutor, TabCompleter {

    private final Map<String, ConfirmationAction> confirmation = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        Fusion instance = Fusion.getInstance();
        if ((args.length == 2) || (args.length == 3)) {
            if (args[0].equalsIgnoreCase("use")) {
                if (!Utils.hasCraftingUsePermission(sender, null)) {
                    return true;
                }
                String[] professionArgs = args[1].split(":");
                String profession = professionArgs[0];
                Category category = null;

                ProfessionGuiRegistry eq = ProfessionsCfg.getGuiMap().get(profession);

                if (professionArgs.length == 2 && Fusion.getInstance().checkPermission(sender, "fusion.craft.use.categories")) {
                    category = ProfessionsCfg.getTable(profession).getCategory(professionArgs[1]);
                } else if (professionArgs.length == 2) {
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noPermission", sender);
                    return true;
                }

                if (eq == null) {
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.notACrafting",
                            sender,
                            new MessageData("name", args[1]),
                            new MessageData("sender", sender));
                    return true;
                }
                if (args.length == 3) {
                    if (!instance.checkPermission(sender, "fusion.admin.use")) {
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        CodexEngine.get().getMessageUtil().sendMessage("notAPlayer",
                                sender,
                                new MessageData("name", args[2]),
                                new MessageData("sender", sender));
                        return true;
                    }

                    //TODO ?? Make sure they have unlocked this crafting menu

                    openGui(target, eq, category);
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.useConfirmOther",
                            sender,
                            new MessageData("craftingInventory", eq),
                            new MessageData("sender", sender),
                            new MessageData("target", target));
                    return true;
                } else {
                    if (sender instanceof Player player) {
                        if (!Utils.hasCraftingUsePermission(sender, eq.getProfession())) {
                            return true;
                        }
                        //Make sure they have unlocked this crafting menu
                        if (!PlayerLoader.getPlayer(player).hasProfession(eq.getProfession())) {
                            if (player.isOp()) {
                                openGui(player, eq, category);
                                CodexEngine.get().getMessageUtil().sendMessage("fusion.useConfirm",
                                        sender,
                                        new MessageData("craftingInventory", eq),
                                        new MessageData("player", sender));
                                return true;
                            }
                            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.notUnlocked", sender);
                            return true;
                        }
                        openGui(player, eq, category);
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.useConfirm",
                                sender,
                                new MessageData("craftingInventory", eq),
                                new MessageData("player", sender));
                        return true;
                    } else {
                        CodexEngine.get()
                                .getMessageUtil()
                                .sendMessage("fusion.help", sender, new MessageData("sender", sender),
                                        new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                    }
                }
            } else if (args[0].equalsIgnoreCase("master")) {
                if (sender instanceof Player player) {
                    String guiName = args[1];
                    CraftingTable table = ProfessionsCfg.getTable(guiName);
                    if (table == null) {
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.notACrafting",
                                sender,
                                new MessageData("name", args[1]),
                                new MessageData("sender", sender));
                        return true;
                    }

                    if (PlayerLoader.getPlayer(((Player) sender).getUniqueId()).hasMastered(table.getName())) {
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.error.alreadyMastered",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                        return true;
                    }

                    if (LevelFunction.getLevel(player, table) < table.getMasteryUnlock()) {
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMasteryLevel",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                        return true;
                    }

                    if (!CodexEngine.get().getVault().canPay(player, table.getMasteryFee())) {
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMasteryFunds",
                                sender,
                                new MessageData("sender", sender),
                                new MessageData("craftingTable", table));
                        return true;
                    }
                    FusionAPI.getEventServices().getProfessionService().masterProfession(table.getName(), player, true);
                    PlayerLoader.getPlayer(((Player) sender).getUniqueId()).setMastered(table.getName(), true);
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.mastered",
                            sender,
                            new MessageData("sender", sender),
                            new MessageData("craftingTable", table));
                    return true;
                } else {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("fusion.help", sender, new MessageData("sender", sender),
                                    new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            } else if (args[0].equalsIgnoreCase("forget")) {
                if (sender instanceof Player player) {
                    CraftingTable table = ProfessionsCfg.getTable(args[1]);
                    if (table == null) {
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.notACrafting",
                                sender,
                                new MessageData("name", args[1]),
                                new MessageData("sender", sender));
                        return true;
                    }
                    ConfirmationAction action =
                            () -> FusionAPI.getEventServices().getProfessionService().leaveProfession(table, player);

                    confirmation.put(player.getUniqueId().toString(), action);
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.forget.confirm",
                            sender,
                            new MessageData("sender", sender),
                            new MessageData("craftingTable", table));

                    Bukkit.getScheduler().runTaskLater(Fusion.getInstance(),
                            () -> confirmation.remove(player.getUniqueId().toString()), 15 * 20L);

                    return true;
                } else {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("fusion.help", sender, new MessageData("sender", sender),
                                    new MessageData("text", label + " " + StringUtils.join(args, ' ')));
                }
            } else if (args[0].equalsIgnoreCase("storage")) {
                String storage = args[1];
                DatabaseType type =
                        DatabaseType.valueOf(Objects.requireNonNull(Cfg.getConfig())
                                .getString("storage.type", "LOCALE")
                                .toUpperCase());
                switch (storage.toLowerCase()) {
                    case "local":
                        if (type == DatabaseType.LOCAL) {
                            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.alreadyUsedStorage",
                                    sender,
                                    new MessageData("storage", storage));
                            return true;
                        }
                        SQLManager.swapToLocal();
                        CodexEngine.get()
                                .getMessageUtil()
                                .sendMessage("fusion.storageChanged", sender, new MessageData("storage", storage));
                        break;
                    case "sql":
                        if (type == DatabaseType.MARIADB || type == DatabaseType.MYSQL) {
                            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.alreadyUsedStorage",
                                    sender,
                                    new MessageData("storage", storage));
                            return true;
                        }
                        SQLManager.swapToSql();
                        CodexEngine.get()
                                .getMessageUtil()
                                .sendMessage("fusion.storageChanged", sender, new MessageData("storage", storage));
                        break;
                    default:
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.error.invalidStorage",
                                sender,
                                new MessageData("storage", storage));
                        break;
                }
                return true;
            } else if (args[0].equalsIgnoreCase("join")) {
                if (sender instanceof Player player) {
                    if (ProfessionsCfg.getGuiMap().containsKey(args[1])) {
                        BrowseGUI.joinProfession(player, ProfessionsCfg.getGUI(args[1]));
                    } else {
                        CodexEngine.get()
                                .getMessageUtil()
                                .sendMessage("fusion.notACrafting",
                                        sender,
                                        new MessageData("name", args[1]),
                                        new MessageData("sender", sender));
                    }
                } else {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                }
                return true;
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("browse")) {
                if (!(sender instanceof Player player)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                if (player.isPermissionSet("fusion.browse") && !player.hasPermission("fusion.browse")) {
                    return true;
                }
                BrowseGUI.open(player);
                return true;
            } else if (args[0].equalsIgnoreCase("show")) {
                if (!(sender instanceof Player player)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                if (Fusion.getInstance().checkPermission(sender, "fusion.show")) {
                    IngredientUsage.showIngredientUsage(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("level")) {
                if (!(sender instanceof Player)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                for (Profession profession : PlayerLoader.getPlayer(((Player) sender).getUniqueId()).getProfessions()) {
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.level.format", sender,
                            new MessageData("category", profession.getName()),
                            new MessageData("level", profession.getLevel()),
                            new MessageData("experience",
                                    PlayerLoader.getPlayer(((Player) sender).getUniqueId())
                                            .getExperience(profession)));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("auto")) {
                if (!(sender instanceof Player player)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
                    return true;
                }
                if (Cfg.craftingQueue) {
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.error.autoDisabled", sender);
                    return true;
                }
                if (!instance.checkPermission(sender, "fusion.auto")) {
                    return true;
                }

                boolean autoOn = PlayerLoader.getPlayer(player).isAutoCrafting();

                PlayerLoader.getPlayer(player).setAutoCrafting((autoOn = !autoOn));
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("fusion.autoToggle", player, new MessageData("state", autoOn ? "on" : "off"));

                return true;
            } else if (args[0].equalsIgnoreCase("confirm")) {
                String id = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";

                if (confirmation.containsKey(id)) {
                    confirmation.get(id).doAction();
                    confirmation.remove(id);
                } else {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("fusion.nothingToConfirm", sender, new MessageData("sender", sender));
                }

                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!instance.checkPermission(sender, "fusion.reload")) {
                    return true;
                }
                instance.closeAll();
                instance.reloadConfig();
                instance.reloadLang();
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("fusion.reload", sender, new MessageData("sender", sender));
                return true;
            }
        }
        CodexEngine.get().getMessageUtil().sendMessage("fusion.help", sender, new MessageData("sender", sender),
                new MessageData("text", label + " " + StringUtils.join(args, ' ')));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        List<String> entries = new ArrayList<>();
        if (args.length == 1) {
            if ("browse".startsWith(args[0])) entries.add("browse");
            if ("level".startsWith(args[0])) entries.add("level");
            if ("confirm".startsWith(args[0])) entries.add("confirm");
            if ("use".startsWith(args[0])) entries.add("use");
            if ("master".startsWith(args[0])) entries.add("master");
            if ("forget".startsWith(args[0])) entries.add("forget");
            if ("join".startsWith(args[0])) entries.add("join");
            if (sender.hasPermission("fusion.admin.use") && "storage".startsWith(args[0]))
                entries.add("storage");
            if (sender.hasPermission("fusion.auto") && "auto".startsWith(args[0]))
                entries.add("auto");
            if (sender.hasPermission("fusion.reload") && "reload".startsWith(args[0]))
                entries.add("reload");
            if (sender.hasPermission("fusion.show") && "show".startsWith(args[0]))
                entries.add("show");
        } else if (args.length == 2) {
            List<Profession> professions =
                    new ArrayList<>(PlayerLoader.getPlayer(((Player) sender).getUniqueId()).getProfessions());
            if (args[0].equalsIgnoreCase("use")) {
                for (String name : professions.stream().map(Profession::getName).toList()) {
                    if (name.startsWith(args[1])) entries.add(name);

                }
                if (sender.hasPermission("fusion.craft.use.categories") && args[1].contains(":")) {
                    String profession = args[1].split(":")[0];
                    if (ProfessionsCfg.getGuiMap().containsKey(profession)) {
                        for (String category : ProfessionsCfg.getTable(profession).getCategories().keySet()) {
                            if ((profession + ":" + category).startsWith(args[1]))
                                entries.add((profession + ":" + category));
                        }
                    }
                }
            } else if (args[0].equalsIgnoreCase("master")) {
                for (String name : professions.stream()
                        .filter(Profession::isMastered)
                        .map(Profession::getName)
                        .toList()) {
                    if (name.startsWith(args[1])) entries.add(name);
                }
            } else if (args[0].equalsIgnoreCase("forget")) {
                for (String name : ProfessionsCfg.getMap().keySet()) {
                    if (name.startsWith(args[1])) entries.add(name);
                }
            } else if (args[0].equalsIgnoreCase("join")) {
                for (String name : ProfessionsCfg.getGuiMap().keySet()) {
                    if (!professions.stream().map(Profession::getName).toList().contains(name)) {
                        if (name.startsWith(args[1])) entries.add(name);
                    }
                }
            } else if (args[0].equalsIgnoreCase("storage") && sender.hasPermission("fusion.admin")) {
                if ("local".startsWith(args[1])) entries.add("local");
                if ("sql".startsWith(args[1])) entries.add("sql");
            }
        } else if (args.length == 3) {
            if (sender.hasPermission("fusion.admin.use") && args[0].equalsIgnoreCase("use")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().startsWith(args[2])) entries.add(player.getName());
                }
            }
        }
        return entries;
    }


    public interface ConfirmationAction {
        void doAction();
    }

    private void openGui(Player player, ProfessionGuiRegistry registry, Category category) {
        if (category == null) {
            registry.open(player);
        } else {
            registry.open(player, category);
        }
    }
}
