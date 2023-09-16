package tech.sebazcrc.blockshuffle.Game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import tech.sebazcrc.blockshuffle.Blocks;
import tech.sebazcrc.blockshuffle.Main;

import java.util.UUID;

public class GamePlayer {

    private UUID id;
    private String name;
    private Blocks currentBlock;

    private boolean pendingSpectator;
    private boolean lost;

    public GamePlayer(UUID id) {
        this.id = id;
        this.name = Bukkit.getOfflinePlayer(id).getName();
        this.currentBlock = Blocks.NONE;
        this.pendingSpectator = false;
        this.lost = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isOnline() {
        return Bukkit.getOfflinePlayer(id).isOnline();
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(id);
    }

    public Player getOnlinePlayer() {
        if (!isOnline()) return null;
        return (Player) getPlayer();
    }

    public void setCurrentBlock(Blocks currentBlock) {
        if (currentBlock == null) currentBlock = Blocks.NONE;

        this.currentBlock = currentBlock;
    }
    public Blocks getCurrentBlock() { return currentBlock; }

    public boolean hasBlock() { return currentBlock != Blocks.NONE; }

    public void sendMessage(String s) {
        if (isOnline()) {
            getOnlinePlayer().sendMessage(Main.format(s));
        }
    }

    public boolean hasLost() {
        return lost;
    }

    public void loose(boolean broadcast) {
        this.lost = true;
        if (broadcast) Bukkit.broadcastMessage(Main.prefix + Main.format("&b" + getName() + " &6&lNO &elogró encontrar su bloque (" + getCurrentBlock().getColor() + ChatColor.BOLD + getCurrentBlock().getName() + "&r&e)"));
        this.setCurrentBlock(null);

        if (isOnline()) {
            getOnlinePlayer().setGameMode(GameMode.SPECTATOR);
        } else {
            this.pendingSpectator = true;
        }

        Main.getInstance().checkRemainPlayers();
    }

    public void onJoin() {
        if (pendingSpectator) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (isOnline()) {
                        getOnlinePlayer().setGameMode(GameMode.SPECTATOR);
                        pendingSpectator = false;
                    }
                }
            },3L);
        }
    }

    public void onDeath() {
        this.lost = true;
        getOnlinePlayer().setGameMode(GameMode.SPECTATOR);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (isOnline()) {
                    getOnlinePlayer().spigot().respawn();
                    getOnlinePlayer().setGameMode(GameMode.SPECTATOR);
                } else {
                    pendingSpectator = true;
                }
            }
        }, 3L);

        Main.getInstance().checkRemainPlayers();
    }
}
