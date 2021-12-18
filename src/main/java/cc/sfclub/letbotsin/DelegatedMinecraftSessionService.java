package cc.sfclub.letbotsin;

import cc.sfclub.letbotsin.util.UUIDUtil;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.ib67.util.reflection.AccessibleField;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.bukkit.Bukkit.getLogger;

public class DelegatedMinecraftSessionService extends HttpMinecraftSessionService {
    private final MinecraftSessionService service;

    public DelegatedMinecraftSessionService(MinecraftSessionService service, HttpAuthenticationService auth) {
        super(auth);
        this.service = service;
        getLogger().info("Injected. "+service);
    }

    @Override
    public void joinServer(GameProfile gameProfile, String s, String s1) throws AuthenticationException {
        getLogger().info(s+" "+s1+" "+gameProfile);
        service.joinServer(gameProfile, s, s1);
    }

    public GameProfile hasJoinedServer(GameProfile gameProfile, String serverId, InetAddress inetAddress) throws AuthenticationUnavailableException {
        getLogger().info(gameProfile.toString());
        if(LetBotsIn.getInstance().isBot(gameProfile.getName(),inetAddress==null?null:inetAddress.getHostAddress())){
            return new GameProfile(UUIDUtil.calcUUID(gameProfile.getName()),gameProfile.getName());
        }
        return service.hasJoinedServer(gameProfile, serverId, inetAddress);
    }

    // 对于老版本的支持
    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
        return hasJoinedServer(user, serverId, null);
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile gameProfile, boolean b) {
        return service.getTextures(gameProfile, b);
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile gameProfile, boolean b) {
        return service.fillProfileProperties(gameProfile, b);
    }
}
