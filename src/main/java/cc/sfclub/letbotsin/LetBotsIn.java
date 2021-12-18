package cc.sfclub.letbotsin;

import cc.sfclub.letbotsin.data.AllowedPlayer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.ib67.Util;
import io.ib67.util.reflection.AccessibleField;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LetBotsIn extends JavaPlugin {
    public static final String METADATA_KEY = "letbotsin_bot";

    @Getter
    private List<AllowedPlayer> allowedPlayers;
    @Getter
    private boolean ignoreIPCheck;
    @Getter
    private boolean debug;
    private static LetBotsIn instance;
    public static LetBotsIn getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance=this;
        loadConfig();
        // Inject session service
        inject();
        getServer().getPluginManager().registerEvents(new Listeners(), this);
        // Initialize packet filter...
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this, PacketType.Login.Server.ENCRYPTION_BEGIN,PacketType.Login.Client.START));
    }
    private void loadConfig(){
        getDataFolder().mkdirs();
        saveDefaultConfig();
        ConfigurationSection section = getConfig().getConfigurationSection("allowedPlayers");
        allowedPlayers = section.getKeys(false)
                .stream()
                .map(key -> {
                    String ipPattern = section.getString(key + ".ipPattern");
                    String namePattern = section.getString(key + ".namePattern");
                    if (ipPattern == null || namePattern == null) {
                        getLogger().warning(key + ": ip or name pattern CAN'T be null or empty");
                        getLogger().warning("Skipping");
                        return null;
                    }
                    return new AllowedPlayer(namePattern, ipPattern);
                }).filter(Objects::nonNull).collect(Collectors.toList());
        ignoreIPCheck = getConfig().getBoolean("ignoreIpCheck", true);
        debug = getConfig().getBoolean("debug", false);

    }
    public boolean isBot(String name, String address) {
        boolean result =allowedPlayers.stream().anyMatch(e -> e.matches(name,address));
        if(debug){
            getLogger().info("DEBUG: isBot? "+name+" Address: "+address+" Result:"+result);
        }
        return result;
    }

    @Override
    public void onDisable() {

    }

    public boolean isBot(Player player) {
        return player.hasMetadata(METADATA_KEY);
    }
    @SneakyThrows
    private void inject() {

        Class<?> craftServerClass = getServer().getClass();
        Method craftServerGetHandle = craftServerClass.getDeclaredMethod("getHandle");
        Class<?> dedicatedPlayerListClass = craftServerGetHandle.getReturnType();
        Method dedicatedPlayerListGetHandler = null/*dedicatedPlayerListClass.getDeclaredMethod("getServer")*/;
        for (Method method : dedicatedPlayerListClass.getMethods()) {
            if (method.getReturnType().getName().contains("DedicatedServer")) {
                dedicatedPlayerListGetHandler = method;
            }
        }

        Class<?> minecraftServerClass = dedicatedPlayerListGetHandler.getReturnType().getSuperclass();
        String fname = null;
        for (Field field : minecraftServerClass.getDeclaredFields()) {
            if (field.getType().getCanonicalName().equals(MinecraftSessionService.class.getCanonicalName())) {
                fname = field.getName();
            }
        }

        AccessibleField<Object> field = new AccessibleField<Object>((Class<Object>) minecraftServerClass, fname, false);
        Object obj = dedicatedPlayerListGetHandler.invoke(craftServerGetHandle.invoke(getServer()));
        HttpMinecraftSessionService svc = (HttpMinecraftSessionService) field.get(obj);
        DelegatedMinecraftSessionService service = new DelegatedMinecraftSessionService(svc, svc.getAuthenticationService());
        field.set(obj, service);
    }
}
