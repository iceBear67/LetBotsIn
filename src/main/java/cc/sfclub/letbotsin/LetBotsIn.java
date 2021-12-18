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

    public static LetBotsIn getInstance() {
        return LetBotsIn.getPlugin(LetBotsIn.class);
    }

    @Override
    public void onEnable() {
        // 加载配置
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

        // 注入 SessionService
        inject();
        getServer().getPluginManager().registerEvents(new Listeners(), this);
        // 初始化 filter 避免bot进行正版验证
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this, PacketType.Login.Server.ENCRYPTION_BEGIN,PacketType.Login.Client.START));
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

    private void inject() {
        Util.runCatching(() -> {
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
            return null;
        }).alsoPrintStack().onSuccess(e -> {
            getLogger().info("Load successful!");
        }).onFailure(e -> {
            getLogger().info("Can't load!");
        });
    }
}
