package us.circuitsoft.slack;

import com.google.gson.JsonObject;
import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.json.simple.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

/**
 * Created by John on 11/30/2014.
 */
public class SlackBungee extends ConfigurablePlugin implements Listener {

    private boolean n;

    public void onEnable() {
        getProxy().getPluginManager().registerListener(this,this);
        updateConfig("1.2.0");
        n = getConfig().getString("webhook").equals("https://hooks.slack.com/services/");
        if (n) {
            getLogger().severe("You have not set your webhook URL in the config!");
        }
    }


    @EventHandler
    public void onChat(ChatEvent ev) {
        ProxiedPlayer p = (ProxiedPlayer) ev.getSender();
        String m = ev.getMessage();
        if (m.startsWith("/")) {
            if (blacklist(m) && permCheck("slack.hide.command", p)) {
                payload(p.getServer().getInfo().getName()+": "+m, p.getName());
            }
            return;
        }
        if (permCheck("slack.hide.chat", p)) {
            payload("("+ p.getServer().getInfo().getName() + ") "+ '"' + m + '"', p.getName());
        }
    }

    @EventHandler
    public void onJoin(ServerSwitchEvent ev) {
        ProxiedPlayer p = (ProxiedPlayer) ev.getPlayer();
        if (permCheck("slack.hide.logout", p)) {
            payload("logged in: " + p.getServer().getInfo().getName(), p.getName());
        }
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent ev) {
        if(permCheck("slack.hide.logout", ev.getPlayer())) {
            payload("logged out", ev.getPlayer().getName());
        }
    }

    /**
     * Send a message to Slack.
     * @param m The message sent to Slack.
     * @param p The name of the sender of the message sent to Slack.
     * @return True if the message was successfully sent to Slack.
     */
    public boolean payload(String m, String p) {
        JsonObject j = new JsonObject();
        j.addProperty("text", p + ": " + m);
        j.addProperty("username", p);
        j.addProperty("icon_url", "https://cravatar.eu/helmhead/" + p + "/100.png");
        String b = "payload=" + j.toString();
        return post(b);
    }

    /**
     * Send a message to Slack with a custom user icon.
     * @param m The message sent to Slack.
     * @param p The name of the sender of the message sent to Slack.
     * @param i The URL of an image of the sender of the message sent to Slack. (recommended for non player messages).
     * @return True if the message was successfully sent to Slack.
     */
    public boolean payload(String m, String p, String i) {
        if(permCheck("slack.hide.*", getProxy().getPlayer(p))) {
            JsonObject j = new JsonObject();
            j.addProperty("text", p + ": " + m);
            j.addProperty("username", p);
            j.addProperty("icon_url", i);
            String b = "payload=" + j.toString();
            return post(b);
        } else {
            return false;
        }
    }

    private boolean post(String b) {
        int i = 0;
        if (n) {
            getLogger().severe("You have not set your webhook URL in the config!");
        } else {
            try {
                URL u = new URL(getConfig().getString("webhook"));
                HttpURLConnection C = (HttpURLConnection)u.openConnection();
                C.setRequestMethod("POST");
                C.setDoOutput(true);
                try (BufferedOutputStream B = new BufferedOutputStream(C.getOutputStream())) {
                    B.write(b.getBytes("utf8"));
                    B.flush();
                }
                i = C.getResponseCode();
                String o = Integer.toString(i);
                String c = C.getResponseMessage();
                //m8 y u make dis spam. psh
                getLogger().log(Level.INFO, "{0} {1}", new Object[]{o, c});
                C.disconnect();
            } catch (MalformedURLException e) {
                getLogger().log(Level.SEVERE, "URL is not valid: ", e);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "IO exception: ", e);
            }
        }
        return i == 200;
    }

    private boolean blacklist(String m) {
        return !getConfig().getStringList("blacklist").contains(m);
    }

    private void updateConfig(String v) {
        this.saveDefaultConfig();
        if (getConfig().getString("v") == null ? v != null : !getConfig().getString("v").equals(v)) {
            getConfig().options().copyDefaults(true);
            getConfig().set("version", v);
        }
        this.saveConfig();
    }

    private boolean permCheck(String c, ProxiedPlayer p) {
        return !p.hasPermission(c);
    }


}
