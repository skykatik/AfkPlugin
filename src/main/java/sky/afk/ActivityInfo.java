package sky.afk;

import arc.util.*;
import mindustry.core.NetClient;
import mindustry.gen.*;

import java.util.Objects;

import static mindustry.net.Administration.PlayerInfo;
import static sky.afk.AfkPlugin.config;

public class ActivityInfo{
    public String uuid;

    public int tileX;
    public int tileY;

    public float mouseX;
    public float mouseY;

    public boolean afk;

    public long lastBuildActivityTime;

    public ActivityInfo(Player player){
        update(player);
    }

    public void update(Player player){
        uuid = player.uuid();

        tileX = player.tileX();
        tileY = player.tileX();

        mouseX = player.mouseX();
        mouseY = player.mouseY();
    }

    public void ifAfk(){
        if(afk){
            Player player = Objects.requireNonNull(Groups.player.find(p -> Objects.equals(p.uuid(), uuid)), "User with uuid '" + uuid + "' not found");
            Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] at now active!", NetClient.colorizeName(player.id(), player.name())));
            afk = false;
        }
    }

    public boolean isOldMessage(Player player){
        PlayerInfo playerInfo = player.getInfo();
        return playerInfo.lastMessageTime != 0 && Time.millis() - playerInfo.lastMessageTime > config.inactivityTime;
    }

    public boolean isStand(Player player){
        return inDiapason(tileX, player.tileX(), 3F) &&
               inDiapason(tileY, player.tileY(), 3F) ||
               inDiapason(mouseX, player.mouseX(), 1.5F) &&
               inDiapason(mouseY, player.mouseY(), 1.5F);
    }

    private boolean inDiapason(float f1, float f2, float d){
        return f1 + d > f2 && f2 > f1 - d;
    }
}
