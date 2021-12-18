package cc.sfclub.letbotsin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.ib67.util.Pair;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class PacketListener extends PacketAdapter {
    private final Map<String,String> ipNameMap = new HashMap<>();

    public PacketListener(Plugin plugin, PacketType... types) {
        super(plugin, ListenerPriority.HIGHEST,types);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if(event.getPacketType() == PacketType.Login.Client.START){
            PacketContainer container = event.getPacket();
            String id = container.getGameProfiles().read(0).getName();
            ipNameMap.put(event.getPlayer().getAddress().toString(),id);
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if(event.getPacketType() == PacketType.Login.Server.ENCRYPTION_BEGIN){
            if (LetBotsIn.getInstance().isBot(ipNameMap.get(event.getPlayer().getAddress().toString()),event.getPlayer().getAddress().getAddress().getHostAddress())) {
                event.setCancelled(true);
            }
        }
    }
}
