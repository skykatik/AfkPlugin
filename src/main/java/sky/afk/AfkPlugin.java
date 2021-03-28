package sky.afk;

import arc.Events;
import arc.files.Fi;
import arc.struct.*;
import arc.util.*;
import com.google.gson.*;
import mindustry.Vars;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.ui.dialogs.JoinDialog;

import static mindustry.Vars.*;

public class AfkPlugin extends Plugin{
    public final ObjectMap<String, ActivityInfo> activities = new ObjectMap<>();

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setPrettyPrinting()
            .create();

    protected static Config config;

    public AfkPlugin(){
        Fi cfg = dataDirectory.child("afk-plugin.json");
        if(!cfg.exists()){
            cfg.writeString(gson.toJson(config = new Config()));
        }else{
            config = gson.fromJson(cfg.reader(), Config.class);
        }
    }

    @Override
    public void init(){

        Events.on(TapEvent.class, event -> {
            Player player = event.player;
            if(player != null){
                ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                activity.ifAfk();
            }
        });

        Events.on(PlayerChatEvent.class, event -> {
            Player player = event.player;
            ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
            activity.ifAfk();
        });

        Events.on(PlayerJoin.class, event -> activities.put(event.player.uuid(), new ActivityInfo(event.player)));

        Events.on(PlayerLeave.class, event -> activities.remove(event.player.uuid()));

        Events.on(BlockBuildBeginEvent.class, event -> {
            if(event.unit.isPlayer()){
                Player player = (Player)event.unit.controller();
                ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                activity.lastBuildActivityTime = Time.millis();
                activity.ifAfk();
            }
        });

        Events.on(ConfigEvent.class, event -> {
            Player player = event.player;
            if(player != null){
                ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                activity.lastBuildActivityTime = Time.millis();
                activity.ifAfk();
            }
        });

        Timer.schedule(() -> {
            for(Player player : Groups.player){
                ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                if(!activity.afk && activity.isStand(player) && activity.isOldMessage(player) ^ Time.timeSinceMillis(activity.lastBuildActivityTime) < config.inactivityTime){
                    Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] at now inactive!", NetClient.colorizeName(player.id(), player.name())));
                    activity.afk = true;
                    if(config.enableKick && (!player.admin() || player.admin() && !config.ignoreAdmins)){
                        player.kick("You have been kicked for inactive from the server!", (int)config.kickDuration);
                        connectToHub(player);
                    }
                }else{
                    activity.update(player);
                }
            }
        }, 5, 15);
    }

    private void connectToHub(Player player){
        if(config.hubIp != null){
            String ip = config.hubIp;
            int port = Vars.port;
            String[] parts = ip.split(":");
            if(ip.contains(":") && Strings.canParsePositiveInt(parts[1])){
                ip = parts[0];
                port = Strings.parseInt(parts[1]);
            }
            Call.connect(player.con, ip, port);
        }
    }

    static class Config{

        /** Enable auto kick */
        public boolean enableKick;

        /** Kick inactive admins */
        public boolean ignoreAdmins = true;

        /** Hub IP address. write in the format <b>ip:port</b>. Default disabled */
        public String hubIp = null;

        /** Inactivity player time. In milliseconds. Default 30 minutes */
        public long inactivityTime = 1000 * 60 * 30;

        /** Kick duration. In milliseconds. Default 1 second */
        public long kickDuration = 1000;

        // TODO: implement
        /** Max warnings count. If {@code -1} then disabled. Default disabled */
        public int maxWarningsCount = -1;
    }
}
