package tech.sebazcrc.blockshuffle;

import com.google.common.base.Joiner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tech.sebazcrc.blockshuffle.Game.GamePlayer;
import tech.sebazcrc.blockshuffle.Util.ScoreHelper;
import tech.sebazcrc.blockshuffle.Util.ScoreStringBuilder;

import java.util.*;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin implements CommandExecutor {

    public static String prefix;
    private static Main instance;

    private Blocks[] blocks;
    private SplittableRandom random = new SplittableRandom();

    private List<GamePlayer> players;
    public Map<Player, Integer> currentSubString;
    private ArrayList<String> lines;

    private int time = 0;
    private int showing = 0;
    private GameState state = GameState.WAITING;


    @Override
    public void onEnable() {
        instance = this;
        prefix = format("&8[&3&lBlockShuffle&8] &7&l➤ &r&f");

        this.blocks = Arrays.stream(Blocks.values()).filter(blocks1 -> blocks1.getMat() != null).collect(Collectors.toList()).toArray(new Blocks[Blocks.values().length-1]);
        this.players = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            GamePlayer gp = new GamePlayer(p.getUniqueId());
            this.players.add(gp);
        }

        this.lines = new ArrayList<>();

        lines.add("&6&lSebazCRC Projects");
        lines.add("&e&lS&6&lebazCRC Projects");
        lines.add("&e&lS&6&lebazCRC Projects");
        lines.add("&e&lSe&6&lbazCRC Projects");
        lines.add("&e&lSeb&6&lazCRC Projects");
        lines.add("&e&lSeba&6&lzCRC Projects");
        lines.add("&e&lSebaz&6&lCRC Projects");
        lines.add("&e&lSebazC&6&lRC Projects");
        lines.add("&e&lSebazCR&6&lC Projects");
        lines.add("&e&lSebazCRC &6&lProjects");
        lines.add("&e&lSebazCRC P&6&lrojects");
        lines.add("&e&lSebazCRC Pr&6&lojects");
        lines.add("&e&lSebazCRC Pro&6&ljects");
        lines.add("&e&lSebazCRC Proj&6&lects");
        lines.add("&e&lSebazCRC Proje&6&lcts");
        lines.add("&e&lSebazCRC Projec&6&lts");
        lines.add("&e&lSebazCRC Project&6&ls");
        lines.add("&e&lSebazCRC Projects");

        this.currentSubString = new HashMap<>();
        tickAll();
        System.out.print("Blocks = " + blocks.length);

        getServer().getPluginManager().registerEvents(new Events(this), instance);
        getCommand("shuffle").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("shuffle")) {
            if (!sender.hasPermission("shuffle.use")) {
                sender.sendMessage(format("&cNo tienes permisos."));
                return false;
            }

            if (args[0].equalsIgnoreCase("start")) {
                sender.sendMessage(format("&aIniciado"));

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (getPlayer(p.getName()) == null) {
                        GamePlayer gp = new GamePlayer(p.getUniqueId());
                        getPlayers().add(gp);
                        chooseBlockFor(gp);
                    } else {
                        chooseBlockFor(getPlayer(p.getName()));
                    }
                }
                setState(Main.GameState.PLAYING);

                return true;
            } else if (args[0].equalsIgnoreCase("debugtime")) {
                setTime(getPlayTime() + 285);
                return true;
            }
        }
        return false;
    }

    private void tickAll() {
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    tickScoreboard(p);
                }
            }
        }, 0L, 3L);

        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                if (getState() == GameState.PLAYING) {
                    int timer = (time % 300);
                    int reaming = (300-timer);

                    if (reaming == 30 || reaming == 45 || reaming <= 10) {
                        if (reaming == 1) {

                            List<GamePlayer> pending = getPlayingPlayers().stream().filter(gamePlayer -> gamePlayer.hasBlock()).collect(Collectors.toList());
                            List<String> searching = pending.stream().map(gamePlayer -> gamePlayer.getName()).collect(Collectors.toList());

                            Collections.sort(searching, String.CASE_INSENSITIVE_ORDER);
                            String list = Joiner.on(", ").join(searching);

                            if (searching.size() > 1) {
                                Bukkit.broadcastMessage(prefix + format("Los jugadores &b" + list + " &fno lograron encontrar su bloque."));
                                for (GamePlayer gp : pending) {
                                    gp.loose(false);
                                }
                            } else if (searching.size() == 1) {
                                Bukkit.broadcastMessage(prefix + format("El jugador &b" + list + " &fno logró encontrar su bloque."));
                                for (GamePlayer gp : pending) {
                                    gp.loose(false);
                                }
                            } else {
                                Bukkit.broadcastMessage(prefix + format("Ningún jugador fue eliminado."));
                            }

                            for (GamePlayer rp : getPlayingPlayers()) {
                                chooseBlockFor(rp);
                            }

                        } else {
                            List<GamePlayer> searching = getPlayingPlayers();
                            for (GamePlayer gp : searching) {
                                gp.sendMessage(prefix + format("Quedan &b" + (reaming + (reaming > 1 ? " &fsegundos" : " &fsegundo")) + " para encontrar el bloque."));
                            }
                        }
                    }

                    time++;
                    if (showing < 6) showing++;
                } else {
                    if (showing < 3) showing++;
                }
            }
        }, 0L, 20L);
    }

    public boolean checkForBlock(GamePlayer gp) {
        if (!gp.isOnline()) return false;
        return gp.getOnlinePlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == gp.getCurrentBlock().getMat();
    }

    public void foundBlock(GamePlayer gp) {
        Bukkit.broadcastMessage(prefix + format("&b" + gp.getName() + " &eencontró su bloque (" + gp.getCurrentBlock().getColor() + ChatColor.BOLD + gp.getCurrentBlock().getName() + "&r&e)"));
        gp.setCurrentBlock(null);
    }

    public void eliminatePlayer(Player p) {
        if (getPlayer(p.getName()) != null) getPlayer(p.getName()).loose(true);
    }

    public void chooseBlockFor(GamePlayer gp) {
        Blocks b = null;
        while (b == null) {
            Blocks n = this.blocks[this.random.nextInt(this.blocks.length)];
            if (n != Blocks.NONE) b = n;
        }

        gp.sendMessage(prefix + "&eDebes encontrar: " + b.getColor() + ChatColor.BOLD + b.getName());
        gp.setCurrentBlock(b);
    }

    private void tickScoreboard(Player p) {
        updateScoreboard(getPlayer(p.getName()));

        if (ScoreHelper.hasScore(p)) {

            if (!this.currentSubString.containsKey(p)) {
                this.currentSubString.put(p, 0);
            } else {
                int plus = this.currentSubString.get(p) + 1;
                if (plus > this.lines.size()-1) {
                    plus = 0;
                }
                this.currentSubString.replace(p, plus);
            }

            ScoreHelper.getByPlayer(p).setTitle(format(lines.get(this.currentSubString.get(p))));
        }
    }

    private void createScoreboard(Player p) {
        if (!ScoreHelper.hasScore(p)) {
            ScoreHelper.createScore(p).setTitle("&6&lSebazCRC Projects");
        }
    }

    private void updateScoreboard(GamePlayer gp) {

        Player p = gp.getOnlinePlayer();

        if (ScoreHelper.hasScore(p)) {

            ScoreHelper helper = ScoreHelper.getByPlayer(p.getPlayer());
            String s = getScoreboardLines(gp);

            String[] split = s.split("\n");
            List<String> lines = new ArrayList<>();

            for (int i = 0; i < split.length; ++i) {
                String str2 = split[i];
                lines.add(format(str2));
            }

            helper.setSlotsFromList(lines);

        } else {
            createScoreboard(p);
        }
    }

    private String getScoreboardLines(GamePlayer p) {

        String s;

        if (getState() == GameState.PLAYING || getState() == GameState.ENDING) {
            ScoreStringBuilder b = new ScoreStringBuilder(true);

            if (showing <= 2) {
                b.add("Jugadores:");
                b.add("&a" + getPlayingPlayers().size());
            } else {
                b.add("Espectadores:");
                b.add("&a" + getSpectators().size());
                if (showing == 5) showing = 0;
            }
            if (getState() != GameState.ENDING) {
                b.add(" ");
                b.add("&fTiempo:");
                b.add("&a" + getTime());
                if (!p.hasLost()) {
                    String bn = (p.getCurrentBlock() != Blocks.NONE ? "" + p.getCurrentBlock().getColor() + ChatColor.BOLD + p.getCurrentBlock().getName() : "Ninguno");
                    b.space().add("Tu Bloque:").add(bn);
                }
            }
            b.add(" ");
            s = b.build();
        } else {
            ScoreStringBuilder b = new ScoreStringBuilder(true);
            String wait = "ESPERANDO";
            if (showing == 1) wait = wait + ".";
            if (showing == 2) {
                wait = wait + "..";
            }
            if (showing == 3) {
                wait = wait + "...";
                showing = 0;
            }
            b.add("&6&l" + wait);
            b.add(" ");
            b.add("&fJugadores:");
            b.add("&a" + Bukkit.getOnlinePlayers().size());
            b.add(" ");
            s = b.build();
        }

        return s;
    }

    private String getTime() {
        int hrs = time / 3600;
        int minAndSec = time % 3600;
        int min = minAndSec / 60;
        int sec = minAndSec % 60;

        return (hrs > 9 ? hrs : "0" + hrs) + ":" + (min > 9 ? min : "0" + min) + ":" + (sec > 9 ? sec : "0" + sec);
    }

    public Blocks[] getBlocks() {
        return blocks;
    }

    public SplittableRandom getRandom() {
        return random;
    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public List<GamePlayer> getPlayingPlayers() {
        return players.stream().filter(gamePlayer -> !gamePlayer.hasLost()).collect(Collectors.toList());
    }

    public List<GamePlayer> getSpectators() {
        return players.stream().filter(gamePlayer -> gamePlayer.hasLost()).collect(Collectors.toList());
    }

    public GamePlayer getPlayer(String name) {
        for (GamePlayer gp : getPlayers()) {
            if (gp.getName().equalsIgnoreCase(name)) {
                return gp;
            }
        }

        return null;
    }

    public GameState getState() {
        return state;
    }

    public static String format(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static Main getInstance() {
        return instance;
    }

    public void setState(GameState stat) {
        this.state = stat;
    }

    public void checkRemainPlayers() {
        List<GamePlayer> remain = getPlayingPlayers();

        if (remain.size() == 1) {
            GamePlayer winner = remain.get(0);
            Bukkit.broadcastMessage(prefix + format("¡&b" + winner.getName() + " &fha ganado la partida!"));
            setState(GameState.ENDING);
        } else if (remain.size() < 1) {
            Bukkit.broadcastMessage(prefix + "¡Ningún jugador consiguió encontrar su bloque!");
            setState(GameState.ENDING);
        }

        for (Player on : Bukkit.getOnlinePlayers()) {
            on.playSound(on.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 10.0F, 10.0F);
        }
    }

    public void setTime(int i) {
        this.time = i;
    }

    public int getPlayTime() {
        return time;
    }

    public enum GameState {
        WAITING, PLAYING, ENDING
    }
}
