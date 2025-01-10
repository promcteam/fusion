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
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.Profession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commands implements CommandExecutor, TabCompleter {

    private final Map<String, ConfirmationAction> confirmation = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (args.length == 0) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.help", sender, new MessageData("sender", sender),
                    new MessageData("text", label + " " + StringUtils.join(args, ' ')));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "auto" -> CommandMechanics.toggleAutoCrafting(sender);
            case "browse" -> CommandMechanics.openBrowseGui(sender);
            case "confirm" -> CommandMechanics.confirmAction(sender, confirmation);
            case "exp" -> {
                if(args.length == 5) {
                    CommandMechanics.setProfessionExp(sender, args);
                }
            }
            case "forget" -> {
                if (args.length == 2)
                    CommandMechanics.forgetProfession(sender, args, confirmation);
            }
            case "join" -> {
                if (args.length == 2)
                    CommandMechanics.joinProfession(sender, args);
            }
            case "level" -> {
                if(args.length == 5) {
                    CommandMechanics.setProfessionLevel(sender, args);
                }
            }
            case "reload" -> CommandMechanics.reloadPlugin(sender);
            case "master" -> {
                if (args.length == 2)
                    CommandMechanics.masterProfession(sender, args);
                return true;
            }
            case "show" -> CommandMechanics.openIngredientGui(sender);
            case "stats" -> CommandMechanics.showStats(sender);
            case "storage" -> {
                if (args.length == 2) {
                    CommandMechanics.setStorage(sender, args);
                }
                return true;
            }
            case "use" -> {
                if (args.length >= 2) {
                    CommandMechanics.useProfession(sender, args);
                    return true;
                }
            }
            default ->
                    CodexEngine.get().getMessageUtil().sendMessage("fusion.help", sender, new MessageData("sender", sender),
                            new MessageData("text", label + " " + StringUtils.join(args, ' ')));
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
        if (args.length == 1) {
            if ("browse".startsWith(args[0])) entries.add("browse");
            if ("level".startsWith(args[0])) entries.add("level");
            if ("stats".startsWith(args[0])) entries.add("stats");
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
}
