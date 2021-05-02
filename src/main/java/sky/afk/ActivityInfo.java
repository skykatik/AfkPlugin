package sky.afk;

import arc.util.*;
import mindustry.core.NetClient;
import mindustry.gen.*;

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
    public int warnings = 0;

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
            Player player = Groups.player.find(p -> p.uuid().equals(uuid));
            if(warnings != 0 && config.warningsEnabled()){
                Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] at now active! Warns reset",
                        NetClient.colorizeName(player.id(), player.name())));
                warnings = 0;
            }else{
                Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] at now active!",
                        NetClient.colorizeName(player.id(), player.name())));
            }
            afk = false;
        }
    }

    public boolean isOldMessage(Player player){
        PlayerInfo playerInfo = player.getInfo();
        return Time.timeSinceMillis(playerInfo.lastMessageTime) > config.inactivityTime;
    }

    public boolean isOldBuildActivity(){
        return Time.timeSinceMillis(lastBuildActivityTime) > config.inactivityTime;
    }

    public boolean isStand(Player player){
        return inDiapason(tileX, player.tileX(), 3f) &&
               inDiapason(tileY, player.tileY(), 3f) ||
               inDiapason(mouseX, player.mouseX(), 1.5f) &&
               inDiapason(mouseY, player.mouseY(), 1.5f);
    }

    private boolean inDiapason(float f1, float f2, float d){
        return f1 + d > f2 && f2 > f1 - d;
    }
}
