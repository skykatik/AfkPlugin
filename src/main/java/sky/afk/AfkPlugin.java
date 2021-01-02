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
        Fi cfg = dataDirectory.child("mods/afk-plugin.json");
        if(!cfg.exists()){
            config = new Config();
            cfg.writeString(gson.toJson(config));
        }
        config = gson.fromJson(cfg.reader(), Config.class);
    }

    @Override
    public void init(){

        Events.on(TapEvent.class, event -> {
            Player player = event.player;
            if(player != null) {
                ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                activity.ifAfk();
            }
        });

        Events.on(PlayerChatEvent.class, event -> {
            Player player = event.player;
            if(player != null) {
                ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                activity.ifAfk();
            }
        });

        Events.on(PlayerJoin.class, event -> activities.put(event.player.uuid(), new ActivityInfo(event.player)));

        Events.on(PlayerLeave.class, event -> activities.remove(event.player.uuid()));

        Events.on(BlockBuildBeginEvent.class, event -> {
            if(event.unit.isPlayer()){
                Player player = (Player)event.unit.controller();
                if(player != null) {
                    ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                    activity.lastBuildActivityTime = Time.millis();
                    activity.ifAfk();
                }
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
            if(state.isPlaying()){
                //Call.sendMessage("running afk timer");
                for(Player player : Groups.player){
                    if (player==null) continue;
                    ActivityInfo activity = activities.get(player.uuid(), () -> new ActivityInfo(player));
                    Log.debug("@ | @ | @ | @", !activity.afk, activity.isStand(player), activity.isOldMessage(player), Time.timeSinceMillis(activity.lastBuildActivityTime) < config.inactivityTime);
                    if(!activity.afk && activity.isStand(player) && activity.isOldMessage(player) ^ Time.timeSinceMillis(activity.lastBuildActivityTime) < config.inactivityTime){

                        if (activity.warns >(config.warnthreshold+1)) {
                            activity.afk = true;
                            if (config.enableKick) {
                                if (!player.admin() || player.admin && !config.ignoreAdmins) {
                                    Call.sendMessage(Strings.format("[lightgray]Player @[lightgray] is kicked for afk!", NetClient.colorizeName(player.id(), player.name())));
                                    //player.kick("You have been kicked for being afk!", (int)config.kickDuration);
                                    Vars.net.pingHost(config.kickurl, config.kickport, host -> {
                                        Call.infoMessage(player.con, "You have been moved into hub for AFK ;-;");
                                        Call.connect(player.con, config.kickurl, config.kickport);
                                    }, (e) -> {
                                    });
                                }else{
                                    player.sendMessage(Strings.format("[lightgray]Player @[lightgray] is afk!!", NetClient.colorizeName(player.id(), player.name())));
                                }
                            }else{
                                player.sendMessage(Strings.format("[lightgray]Player @[lightgray] is afk!", NetClient.colorizeName(player.id(), player.name())));
                            }
                        }else{
                            if (activity.warns != 0) {
                                player.sendMessage(Strings.format("[lightgray]Player @[lightgray] is afk! Warning (" + (activity.warns) + "/" + (config.warnthreshold+1) + ")", NetClient.colorizeName(player.id(), player.name())));
                                //Call.infoMessage(player.con,"AFK Warning! ("+(activity.warns+1)+"/"+config.warnthreshold+")");
                            }
                            activity.warns++;
                            activity.update(player);
                        }
                    }else if (!activity.isStand(player)) {
                        activity.ifAfk();
                        activity.update(player);
                    }else{
                            activity.warns=0;
                            activity.update(player);
                        }
                    }
                }

        }, 2, 60);
    }

    static class Config{

        /** Enable auto kick */
        public boolean enableKick = true;

        /** Kick inactive admins */
        public boolean ignoreAdmins = true;

        /** Inactivity player time. In milliseconds. Default 1 minutes */
        public long inactivityTime = 1000 * 60 * 1;

        /** warning threshold, number of times for warnings before kick*/
        public int warnthreshold = 3;// first warn is "free" , so total warns is warnthreshold+1
        /** Kick duration. Default 1 second */
        public long kickDuration = 1000 * 1;
        /** add kick url, kicks player into hub url */
        public String kickurl = "alexmindustryhub.ddns.net";
        /** add kick url, kicks player into hub port */
        public int kickport = 6568;
    }
}
