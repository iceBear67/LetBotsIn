package cc.sfclub.letbotsin.data;

import cc.sfclub.letbotsin.LetBotsIn;
import com.mojang.authlib.GameProfile;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

public final class AllowedPlayer {
    private final Pattern name;
    private final Pattern origin;
    private int hashCode;

    public AllowedPlayer(String name, String origin) {
        this.name = Pattern.compile(name);
        this.origin = Pattern.compile(origin);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AllowedPlayer)){
            return false;
        }
        return hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        if(hashCode!=0){
            return hashCode;
        }
        hashCode = 1;
        hashCode = hashCode * 37 + name.hashCode();
        hashCode = hashCode * 37 + origin.pattern().hashCode();
        return hashCode;
    }

    public boolean matches(String profileName, String address){
        if(address == null && !LetBotsIn.getInstance().isIgnoreIPCheck()){ // for older versions
            return false;
        }
        return name.matcher(profileName).matches() && (LetBotsIn.getInstance().isIgnoreIPCheck() || origin.matcher(address).matches());
    }
}
