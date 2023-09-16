package tech.sebazcrc.blockshuffle;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import tech.sebazcrc.blockshuffle.Game.GamePlayer;
import tech.sebazcrc.blockshuffle.Util.ScoreHelper;

public class Events implements Listener {

    private Main instance;

    public Events(Main instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        GamePlayer gp = getPlayer(p);

        if (gp == null) {
            if (!p.isOp() && instance.getState() != Main.GameState.WAITING) {
                e.setKickMessage(Main.format("&cLa partida ya ha comenzado."));
                e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        GamePlayer gp = getPlayer(p);

        if (gp != null) {
            gp.onJoin();
        } else {
            gp = new GamePlayer(p.getUniqueId());
            Main.getInstance().getPlayers().add(gp);
            instance.chooseBlockFor(gp);
        }

        if (ScoreHelper.hasScore(e.getPlayer())) {
            e.getPlayer().setScoreboard(ScoreHelper.getByPlayer(e.getPlayer()).getScoreboard());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        GamePlayer gp = getPlayer(p);

        if (p.getGameMode() == GameMode.SPECTATOR) {
            Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        p.spigot().respawn();
                        p.setGameMode(GameMode.SPECTATOR);
                    }
                }
            }, 3L);
            return;
        }
        if (instance.getState() != Main.GameState.PLAYING) return;


        instance.eliminatePlayer(p);
        for (Player on : Bukkit.getOnlinePlayers()) {
            on.playSound(on.getLocation(), Sound.ENTITY_WITHER_DEATH, 100.0F, 1.0F);
        }

        if (gp != null) {
            gp.onDeath();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        GamePlayer gp = getPlayer(p);

        if (gp != null) {
            if (!gp.hasLost() && gp.hasBlock()) {
                if (instance.checkForBlock(gp)) {
                    instance.foundBlock(gp);
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().isOp()) {
            if (e.getMessage().startsWith("/startgame")) {
                e.setCancelled(true);

            }
            if (e.getMessage().startsWith("/debugtime")) {

            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.VOID && ((Player) e.getEntity()).getGameMode() == GameMode.SPECTATOR) {
            e.setCancelled(true);
        }
    }

    private GamePlayer getPlayer(Player p) {
        return Main.getInstance().getPlayer(p.getName());
    }
}
