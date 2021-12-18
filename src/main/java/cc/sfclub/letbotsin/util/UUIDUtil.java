package cc.sfclub.letbotsin.util;

import java.util.UUID;

public class UUIDUtil {
    public static UUID calcUUID(String name){
        return UUID.nameUUIDFromBytes(("LetBosIN:"+name).getBytes());
    }
}
