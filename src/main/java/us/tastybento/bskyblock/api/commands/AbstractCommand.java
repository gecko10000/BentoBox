package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.util.Util;

/**
 *
 * @author Poslovitch
 */
public abstract class AbstractCommand implements CommandExecutor, TabCompleter {

    private BSkyBlock plugin;

    private final Map<String, ArgumentHandler> argumentsMap;
    private final Map<String, String> aliasesMap;

    public final String label;
    public boolean isPlayer;
    public boolean inTeam;
    public UUID teamLeaderUUID;
    public Set<UUID> teamMembers;
    public Player player;
    public UUID playerUUID;

    private final boolean help;
    private static final int MAX_PER_PAGE = 7;

    protected AbstractCommand(BSkyBlock plugin, String label, boolean help) {
        this.plugin = plugin;
        this.argumentsMap = new HashMap<>(1);
        this.aliasesMap = new HashMap<>(1);
        this.label = label;
        this.help = help;

        // Register the help argument if needed
        if (help) {
            addArgument(new String[]{"help", "?"}, new ArgumentHandler() {
                @Override
                public boolean canUse(CommandSender sender) {
                    return true; // If the player has access to this command, he can get help
                }

                @Override
                public void execute(CommandSender sender, String[] args) {

                }

                @Override
                public List<String> tabComplete(CommandSender sender, String[] args) {
                    return null; // No tab options for this one
                }

                @Override
                public String[] usage(CommandSender sender) {
                    return new String[] {"", ""};
                }
            });
        }

        // Register the other arguments
        setup();
    }

    /**
     *
     */
    public interface ArgumentHandler {
        boolean canUse(CommandSender sender);
        void execute(CommandSender sender, String[] args);
        List<String> tabComplete(CommandSender sender, String[] args);
        String[] usage(CommandSender sender);
    }

    public abstract void setup();

    public abstract boolean canUse(CommandSender sender);
    public abstract void execute(CommandSender sender, String[] args);

    public void addArgument(String[] names, ArgumentHandler handler) {
        // TODO add some security checks to avoid duplicates
        argumentsMap.put(names[0], handler);
        for (int i = 1 ; i < names.length ; i++) {
            aliasesMap.put(names[0], names[i]);
        }
    }

    public ArgumentHandler getHandler(String argument) {
        if (isAlias(argument)) return argumentsMap.get(getParent(argument));
        else return argumentsMap.get(argument);
    }

    public void setHandler(String argument, ArgumentHandler handler) {
        if (argumentsMap.containsKey(argument)) argumentsMap.put(argument, handler);
    }

    public boolean isAlias(String argument) {
        if (aliasesMap.containsValue(argument)) return true;
        return false;
    }

    public void addAliases(String parent, String... aliases) {
        if (argumentsMap.containsKey(parent)) {
            for (String alias : aliases) {
                if (!aliasesMap.containsKey(alias) && !aliasesMap.containsValue(alias)) aliasesMap.put(parent, alias);
            }
        }
    }

    public void removeAliases(String... aliases) {
        for (String alias : aliases) {
            if (aliasesMap.containsValue(alias)) aliasesMap.remove(getParent(alias));
        }
    }

    public String getParent(String alias) {
        if (isAlias(alias)) {
            for(String parent : aliasesMap.keySet()) {
                if (aliasesMap.get(parent).equals(alias)) return parent;
            }
            return null;
        }
        else return alias;
    }

    public List<String> getAliases(String argument) {
        return null; //TODO
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        checkForPlayer(sender);
        if (this.canUse(sender)) {
            if(args.length >= 1) {
                ArgumentHandler handler = getHandler(args[0]); // Store the handler to save some calculations
                if (handler != null && handler.canUse(sender)) {
                    handler.execute(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                } else if (help) {
                    if (argumentsMap.containsKey("help")) {
                        argumentsMap.get("help").execute(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                    }
                } else {
                    // Unknown handler
                    this.execute(sender, args);
                }
            } else {
                // No args
                this.execute(sender, args);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        List<String> options = new ArrayList<String>();
        checkForPlayer(sender);
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");
        if (canUse(sender)) {
            if (args.length <= 1) {
                // Go through every argument, check if player can use it and if so, add it in tab options
                for(String argument : argumentsMap.keySet()) {
                    if (getHandler(argument).canUse(sender)) options.add(argument);
                }
            } else {
                // If player can execute the argument, get its tab-completer options
                ArgumentHandler handler = getHandler(args[0]);
                if (handler != null && handler.canUse(sender)) {
                    // We remove the 1st arg - and remove any blank args caused by hitting space before the tab
                    List<String> tabOptions = handler.tabComplete(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                    if (tabOptions != null) options.addAll(tabOptions);
                }
            }
        }
        return Util.tabLimit(options, lastArg);
    }

    private static String[] clean(final String[] v) {
        List<String> list = new ArrayList<String>(Arrays.asList(v));
        list.removeAll(Collections.singleton(""));
        return list.toArray(new String[list.size()]);
    }
    
    /**
     * Sets some variables and flags if this is a player
     * @param sender
     */
    private void checkForPlayer(CommandSender sender) {
        // Check if the command sender is a player or not
        if (sender instanceof Player) {
            isPlayer = true;
            player = (Player)sender;
            playerUUID = player.getUniqueId();
        }
        // Check if the player is in a team or not and if so, grab the team leader's UUID
        if (plugin.getPlayers().inTeam(playerUUID)) {
            inTeam = true;
            teamLeaderUUID = plugin.getIslands().getTeamLeader(playerUUID);
            teamMembers = plugin.getIslands().getMembers(teamLeaderUUID);
        }
        
    }
}
