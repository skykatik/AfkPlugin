package sky.afk;

import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.*;
import com.google.gson.*;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;

import static mindustry.Vars.*;

public class AfkPlugin extends Plugin{
    public final ObjectMap<String, ActivityInfo> activities = new ObjectMap<>();

    private final Interval interval = new Interval();
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setPrettyPrinting()
            .create();

    protected static Config config;

    @Override
    public void init(){

        Fi cfg = dataDirectory.child("afk-plugin.json");
        if(!cfg.exists()){
            cfg.writeString(gson.toJson(config = new Config()));
            Log.info("Configuration file created... (@)", cfg.absolutePath());
        }else{
            config = gson.fromJson(cfg.reader(), Config.class);
        }

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

        Events.run(Trigger.update, () -> {
            if(interval.get(60f * 0.75f)){
                for(Player player : Groups.player){
                    ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                    boolean notAdmin = !player.admin() || player.admin() && !config.ignoreAdmins;
                    if(activity.isStand(player) & activity.isOldMessage(player) & activity.isOldBuildActivity()){
                        if(!activity.afk){
                            Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] at now inactive!",
                                    NetClient.colorizeName(player.id(), player.name())));
                            activity.afk = true;
                        }else if(config.warningsEnabled() && notAdmin && activity.warnings < config.maxWarningsCount){
                            Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] at now inactive! Warning (@/@)",
                                    NetClient.colorizeName(player.id(), player.name()), activity.warnings, config.maxWarningsCount));
                        }

                        activity.warnings++;

                        if(notAdmin && config.warningsEnabled() && activity.warnings > config.maxWarningsCount){
                            if(config.enableKick){
                                player.kick("You have been kicked for inactive from the server!", config.kickDuration);
                                return;
                            }

                            connectToHub(player);
                        }
                    }else{
                        activity.update(player);
                        activity.ifAfk();
                    }
                }
            }
        });
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

        /** Max warnings count. If {@code -1} then disabled. Default disabled */
        public int maxWarningsCount = -1;

        public boolean warningsEnabled(){
            return maxWarningsCount != -1;
        }
    }
}
