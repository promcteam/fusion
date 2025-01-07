package studio.magemonkey.fusion.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.BrowseGUI;
import studio.magemonkey.fusion.gui.ProfessionGuiRegistry;
import studio.magemonkey.fusion.gui.show.ShowRecipesGui;
import studio.magemonkey.fusion.util.LevelFunction;
import studio.magemonkey.fusion.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CommandMechanics {

    public static void useProfession(CommandSender sender, String[] args) {
        if (!Utils.hasCraftingUsePermission(sender, null)) {
            return;
        }
        String[] professionArgs = args[1].split(":");
        String profession = professionArgs[0];
        Category category = null;

        ProfessionGuiRegistry eq = ProfessionsCfg.getGuiMap().get(profession);

        if (professionArgs.length == 2 && Fusion.getInstance().checkPermission(sender, "fusion.craft.use.categories")) {
            category = ProfessionsCfg.getTable(profession).getCategory(professionArgs[1]);
        } else if (professionArgs.length == 2) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noPermission", sender);
            return;
        }

        if (eq == null) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.notACrafting",
                    sender,
                    new MessageData("name", args[1]),
                    new MessageData("sender", sender));
            return;
        }

        if (args.length == 3) {
            if (!Fusion.getInstance().checkPermission(sender, "fusion.admin.use")) {
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                CodexEngine.get().getMessageUtil().sendMessage("notAPlayer",
                        sender,
                        new MessageData("name", args[2]),
                        new MessageData("sender", sender));
                return;
            }

            //TODO ?? Make sure they have unlocked this crafting menu

            openGui(target, eq, category);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.useConfirmOther",
                    sender,
                    new MessageData("craftingInventory", eq),
                    new MessageData("sender", sender),
                    new MessageData("target", target));
        } else {
            if (sender instanceof Player player) {
                if (!Utils.hasCraftingUsePermission(sender, eq.getProfession())) {
                    return;
                }
                //Make sure they have unlocked this crafting menu
                if (!PlayerLoader.getPlayer(player).hasProfession(eq.getProfession())) {
                    if (player.isOp()) {
                        openGui(player, eq, category);
                        CodexEngine.get().getMessageUtil().sendMessage("fusion.useConfirm",
                                sender,
                                new MessageData("craftingInventory", eq),
                                new MessageData("player", sender));
                        return;
                    }
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.error.notUnlocked", sender);
                    return;
                }
                openGui(player, eq, category);
                CodexEngine.get().getMessageUtil().sendMessage("fusion.useConfirm",
                        sender,
                        new MessageData("craftingInventory", eq),
                        new MessageData("player", sender));
            } else {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.help", sender, new MessageData("sender", sender));
            }
        }
    }

    public static void masterProfession(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            String guiName = args[1];
            CraftingTable table = ProfessionsCfg.getTable(guiName);
            if (table == null) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.notACrafting",
                        sender,
                        new MessageData("name", args[1]),
                        new MessageData("sender", sender));
                return;
            }

            if (PlayerLoader.getPlayer(((Player) sender).getUniqueId()).hasMastered(table.getName())) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.error.alreadyMastered",
                        sender,
                        new MessageData("sender", sender),
                        new MessageData("craftingTable", table));
            }

            if (LevelFunction.getLevel(player, table) < table.getMasteryUnlock()) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMasteryLevel",
                        sender,
                        new MessageData("sender", sender),
                        new MessageData("craftingTable", table));
            }

            if (!CodexEngine.get().getVault().canPay(player, table.getMasteryFee())) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMasteryFunds",
                        sender,
                        new MessageData("sender", sender),
                        new MessageData("craftingTable", table));
            }
            FusionAPI.getEventServices().getProfessionService().masterProfession(table.getName(), player, true);
            PlayerLoader.getPlayer(((Player) sender).getUniqueId()).setMastered(table.getName(), true);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.mastered",
                    sender,
                    new MessageData("sender", sender),
                    new MessageData("craftingTable", table));
        } else {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.help", sender, new MessageData("sender", sender));
        }
    }

    public static void forgetProfession(CommandSender sender, String[] args, Map<String, Commands.ConfirmationAction> confirmation) {
        if (sender instanceof Player player) {
            CraftingTable table = ProfessionsCfg.getTable(args[1]);
            if (table == null) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.notACrafting",
                        sender,
                        new MessageData("name", args[1]),
                        new MessageData("sender", sender));
                return;
            }
            Commands.ConfirmationAction action =
                    () -> FusionAPI.getEventServices().getProfessionService().leaveProfession(table, player);

            confirmation.put(player.getUniqueId().toString(), action);
            CodexEngine.get().getMessageUtil().sendMessage("fusion.forget.confirm",
                    sender,
                    new MessageData("sender", sender),
                    new MessageData("craftingTable", table));

            Bukkit.getScheduler().runTaskLater(Fusion.getInstance(),
                    () -> confirmation.remove(player.getUniqueId().toString()), 15 * 20L);

        } else {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.help", sender, new MessageData("sender", sender));
        }
    }


    public static void joinProfession(CommandSender sender, String[] args) {
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
    }

    public static void setStorage(CommandSender sender, String[] args) {
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
                    return;
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
                    return;
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
    }

    public static void openBrowseGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
            return;
        }
        if (player.isPermissionSet("fusion.browse") && !player.hasPermission("fusion.browse")) {
            return;
        }
        BrowseGUI.open(player);
    }

    public static void openIngredientGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
            return;
        }
        if (Fusion.getInstance().checkPermission(sender, "fusion.show")) {
            showIngredientUsage(player);
        }
    }

    public static void showLevels(CommandSender sender) {
        if (!(sender instanceof Player)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
            return;
        }
        for (Profession profession : PlayerLoader.getPlayer(((Player) sender).getUniqueId()).getProfessions()) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.level.format", sender,
                    new MessageData("category", profession.getName()),
                    new MessageData("level", profession.getLevel()),
                    new MessageData("experience",
                            PlayerLoader.getPlayer(((Player) sender).getUniqueId())
                                    .getExperience(profession)));
        }
    }

    public static void toggleAutoCrafting(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("senderIsNotPlayer", sender, new MessageData("sender", sender));
            return;
        }
        if (Cfg.craftingQueue) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.autoDisabled", sender);
            return;
        }
        if (!Fusion.getInstance().checkPermission(sender, "fusion.auto")) {
            return;
        }

        boolean autoOn = PlayerLoader.getPlayer(player).isAutoCrafting();

        PlayerLoader.getPlayer(player).setAutoCrafting((autoOn = !autoOn));
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("fusion.autoToggle", player, new MessageData("state", autoOn ? "on" : "off"));
    }

    public static void reloadPlugin(CommandSender sender) {
        if (!Fusion.getInstance().checkPermission(sender, "fusion.reload")) {
            return ;
        }
        Fusion.getInstance().closeAll();
        Fusion.getInstance().reloadConfig();
        Fusion.getInstance().reloadLang();
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("fusion.reload", sender, new MessageData("sender", sender));
    }

    public static void confirmAction(CommandSender sender, Map<String, Commands.ConfirmationAction> confirmation) {
        String id = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";
        if (confirmation.containsKey(id)) {
            confirmation.get(id).doAction();
            confirmation.remove(id);
        } else {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.nothingToConfirm", sender, new MessageData("sender", sender));
        }
    }

    private static void openGui(Player player, ProfessionGuiRegistry registry, Category category) {
        if (category == null) {
            registry.open(player);
        } else {
            registry.open(player, category);
        }
    }

    private static void showIngredientUsage(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return;
        }

        Map<Recipe, RecipeItem> recipeUsage = new HashMap<>();
        for (CraftingTable table : ProfessionsCfg.getMap().values()) {
            for (Recipe recipe : table.getRecipes().values()) {
                for (RecipeItem ingredient : recipe.getConditions().getRequiredItems()) {
                    if (CalculatedRecipe.isSimilar(ingredient.getItemStack(), item)) {
                        recipeUsage.put(recipe, ingredient);
                    }
                }
            }
        }

        new ShowRecipesGui(player, recipeUsage).open(player);
    }
}
