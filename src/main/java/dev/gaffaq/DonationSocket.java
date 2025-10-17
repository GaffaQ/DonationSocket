package dev.gaffaq;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;

public class DonationSocket extends JavaPlugin {

    private int port;
    private String token;
    private List<String> whitelist;
    private String broadcastMessage;
    private List<String> commands;
    private List<String> sounds;

    private Thread listenerThread;
    private boolean running = true;

    private File logFile;
    private FileConfiguration logConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        setupLogFile();
        startSocketListener();
        getLogger().info("Plugin Donation Socket aktif di port: " + port);
    }

    @Override
    public void onDisable() {
        running = false;
        getLogger().info("DonationSocket closed");
    }

    private void loadConfigValues() {
        port = getConfig().getInt("server.port", 19132);
        token = getConfig().getString("server.token", "SECRET123");
        whitelist = new CopyOnWriteArrayList<>(getConfig().getStringList("whitelist"));
        broadcastMessage = getConfig().getString("broadcast.message",
                "&6{username} &eberdonasi sebesar &aRp{amount}&e dan mendapatkan paket &b{package}&e! 🎉");
        commands = getConfig().getStringList("commands");
        
        // baca sound
        sounds = getConfig().getStringList("broadcast.sounds");
    }

    private void setupLogFile() {
        logFile = new File(getDataFolder(), "donations.yml");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logConfig = YamlConfiguration.loadConfiguration(logFile);
    }

    private void saveLog(String username, double amount, String pack, String ip) {
        String time = new Date().toString();
        logConfig.set("donations." + System.currentTimeMillis(),
                Map.of("username", username, "amount", amount, "package", pack, "ip", ip, "time", time));
        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startSocketListener() {
        listenerThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                getLogger().info("Listening for donations on port " + port);

                while (running) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listenerThread.start();
    }

    private void handleClient(Socket client) {
        String clientIP = client.getInetAddress().getHostAddress();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {

            if (!whitelist.contains(clientIP)) {
                writer.write("IP not allowed\n");
                writer.flush();
                client.close();
                getLogger().warning("Blocked connection from unauthorized IP: " + clientIP);
                return;
            }

            String input = reader.readLine();
            if (input == null) return;

            JSONObject json = new JSONObject(input);
            String username = json.getString("username");
            double amount = json.optDouble("amount", 0);
            String pack = json.optString("package", "Donator");
            String receivedToken = json.getString("token");

            if (!receivedToken.equals(token)) {
                writer.write("Invalid token\n");
                writer.flush();
                client.close();
                getLogger().warning("Invalid token attempt from " + clientIP);
                return;
            }

            Bukkit.getScheduler().runTask(this, () -> {
                String message = ChatColor.translateAlternateColorCodes('&',
                        broadcastMessage
                                .replace("{username}", username)
                                .replace("{package}", pack)
                                .replace("{amount}", String.format("%,.0f", amount))
                );

                Bukkit.broadcastMessage(message);

                for (String cmd : commands) {
                    String parsed = cmd
                            .replace("{username}", username)
                            .replace("{package}", pack)
                            .replace("{amount}", String.valueOf(amount));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                }

                // ini untuk handle soundnya
                if (sounds != null && !sounds.isEmpty()) {
                    String raw = sounds.get(new Random().nextInt(sounds.size()));
                    if (raw != null && !raw.isBlank()) {
                        String normalized = raw.toUpperCase().replace('-', '_').replace(' ', '_');
                        try {
                            Sound s = Sound.valueOf(normalized);
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), s, 1.0f, 1.0f);
                            }
                            getLogger().info("Memutar sound nih: " + raw);
                        } catch (IllegalArgumentException iae) {
                            getLogger().warning("Configured sound '" + raw + "' is not a valid Sound enum value.");
                        }
                    }
                }

                saveLog(username, amount, pack, clientIP);
            });

            writer.write("Donation processed\n");
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
