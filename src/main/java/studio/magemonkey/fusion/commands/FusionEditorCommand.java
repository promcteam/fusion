package studio.magemonkey.fusion.commands;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.CommandType;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.CodexItemException;
import studio.magemonkey.codex.util.DeserializationWorker;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.cfg.editors.EditorCriteria;
import studio.magemonkey.fusion.cfg.editors.EditorRegistry;
import studio.magemonkey.fusion.data.professions.ProfessionConditions;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.editors.Editor;
import studio.magemonkey.fusion.gui.editors.browse.BrowseEditor;
import studio.magemonkey.fusion.gui.editors.professions.ProfessionEditor;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.TabCacher;

import java.util.*;

public class FusionEditorCommand implements CommandExecutor, TabCompleter {

    private static Map<UUID, EditorCriteria> editorCriteria = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        Editor editor = EditorRegistry.getCurrentEditor(player);
        if (args.length == 0) {
            if (editor != null) {
                editor.open(player);
                return true;
            }
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax",
                            player,
                            new MessageData("syntax", "/fusion-editor <profession|browse>"));
            return true;
        }
        if (editor == null || !editorCriteria.containsKey(player.getUniqueId())) {
            switch (args[0].toLowerCase()) {
                case "profession":
                    if (args.length < 2) {
                        CodexEngine.get()
                                .getMessageUtil()
                                .sendMessage("editor.invalidSyntax",
                                        player,
                                        new MessageData("syntax",
                                                "/fusion-editor profession <professionName> [profession to copy]"));
                        return true;
                    }
                    String professionName = args[1];
                    switch (args.length) {
                        case 2:
                            if (ProfessionsCfg.getMap().containsKey(professionName)) {
                                EditorRegistry.getProfessionEditor(player, professionName).open(player);
                                CodexEngine.get()
                                        .getMessageUtil()
                                        .sendMessage("editor.editProfession",
                                                player,
                                                new MessageData("profession", professionName));
                            } else {
                                if (ProfessionsCfg.createNewProfession(professionName, null)) {
                                    EditorRegistry.getProfessionEditor(player, professionName).open(player);
                                    CodexEngine.get()
                                            .getMessageUtil()
                                            .sendMessage("editor.createdNewProfession",
                                                    player,
                                                    new MessageData("profession", professionName));
                                }
                            }
                            break;
                        case 3:
                            String refProfession = args[2];
                            if (ProfessionsCfg.getMap().containsKey(professionName)) {
                                CodexEngine.get()
                                        .getMessageUtil()
                                        .sendMessage("editor.professionAlreadyExists",
                                                player,
                                                new MessageData("profession", professionName));
                                return true;
                            }
                            if (!ProfessionsCfg.getMap().containsKey(refProfession)) {
                                CodexEngine.get()
                                        .getMessageUtil()
                                        .sendMessage("editor.invalidProfession",
                                                player,
                                                new MessageData("profession", refProfession));
                                return true;
                            }
                            if (ProfessionsCfg.createNewProfession(professionName, refProfession)) {
                                EditorRegistry.getProfessionEditor(player, professionName).open(player);
                                CodexEngine.get()
                                        .getMessageUtil()
                                        .sendMessage("editor.copyProfession",
                                                player,
                                                new MessageData("oldProfession", refProfession),
                                                new MessageData("newProfession", professionName));
                            }
                            break;
                        default:
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidSyntax",
                                            player,
                                            new MessageData("syntax",
                                                    "/fusion-editor profession <professionName> [profession to copy]"));
                            break;
                    }
                    break;
                case "browse":
                    EditorRegistry.getBrowseEditor(player).open(player);
                    break;
            }
            return true;
        }
        EditorCriteria criteria = editorCriteria.get(player.getUniqueId());

        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            switch (criteria) {
                case Profession_Edit_Name -> updateProfessionName(professionEditor, args);
                case Profession_Edit_Icon -> updateProfessionIcon(professionEditor, args);

                case Profession_Category_Add, Profession_Category_Edit ->
                        updateCategory(professionEditor, args, criteria);

                case Pattern_Edit_Name -> updatePatternItemName(professionEditor, args);
                case Pattern_Edit_Lore -> addPatternItemLore(professionEditor, args);
                case Pattern_Edit_Pattern -> updatePatternItem(professionEditor, args);
                case Pattern_Add_Commands -> addPatternItemCommand(professionEditor, args);
                case Pattern_Add_Enchants -> addPatternEnchants(professionEditor, args);
                case Pattern_Add_Flags -> addPatternFlags(professionEditor, args);

                case Profession_Recipe_Edit_Name -> updateRecipeName(professionEditor, args);
                case Profession_Recipe_Add_Commands -> addRecipeCommand(professionEditor, args);
                case Profession_Recipe_Add -> addNewRecipe(professionEditor, args);
                case Profession_Recipe_Edit_ResultItem, Profession_Recipe_Add_Ingredients,
                     Profession_Recipe_Edit_Ingredients -> updateRecipeItems(professionEditor, args, criteria);
                case Profession_Recipe_Edit_Rank -> updateRecipeRank(professionEditor, args);
                case Profession_Recipe_Add_Conditions -> addRecipeConditions(professionEditor, args);
                default -> editor.open(player);
            }
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            switch (criteria) {
                case Browse_Edit_Name -> updateBrowseName(browseEditor, args);
                case Browse_Add_Profession -> addNewProfession(browseEditor, args);
                case Pattern_Edit_Name -> updatePatternItemName(browseEditor, args);
                case Pattern_Edit_Lore -> addPatternItemLore(browseEditor, args);
                case Pattern_Edit_Pattern -> updatePatternItem(browseEditor, args);
                case Pattern_Add_Commands -> addPatternItemCommand(browseEditor, args);
                case Pattern_Add_Enchants -> addPatternEnchants(browseEditor, args);
                case Pattern_Add_Flags -> addPatternFlags(browseEditor, args);
                case Browse_Profession_Add_Ingredients -> addBrowseIngredient(browseEditor, args);
                case Browse_Profession_Edit_Rank -> updateBrowseRank(browseEditor, args);
                case Browse_Profession_Add_Conditions -> addBrowseConditions(browseEditor, args);
                default -> editor.open(player);
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        List<String> entries = new ArrayList<>();
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        Editor editor = EditorRegistry.getCurrentEditor(player);
        if (editor == null || !editorCriteria.containsKey(player.getUniqueId())) {
            if (args.length == 1) {
                if ("profession".startsWith(args[0].toLowerCase())) entries.add("profession");
                if ("browse".startsWith(args[0].toLowerCase())) entries.add("browse");
            } else if (args.length == 2) {
                if ("profession".equalsIgnoreCase(args[0])) {
                    if ("<new profession>".startsWith(args[1].toLowerCase())) entries.add("<new profession>");
                    entries.addAll(TabCacher.getTabs(player.getUniqueId(), "professions", args[1]));
                }
            } else if (args.length == 3) {
                if ("profession".equalsIgnoreCase(args[0]) && !TabCacher.getTabs(player.getUniqueId(),
                        "professions",
                        args[2]).contains(args[1])) {
                    entries.add("<profession to copy>");
                    entries.addAll(TabCacher.getTabs(player.getUniqueId(), "professions", args[2]));
                }
            }
            return entries;
        }
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            EditorCriteria   criteria         = editorCriteria.get(player.getUniqueId());
            switch (criteria) {
                case Profession_Edit_Name:
                case Pattern_Edit_Name:
                    if (args.length == 1) {
                        entries.add("<newName>");
                        entries.add(professionEditor.getTable().getInventoryName());
                    }
                    break;
                case Profession_Edit_Icon:
                    if (args.length == 1) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[0]));
                    }
                    break;
                case Profession_Category_Add:
                case Profession_Category_Edit:
                    if (args.length == 1) {
                        if (criteria == EditorCriteria.Profession_Category_Add) {
                            entries.add("<categoryName>");
                        } else if (professionEditor.getCategoryEditor()
                                .getLastEditedCategoryName()
                                .startsWith(args[0])) {
                            entries.add(professionEditor.getCategoryEditor().getLastEditedCategoryName());
                        }
                    } else if (args.length == 2) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[1]));
                    }
                    break;
                case Profession_Recipe_Add:
                    if (args.length == 1) {
                        entries.add("<recipeName>");
                    } else if (args.length == 2) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[1]));
                    } else if (args.length == 3) {
                        entries.add("<amount>");
                        entries.add("1");
                        entries.add("5");
                        entries.add("32");
                        entries.add("64");
                    }
                    break;
                case Pattern_Edit_Pattern:
                case Profession_Recipe_Edit_ResultItem:
                case Profession_Recipe_Add_Ingredients:
                case Profession_Recipe_Edit_Ingredients:
                    if (args.length == 1) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[0]));
                    } else if (args.length == 2) {
                        entries.add("<amount>");
                        entries.add("1");
                        entries.add("5");
                        entries.add("32");
                        entries.add("64");
                    }
                    break;
                case Browse_Profession_Edit_Rank:
                    if (args.length == 1) {
                        entries.add("<rank>");
                        entries.add(professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getRank());
                    }
                    break;
                case Profession_Recipe_Edit_Name:
                    if (args.length == 1) {
                        entries.add("<newRecipeName>");
                        entries.add(professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipeName());
                    }
                    break;
                case Profession_Recipe_Add_Commands:
                case Pattern_Add_Commands:
                    if (args.length == 1) {
                        if ("console".startsWith(args[0].toUpperCase())) entries.add("console");
                        if ("player".startsWith(args[0].toUpperCase())) entries.add("player");
                        if ("op".startsWith(args[0].toUpperCase())) entries.add("op");
                    } else if (args.length == 2) {
                        entries.add("0");
                        entries.add("<delay>");
                    } else if (args.length == 3) {
                        entries.add("<command without / >");

                    }
                    if (args.length >= 3) {
                        entries.add("{player}");
                    }
                    break;
                case Profession_Recipe_Add_Conditions:
                case Browse_Profession_Add_Conditions:
                    entries.addAll(TabCacher.getConditionsTabs(args));
                    break;
                case Pattern_Edit_Lore:
                    if (args.length == 1) {
                        entries.add("<lore>");
                    }
                    break;
                case Pattern_Add_Enchants:
                    entries.addAll(TabCacher.getEnchantmentsTab(args));
                    break;
                case Pattern_Add_Flags:
                    entries.addAll(TabCacher.getFlagsTab(args));
                    break;
            }
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor   browseEditor = (BrowseEditor) editor;
            EditorCriteria criteria     = editorCriteria.get(player.getUniqueId());
            switch (criteria) {
                case Browse_Edit_Name:
                    if (args.length == 1) {
                        entries.add("<newName>");
                        entries.add(browseEditor.getName());
                    }
                    break;
                case Browse_Add_Profession:
                    if (args.length == 1) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "professions", args[0]));
                    }
                    break;
                case Browse_Profession_Edit_Rank:
                    if (args.length == 1) {
                        entries.add("<rank>");
                        entries.add(browseEditor.getBrowseProfessionsEditor()
                                .getBrowseProfessionEditor()
                                .getConditions()
                                .getRank());
                    }
                    break;
                case Pattern_Edit_Name:
                    if (args.length == 1) {
                        entries.add("<newName>");
                        entries.add(browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().getName());
                    }
                    break;
                case Pattern_Edit_Lore:
                    if (args.length == 1) {
                        entries.add("<lore>");
                    }
                    break;
                case Pattern_Edit_Pattern:
                case Browse_Profession_Add_Ingredients:
                    if (args.length == 1) {
                        entries.addAll(TabCacher.getTabs(TabCacher.GlobalUUID, "items", args[0]));
                    } else if (args.length == 2) {
                        entries.add("<amount>");
                        entries.add("1");
                        entries.add("5");
                        entries.add("32");
                        entries.add("64");
                    }
                    break;
                case Pattern_Add_Commands:
                    if (args.length == 1) {
                        if ("console".startsWith(args[0].toUpperCase())) entries.add("console");
                        if ("player".startsWith(args[0].toUpperCase())) entries.add("player");
                        if ("op".startsWith(args[0].toUpperCase())) entries.add("op");
                    } else if (args.length == 2) {
                        entries.add("0");
                        entries.add("<delay>");
                    } else if (args.length == 3) {
                        entries.add("<command without / >");
                    }
                    if (args.length >= 3) {
                        entries.add("{player}");
                    }
                    break;
                case Browse_Profession_Add_Conditions:
                    entries.addAll(TabCacher.getConditionsTabs(args));
                    break;
                case Pattern_Add_Enchants:
                    entries.addAll(TabCacher.getEnchantmentsTab(args));
                    break;
                case Pattern_Add_Flags:
                    entries.addAll(TabCacher.getFlagsTab(args));
                    break;
            }
        }
        return entries;
    }

    private boolean isValidItem(String item) {
        try {
            // If the material in uppercase is valid, return true
            Material.valueOf(item.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            // If this is a custom item from divinity without "DIVINITY_" prefix, return true
            try {
                return CodexEngine.get().getItemManager().getItemType(item) != null;
            } catch (CodexItemException ignored) {
                return false;
            }
        }
    }

    public static void suggestUsage(Player player, EditorCriteria criteria, String suggestCommand) {
        if (criteria == null) return;
        editorCriteria.put(player.getUniqueId(), criteria);
        switch (criteria) {
            case Profession_Edit_Name:
            case Pattern_Edit_Name:
            case Browse_Edit_Name:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<newName>"));
                break;
            case Browse_Profession_Edit_Rank:
            case Profession_Recipe_Edit_Rank:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<rank>"));
                break;
            case Profession_Recipe_Add_Ingredients:
            case Profession_Recipe_Edit_Ingredients:
            case Browse_Profession_Add_Ingredients:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<ingredient> <amount>"));
                break;
            case Profession_Category_Add:
            case Profession_Category_Edit:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage",
                                player,
                                new MessageData("syntax", "<categoryName> <categoryIcon>"));
                break;
            case Profession_Recipe_Add_Conditions:
            case Browse_Profession_Add_Conditions:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage",
                                player,
                                new MessageData("syntax", "<conditionKey> <conditionValue> <level>"));
                break;
            case Pattern_Add_Commands:
            case Profession_Recipe_Add_Commands:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage",
                                player,
                                new MessageData("syntax", "<caster> <delay> <command without />"));
                break;
            case Profession_Edit_Icon:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<icon>"));
                break;
            case Pattern_Edit_Pattern:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<item> <amount>"));
                break;
            case Pattern_Edit_Lore:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<lore>"));
                break;
            case Profession_Recipe_Add:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage",
                                player,
                                new MessageData("syntax", "<recipeName> <resultItem> <amount>"));
                break;
            case Profession_Recipe_Edit_ResultItem:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<resultItem> <amount>"));
                break;
            case Profession_Recipe_Edit_Name:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<newRecipeName>"));
                break;
            case Browse_Add_Profession:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<professionName>"));
                break;
            case Pattern_Add_Enchants:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<enchantment> [level]"));
                break;
            case Pattern_Add_Flags:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.editorUsage", player, new MessageData("syntax", "<flag>"));
                break;
        }
        sendSuggestMessage(player, suggestCommand);
        player.closeInventory();
    }

    private static void sendSuggestMessage(Player player, String suggestedCommand) {
        if (suggestedCommand == null) return;
        BaseComponent[] components = CodexEngine.get().getMessageUtil().getMessageAsComponent("editor.editorClick");
        for (BaseComponent component : components) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand));
        }
        player.spigot().sendMessage(components);
    }

    /* Profession */
    private void updateProfessionName(ProfessionEditor professionEditor, String[] args) {
        StringBuilder professionNameBuilder = new StringBuilder();
        for (String arg : args) {
            professionNameBuilder.append(arg).append(" ");
        }
        String professionName = professionNameBuilder.toString().trim();
        String oldName        = professionEditor.getTable().getName();
        Player player         = professionEditor.getPlayer();
        professionEditor.getTable().setInventoryName(professionName);
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.professionRenamed",
                        player,
                        new MessageData("oldName", oldName),
                        new MessageData("newName", professionName));
        professionEditor.reload(true);
    }

    private void updateProfessionIcon(ProfessionEditor professionEditor, String[] args) {
        String icon    = args[0];
        String oldIcon = professionEditor.getTable().getIconItem().getID();
        Player player  = professionEditor.getPlayer();
        if (!isValidItem(icon)) {
            CodexEngine.get().getMessageUtil().sendMessage("editor.invalidItem", player, new MessageData("item", icon));
            return;
        }
        try {
            professionEditor.getTable().setIconItem(CodexEngine.get().getItemManager().getItemType(icon));
        } catch (CodexItemException e) {
            CodexEngine.get().getMessageUtil().sendMessage("editor.invalidItem", player, new MessageData("item", icon));
            return;
        }
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.professionIconChanged",
                        player,
                        new MessageData("oldIcon", oldIcon),
                        new MessageData("newIcon", icon));
        professionEditor.reload(true);
    }

    /* Categories */
    private void updateCategory(ProfessionEditor professionEditor, String[] args, EditorCriteria criteria) {
        if (args.length < 2) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax",
                            professionEditor.getPlayer(),
                            new MessageData("syntax", "<categoryName> <categoryIcon>"));
            return;
        }

        String categoryName = args[0];
        String categoryIcon = args[1];
        Player player       = professionEditor.getPlayer();
        if (!isValidItem(categoryIcon)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidItem", player, new MessageData("item", categoryIcon));
            return;
        }

        if (criteria == EditorCriteria.Profession_Category_Add) {
            professionEditor.getTable()
                    .getCategories()
                    .put(categoryName,
                            new Category(Map.of("name",
                                    categoryName,
                                    "icon",
                                    categoryIcon,
                                    "order",
                                    professionEditor.getTable().getCategories().size() + 1)));
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.categoryAdded",
                            player,
                            new MessageData("category", categoryName),
                            new MessageData("profession", professionEditor.getTable().getName()));
        } else {
            if (!professionEditor.getTable().getCategories().containsKey(categoryName)) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.categoryNotFound", player, new MessageData("category", categoryName));
                return;
            }
            professionEditor.getTable().getCategories().get(categoryName).setName(categoryName);
            ItemType oldIcon = professionEditor.getTable().getCategories().get(categoryName).getIconItem();
            try {
                professionEditor.getTable()
                        .getCategories()
                        .get(categoryName)
                        .setIconItem(CodexEngine.get().getItemManager().getItemType(categoryIcon));
            } catch (CodexItemException e) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidItem", player, new MessageData("item", categoryIcon));
                professionEditor.getTable().getCategories().get(categoryName).setIconItem(oldIcon);
                return;
            }
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.categoryEdited",
                            player,
                            new MessageData("category", categoryName),
                            new MessageData("profession", professionEditor.getTable().getName()));
        }
        professionEditor.getCategoryEditor().reload(true);
    }

    /* Patterns */
    private void updatePatternItemName(Editor editor, String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg).append(" ");
        }
        String name = builder.toString().trim();
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            String oldName =
                    professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().getName();
            Player player = professionEditor.getPlayer();
            professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().name(name);
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.patternItemRenamed",
                            player,
                            new MessageData("oldName", oldName),
                            new MessageData("newName", name));
            professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            String oldName =
                    browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().getName();
            Player player = browseEditor.getPlayer();
            browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().name(name);
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.patternItemRenamed",
                            player,
                            new MessageData("oldName", oldName),
                            new MessageData("newName", name));
            browseEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
        }
    }

    private void updatePatternItem(Editor editor, String[] args) {
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            Player           player           = professionEditor.getPlayer();
            if (args.length < 2) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax",
                                professionEditor.getPlayer(),
                                new MessageData("syntax", "<item> <amount>"));
                return;
            }
            try {
                Material material = Material.valueOf(args[0].toUpperCase());
                int      amount   = Integer.parseInt(args[1]);

                professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().material(material);
                professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().amount(amount);
                professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.patternItemUpdated",
                                player,
                                new MessageData("item", args[0]),
                                new MessageData("amount", amount));
            } catch (Exception e) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidItem", player, new MessageData("item", args[0]));
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
            }
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            Player       player       = browseEditor.getPlayer();
            if (args.length < 2) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax",
                                browseEditor.getPlayer(),
                                new MessageData("syntax", "<item> <amount>"));
                return;
            }
            try {
                Material material = Material.valueOf(args[0].toUpperCase());
                int      amount   = Integer.parseInt(args[1]);

                browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().material(material);
                browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().amount(amount);
                browseEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.patternItemUpdated",
                                player,
                                new MessageData("item", args[0]),
                                new MessageData("amount", amount));
            } catch (Exception e) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidItem", player, new MessageData("item", args[0]));
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
            }
        }
    }

    private void addPatternItemLore(Editor editor, String[] args) {
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            Player           player           = professionEditor.getPlayer();
            if (args.length < 1) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", args));
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                builder.append(args[i]);
                if (i < args.length - 1) builder.append(" ");
            }
            professionEditor.getPatternItemsEditor()
                    .getPatternItemEditor()
                    .getBuilder()
                    .newLoreLine(builder.toString());
            professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.patternItemLoreAdded",
                            player,
                            new MessageData("lore", ChatUT.hexString(builder.toString())));
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            Player       player       = browseEditor.getPlayer();
            if (args.length < 1) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", args));
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                builder.append(args[i]);
                if (i < args.length - 1) builder.append(" ");
            }
            browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().newLoreLine(builder.toString());
            browseEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.patternItemLoreAdded",
                            player,
                            new MessageData("lore", ChatUT.hexString(builder.toString())));
        }
    }

    private void addPatternItemCommand(Editor editor, String[] args) {
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            Player           player           = professionEditor.getPlayer();
            if (args.length < 3) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidCommand", player, new MessageData("command", args));
                return;
            }
            StringBuilder commandBuilder = new StringBuilder();
            try {
                CommandType commandType = CommandType.valueOf(args[0].toUpperCase());
                int         delay       = Integer.parseInt(args[1]);
                commandBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    commandBuilder.append(args[i]);
                    if (i < args.length - 1) commandBuilder.append(" ");
                }
                professionEditor.getPatternItemsEditor()
                        .getPatternItemEditor()
                        .addCommand(new DelayedCommand(Map.of("delay",
                                delay,
                                "as",
                                commandType.name(),
                                "cmd",
                                commandBuilder.toString())));
                professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            } catch (Exception e) {
                e.printStackTrace();
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidCommand",
                                player,
                                new MessageData("command", args[0] + " " + args[1] + " " + commandBuilder));
            }
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            Player       player       = browseEditor.getPlayer();
            if (args.length < 3) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidCommand", player, new MessageData("command", args));
                return;
            }
            StringBuilder commandBuilder = new StringBuilder();
            try {
                CommandType commandType = CommandType.valueOf(args[0].toUpperCase());
                int         delay       = Integer.parseInt(args[1]);
                commandBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    commandBuilder.append(args[i]);
                    if (i < args.length - 1) commandBuilder.append(" ");
                }
                browseEditor.getPatternItemsEditor()
                        .getPatternItemEditor()
                        .addCommand(new DelayedCommand(Map.of("delay",
                                delay,
                                "as",
                                commandType.name(),
                                "cmd",
                                commandBuilder.toString())));
                browseEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            } catch (Exception e) {
                e.printStackTrace();
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidCommand",
                                player,
                                new MessageData("command", args[0] + " " + args[1] + " " + commandBuilder));
            }
        }
    }

    private void addPatternEnchants(Editor editor, String[] args) {
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            Player           player           = professionEditor.getPlayer();
            if (args.length < 1) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax",
                                player,
                                new MessageData("syntax", "<enchantment> [level]"));
                return;
            }

            int level = 1;
            if (args.length >= 2) {
                try {
                    level = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
                    return;
                }
            }

            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(args[0].toLowerCase()));
            if (enchantment == null) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidEnchantment", player, new MessageData("enchantment", args[0]));
                return;
            }
            professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().enchant(enchantment, level);
            professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            Player       player       = browseEditor.getPlayer();
            if (args.length < 1) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax",
                                player,
                                new MessageData("syntax", "<enchantment> [level]"));
                return;
            }

            int level = 1;
            if (args.length >= 2) {
                try {
                    level = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
                    return;
                }
            }

            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(args[0].toLowerCase()));
            if (enchantment == null) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidEnchantment", player, new MessageData("enchantment", args[0]));
                return;
            }
            browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().enchant(enchantment, level);
            browseEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
        }
    }

    public void addPatternFlags(Editor editor, String[] args) {
        if (editor instanceof ProfessionEditor) {
            ProfessionEditor professionEditor = (ProfessionEditor) editor;
            Player           player           = professionEditor.getPlayer();
            if (args.length < 1) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<flag>"));
                return;
            }
            try {
                ItemFlag flag = ItemFlag.valueOf(args[0].toUpperCase());
                if (professionEditor.getPatternItemsEditor()
                        .getPatternItemEditor()
                        .getBuilder()
                        .getFlags()
                        .contains(flag)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.flagAlreadyExists", player, new MessageData("flag", flag.name()));
                    return;
                }
                professionEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().flag(flag);
                professionEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            } catch (Exception e) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidFlag", player, new MessageData("flag", args[0]));
            }
        } else if (editor instanceof BrowseEditor) {
            BrowseEditor browseEditor = (BrowseEditor) editor;
            Player       player       = browseEditor.getPlayer();
            if (args.length < 1) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<flag>"));
                return;
            }
            try {
                ItemFlag flag = ItemFlag.valueOf(args[0].toUpperCase());
                if (browseEditor.getPatternItemsEditor()
                        .getPatternItemEditor()
                        .getBuilder()
                        .getFlags()
                        .contains(flag)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.flagAlreadyExists", player, new MessageData("flag", flag.name()));
                    return;
                }
                browseEditor.getPatternItemsEditor().getPatternItemEditor().getBuilder().flag(flag);
                browseEditor.getPatternItemsEditor().getPatternItemEditor().reload(true);
            } catch (Exception e) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidFlag", player, new MessageData("flag", args[0]));
            }
        }
    }

    /* Recipes */
    private void updateRecipeName(ProfessionEditor professionEditor, String[] args) {
        if (args.length < 1) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax",
                            professionEditor.getPlayer(),
                            new MessageData("syntax", "<newRecipeName>"));
            return;
        }

        String recipeName = args[0];
        Player player     = professionEditor.getPlayer();
        String oldName    = professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipeName();
        for (Recipe recipe : professionEditor.getTable().getRecipes().values()) {
            if (recipe.getName().equalsIgnoreCase(recipeName)) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.recipeAlreadyExists", player, new MessageData("recipe", recipeName));
                return;
            }
        }
        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().setName(recipeName);
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.recipeRenamed",
                        player,
                        new MessageData("oldName", oldName),
                        new MessageData("newName", recipeName));
        professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
    }

    private void addRecipeCommand(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 3) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidCommand", player, new MessageData("command", args));
            return;
        }
        StringBuilder commandBuilder = new StringBuilder();
        try {
            CommandType commandType = CommandType.valueOf(args[0].toUpperCase());
            int         delay       = Integer.parseInt(args[1]);
            commandBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                commandBuilder.append(args[i]);
                if (i < args.length - 1) commandBuilder.append(" ");
            }
            professionEditor.getRecipeEditor()
                    .getRecipeItemEditor()
                    .getRecipe()
                    .getResults()
                    .getCommands()
                    .add(new DelayedCommand(Map.of("delay",
                            delay,
                            "as",
                            commandType.name(),
                            "cmd",
                            commandBuilder.toString())));
            professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
        } catch (Exception e) {
            e.printStackTrace();
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidCommand",
                            player,
                            new MessageData("command", args[0] + " " + args[1] + " " + commandBuilder));
        }
    }

    private void addNewRecipe(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length < 3) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax",
                            player,
                            new MessageData("syntax", "<recipeName> <resultItem> <amount>"));
            return;
        }
        try {
            String recipeName = args[0];
            String itemName   = args[1];
            int    amount     = Integer.parseInt(args[2]);
            for (Recipe recipe : professionEditor.getTable().getRecipes().values()) {
                if (recipe.getName().equalsIgnoreCase(recipeName)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.recipeAlreadyExists", player, new MessageData("recipe", recipeName));
                    return;
                }
            }
            if (!isValidItem(itemName)) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidItem", player, new MessageData("item", itemName));
                return;
            }
            if (professionEditor.getTable().getRecipes().containsKey(recipeName)) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.recipeAlreadyExists", player, new MessageData("recipe", recipeName));
                return;
            }
            Map<String, Object> recipeSettings = new LinkedHashMap<>();
            recipeSettings.put("name", recipeName);
            if (!professionEditor.getTable().getCategories().isEmpty())
                recipeSettings.put("category", professionEditor.getTable().getCategories().keySet().iterator().next());
            recipeSettings.put("craftingTime", 0);
            recipeSettings.put("results",
                    Map.of("item",
                            itemName + ":" + amount,
                            "professionExp",
                            0,
                            "vanillaExp",
                            0,
                            "commands",
                            new ArrayList<>()));
            recipeSettings.put("conditions", Map.of("professionLevel", 0, "mastery", false));
            recipeSettings.put("costs", Map.of("items", List.of("STONE:3"), "money", 0.0, "exp", 0));

            professionEditor.getTable()
                    .getRecipes()
                    .put(recipeName, new Recipe(professionEditor.getTable(), recipeSettings));
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.recipeAdded",
                            player,
                            new MessageData("recipe", recipeName),
                            new MessageData("result", itemName));
            professionEditor.getRecipeEditor().reload(true);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidNumber", player, new MessageData("number", args[2]));
        }
    }

    private void updateRecipeItems(ProfessionEditor professionEditor, String[] args, EditorCriteria criteria) {
        Player player = professionEditor.getPlayer();
        if (args.length != 2) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<item> <amount>"));
            return;
        }
        try {
            String itemName = args[0];
            int    amount   = Integer.parseInt(args[1]);
            if (!isValidItem(itemName)) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidItem", player, new MessageData("item", itemName));
                return;
            }

            switch (criteria) {
                case Profession_Recipe_Edit_ResultItem:
                    professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getResults()
                            .setResultItem(RecipeItem.fromConfig(itemName + ":" + amount));
                    professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getResults()
                            .setResultName(itemName + ":" + amount);
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.resultEdited",
                                    player,
                                    new MessageData("recipe",
                                            professionEditor.getRecipeEditor()
                                                    .getRecipeItemEditor()
                                                    .getRecipe()
                                                    .getName()),
                                    new MessageData("result", itemName));
                    break;
                case Profession_Recipe_Add_Ingredients:
                    int i = 0;
                    boolean found = false;
                    for (RecipeItem ingredient : professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getConditions()
                            .getRequiredItems()) {
                        if (ingredient.toConfig().toString().split(":")[0].equalsIgnoreCase(itemName)) {
                            professionEditor.getRecipeEditor()
                                    .getRecipeItemEditor()
                                    .getRecipe()
                                    .getConditions()
                                    .getRequiredItems()
                                    .set(i, RecipeItem.fromConfig(itemName + ":" + amount));
                            professionEditor.getRecipeEditor()
                                    .getRecipeItemEditor()
                                    .getRecipe()
                                    .getConditions()
                                    .getRequiredItemNames()
                                    .set(i, itemName + ":" + amount);
                            found = true;
                        }
                        i++;
                    }
                    if (!found) {
                        professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getRequiredItems()
                                .add(RecipeItem.fromConfig(itemName + ":" + amount));
                        professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getRequiredItemNames()
                                .add(itemName + ":" + amount);
                    }
                    break;
                case Profession_Recipe_Edit_Ingredients:
                    professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getConditions()
                            .getRequiredItems()
                            .clear();
                    professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getConditions()
                            .getRequiredItems()
                            .add(RecipeItem.fromConfig(itemName + ":" + amount));
                    professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getConditions()
                            .getRequiredItemNames()
                            .add(itemName + ":" + amount);
                    break;
            }
            professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
        }
    }

    private void updateRecipeRank(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length != 1) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<rank>"));
            return;
        }
        String rank = args[0];
        professionEditor.getRecipeEditor().getRecipeItemEditor().getRecipe().getConditions().setRank(rank);
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.recipeRankUpdated", player, new MessageData("rank", rank));
        professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
    }

    private void addRecipeConditions(ProfessionEditor professionEditor, String[] args) {
        Player player = professionEditor.getPlayer();
        if (args.length != 3) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax",
                            player,
                            new MessageData("syntax", "<conditionKey> <conditionValue> <level>"));
            return;
        }
        String conditionKey   = args[0];
        String conditionValue = args[1];
        int    level          = Integer.parseInt(args[2]);

        switch (conditionKey) {
            case "professions":
                if (!ProfessionsCfg.getMap().containsKey(conditionValue)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                professionEditor.getRecipeEditor()
                        .getRecipeItemEditor()
                        .getRecipe()
                        .getConditions()
                        .getProfessionConditions()
                        .put(conditionValue, level);
                break;
            case "fabled":
                if (!Bukkit.getPluginManager().isPluginEnabled("Fabled")) return;
                if (!Fabled.getClasses().containsKey(conditionValue)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                professionEditor.getRecipeEditor()
                        .getRecipeItemEditor()
                        .getRecipe()
                        .getConditions()
                        .getFabledClassConditions()
                        .put(conditionValue, level);
                break;
            case "mcmmo":
                if (!Bukkit.getPluginManager().isPluginEnabled("mcMMO")) return;
                try {
                    PrimarySkillType skillType = PrimarySkillType.valueOf(conditionValue.toUpperCase());
                    professionEditor.getRecipeEditor()
                            .getRecipeItemEditor()
                            .getRecipe()
                            .getConditions()
                            .getMcMMOConditions()
                            .put(conditionValue, level);
                } catch (IllegalArgumentException e) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                break;
            case "jobs":
                if (!Bukkit.getPluginManager().isPluginEnabled("Jobs")) return;
                Optional<Job> job = Jobs.getJobs()
                        .stream()
                        .filter(_job -> _job.getName().equalsIgnoreCase(conditionValue))
                        .findFirst();
                if (job.isEmpty()) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                professionEditor.getRecipeEditor()
                        .getRecipeItemEditor()
                        .getRecipe()
                        .getConditions()
                        .getJobsConditions()
                        .put(conditionValue, level);
                break;
            case "aura_abilities":
            case "aura_mana_abilities":
            case "aura_skills":
            case "aura_stats":
                if (!Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || !Bukkit.getPluginManager()
                        .isPluginEnabled("AureliumSkills")) return;
                switch (conditionKey) {
                    case "aura_abilities":
                        if (AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(conditionValue))
                                == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getAuraAbilityConditions()
                                .put(conditionValue, level);
                        break;
                    case "aura_mana_abilities":
                        if (AuraSkillsApi.get()
                                .getGlobalRegistry()
                                .getManaAbility(NamespacedId.fromString(conditionValue)) == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getAuraManaAbilityConditions()
                                .put(conditionValue, level);
                        break;
                    case "aura_skills":
                        if (AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(conditionValue))
                                == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getAuraSkillsConditions()
                                .put(conditionValue, level);
                        break;
                    case "aura_stats":
                        if (AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(conditionValue))
                                == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        professionEditor.getRecipeEditor()
                                .getRecipeItemEditor()
                                .getRecipe()
                                .getConditions()
                                .getAuraStatsConditions()
                                .put(conditionValue, level);
                        break;
                }
            default:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidConditionKey", player, new MessageData("key", conditionKey));
                return;
        }
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.conditionAdded",
                        player,
                        new MessageData("key", conditionKey),
                        new MessageData("value", conditionValue),
                        new MessageData("level", args[2]));
        professionEditor.getRecipeEditor().getRecipeItemEditor().reload(true);
    }


    private void updateBrowseName(BrowseEditor browseEditor, String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg).append(" ");
        }
        String name    = builder.toString().trim();
        String oldName = browseEditor.getName();
        Player player  = browseEditor.getPlayer();
        browseEditor.setName(name);
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.browserRenamed",
                        player,
                        new MessageData("oldName", oldName),
                        new MessageData("newName", name));
        browseEditor.reload(true);
    }

    private void addNewProfession(BrowseEditor browseEditor, String[] args) {
        String professionName = args[0];
        Player player         = browseEditor.getPlayer();
        if (!ProfessionsCfg.getMap().containsKey(professionName)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidProfession", player, new MessageData("profession", professionName));
            return;
        }
        if (browseEditor.getProfessions().contains(professionName)) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.professionAlreadyExists",
                            player,
                            new MessageData("profession", professionName));
            return;
        }
        browseEditor.getProfessions().add(professionName);
        Map<String, Object> conditionsMaps = new LinkedHashMap<>();
        conditionsMaps.put("costs", Map.of("money", 0.0, "exp", 0, "items", new ArrayList<>()));
        conditionsMaps.put("conditions", Map.of());
        browseEditor.getProfessionConditions()
                .put(professionName,
                        new ProfessionConditions(professionName, DeserializationWorker.start(conditionsMaps)));
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.addedProfessionToBrowse", player, new MessageData("profession", professionName));
        browseEditor.getBrowseProfessionsEditor().reload(true);
    }

    private void addBrowseIngredient(BrowseEditor browseEditor, String[] args) {
        Player player = browseEditor.getPlayer();
        if (args.length != 2) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<item> <amount>"));
            return;
        }
        try {
            String itemName = args[0];
            int    amount   = Integer.parseInt(args[1]);
            if (!isValidItem(itemName)) {
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidItem", player, new MessageData("item", itemName));
                return;
            }

            int     i     = 0;
            boolean found = false;
            for (RecipeItem ingredient : browseEditor.getBrowseProfessionsEditor()
                    .getBrowseProfessionEditor()
                    .getConditions()
                    .getRequiredItems()) {
                if (ingredient.toConfig().toString().split(":")[0].equalsIgnoreCase(itemName)) {
                    browseEditor.getBrowseProfessionsEditor()
                            .getBrowseProfessionEditor()
                            .getConditions()
                            .getRequiredItems()
                            .set(i, RecipeItem.fromConfig(itemName + ":" + amount));
                    browseEditor.getBrowseProfessionsEditor()
                            .getBrowseProfessionEditor()
                            .getConditions()
                            .getRequiredItemNames()
                            .set(i, itemName + ":" + amount);
                    found = true;
                }
                i++;
            }
            if (!found) {
                browseEditor.getBrowseProfessionsEditor()
                        .getBrowseProfessionEditor()
                        .getConditions()
                        .getRequiredItems()
                        .add(RecipeItem.fromConfig(itemName + ":" + amount));
                browseEditor.getBrowseProfessionsEditor()
                        .getBrowseProfessionEditor()
                        .getConditions()
                        .getRequiredItemNames()
                        .add(itemName + ":" + amount);
                browseEditor.getBrowseProfessionsEditor().getBrowseProfessionEditor().reload(true);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidNumber", player, new MessageData("number", args[1]));
        }
    }

    private void updateBrowseRank(BrowseEditor browseEditor, String[] args) {
        Player player = browseEditor.getPlayer();
        if (args.length != 1) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax", player, new MessageData("syntax", "<rank>"));
            return;
        }
        String rank = args[0];
        browseEditor.getBrowseProfessionsEditor().getBrowseProfessionEditor().getConditions().setRank(rank);
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.browseRankUpdated", player, new MessageData("rank", rank));
        browseEditor.getBrowseProfessionsEditor().getBrowseProfessionEditor().reload(true);
    }

    private void addBrowseConditions(BrowseEditor browseEditor, String[] args) {
        Player player = browseEditor.getPlayer();
        if (args.length != 3) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("editor.invalidSyntax",
                            player,
                            new MessageData("syntax", "<conditionKey> <conditionValue> <level>"));
            return;
        }
        String conditionKey   = args[0];
        String conditionValue = args[1];
        int    level          = Integer.parseInt(args[2]);

        switch (conditionKey) {
            case "professions":
                if (!ProfessionsCfg.getMap().containsKey(conditionValue)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                browseEditor.getBrowseProfessionsEditor()
                        .getBrowseProfessionEditor()
                        .getConditions()
                        .getProfessionConditions()
                        .put(conditionValue, level);
                break;
            case "fabled":
                if (!Bukkit.getPluginManager().isPluginEnabled("Fabled")) return;
                if (!Fabled.getClasses().containsKey(conditionValue)) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                browseEditor.getBrowseProfessionsEditor()
                        .getBrowseProfessionEditor()
                        .getConditions()
                        .getFabledClassConditions()
                        .put(conditionValue, level);
                break;
            case "mcmmo":
                if (!Bukkit.getPluginManager().isPluginEnabled("mcMMO")) return;
                try {
                    PrimarySkillType skillType = PrimarySkillType.valueOf(conditionValue.toUpperCase());
                    browseEditor.getBrowseProfessionsEditor()
                            .getBrowseProfessionEditor()
                            .getConditions()
                            .getMcMMOConditions()
                            .put(conditionValue, level);
                } catch (IllegalArgumentException e) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                break;
            case "jobs":
                if (!Bukkit.getPluginManager().isPluginEnabled("Jobs")) return;
                Optional<Job> job = Jobs.getJobs()
                        .stream()
                        .filter(_job -> _job.getName().equalsIgnoreCase(conditionValue))
                        .findFirst();
                if (job.isEmpty()) {
                    CodexEngine.get()
                            .getMessageUtil()
                            .sendMessage("editor.invalidConditionValue",
                                    player,
                                    new MessageData("key", conditionKey),
                                    new MessageData("value", conditionValue),
                                    new MessageData("level", args[2]));
                    return;
                }
                browseEditor.getBrowseProfessionsEditor()
                        .getBrowseProfessionEditor()
                        .getConditions()
                        .getJobsConditions()
                        .put(conditionValue, level);
                break;
            case "aura_abilities":
            case "aura_mana_abilities":
            case "aura_skills":
            case "aura_stats":
                if (!Bukkit.getPluginManager().isPluginEnabled("AuraSkills") || !Bukkit.getPluginManager()
                        .isPluginEnabled("AureliumSkills")) return;
                switch (conditionKey) {
                    case "aura_abilities":
                        if (AuraSkillsApi.get().getGlobalRegistry().getAbility(NamespacedId.fromString(conditionValue))
                                == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        browseEditor.getBrowseProfessionsEditor()
                                .getBrowseProfessionEditor()
                                .getConditions()
                                .getAuraAbilityConditions()
                                .put(conditionValue, level);
                        break;
                    case "aura_mana_abilities":
                        if (AuraSkillsApi.get()
                                .getGlobalRegistry()
                                .getManaAbility(NamespacedId.fromString(conditionValue)) == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        browseEditor.getBrowseProfessionsEditor()
                                .getBrowseProfessionEditor()
                                .getConditions()
                                .getAuraManaAbilityConditions()
                                .put(conditionValue, level);
                        break;
                    case "aura_skills":
                        if (AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromString(conditionValue))
                                == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        browseEditor.getBrowseProfessionsEditor()
                                .getBrowseProfessionEditor()
                                .getConditions()
                                .getAuraSkillsConditions()
                                .put(conditionValue, level);
                        break;
                    case "aura_stats":
                        if (AuraSkillsApi.get().getGlobalRegistry().getStat(NamespacedId.fromString(conditionValue))
                                == null) {
                            CodexEngine.get()
                                    .getMessageUtil()
                                    .sendMessage("editor.invalidConditionValue",
                                            player,
                                            new MessageData("key", conditionKey),
                                            new MessageData("value", conditionValue),
                                            new MessageData("level", args[2]));
                            return;
                        }
                        browseEditor.getBrowseProfessionsEditor()
                                .getBrowseProfessionEditor()
                                .getConditions()
                                .getAuraStatsConditions()
                                .put(conditionValue, level);
                        break;
                }
            default:
                CodexEngine.get()
                        .getMessageUtil()
                        .sendMessage("editor.invalidConditionKey", player, new MessageData("key", conditionKey));
                return;
        }
        CodexEngine.get()
                .getMessageUtil()
                .sendMessage("editor.conditionAdded",
                        player,
                        new MessageData("key", conditionKey),
                        new MessageData("value", conditionValue),
                        new MessageData("level", args[2]));
        browseEditor.getBrowseProfessionsEditor().getBrowseProfessionEditor().reload(true);
    }

    public static void removeEditorCriteria(UUID uuid) {
        editorCriteria.remove(uuid);
    }
}
