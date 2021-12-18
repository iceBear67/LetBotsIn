package cc.sfclub.letbotsin;

import cc.sfclub.letbotsin.util.UUIDUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.UUID;

public class Listeners implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        if(event.getPlayer().getUniqueId().equals(UUIDUtil.calcUUID(event.getPlayer().getName()))){
            event.getPlayer().setMetadata(LetBotsIn.METADATA_KEY, new FixedMetadataValue(LetBotsIn.getInstance(),null));
        }
    }
    @EventHandler // idk what if player quit without it
    public void onQuit(PlayerQuitEvent event){
        if(event.getPlayer().getUniqueId().equals(UUIDUtil.calcUUID(event.getPlayer().getName()))){
            event.getPlayer().removeMetadata(LetBotsIn.METADATA_KEY, LetBotsIn.getInstance());
        }
    }
}
