/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.netty.channel.Channel;
import lsctic.communication.server.busevents.NotifyExtensionAbosEvent;
import lsctic.communication.server.busevents.NotifyNewCdrEvent;
import lsctic.communication.server.database.SqlLiteCallRecordDatabase;
import lsctic.communication.server.packet.CdrPacket;
import lsctic.communication.server.pluginlicensecheck.ClientPluginChecker;
import lsctic.communication.server.pluginlicensecheck.PluginLicenseState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelController {
    private EventBus eventBus;

    private final HashMap<String, Integer> internStatus;
    private final HashMap<String, ArrayList<Channel>> ExtensionsAboPerChannel;
    private final HashMap<String, ArrayList<Channel>> cdrPerChannel;
    private final SqlLiteCallRecordDatabase sqlDb;
    private final PluginInterface loadedPlugin;
    private final ClientPluginChecker clientPluginChecker;


    public ChannelController(PluginInterface loadedPlugin) {
        this.eventBus = new EventBus();
        eventBus.register(this);
        try {
            loadedPlugin.loginOnTelephonyServer(eventBus);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to connect to telephone server. Please check your configuration file.", e);
            System.exit(0);
        }
        ExtensionsAboPerChannel
                = new HashMap<>();
        internStatus = new HashMap<>();
        cdrPerChannel
                = new HashMap<>();
        sqlDb = new SqlLiteCallRecordDatabase();

        this.loadedPlugin = loadedPlugin;

        this.clientPluginChecker = new ClientPluginChecker();
    }


    public void subscribeStatusForExtension(String extension, Channel threadChannel) {
        if (!ExtensionsAboPerChannel.containsKey(extension)) {
            ArrayList<Channel> ar = new ArrayList();
            ar.add(threadChannel);
            ExtensionsAboPerChannel.put(extension, ar);
        } else {
            ExtensionsAboPerChannel.get(extension).add(threadChannel);
        }
        if (!internStatus.containsKey(extension)) {
            internStatus.put(extension, 5);
        }
        //first send the user the last state that was saved
        notifyNewAboAboutExtensionState(extension, threadChannel);
        //then order the new state from the telephony server
        loadedPlugin.requestStatus(extension);
    }

    private void notifyNewAboAboutExtensionState(String changed, Channel threadChannel) {
        threadChannel.writeAndFlush("000" + changed + ";" + internStatus.get(changed) + "\r\n");
    }


    public void unsubscribeStatusForExtension(String param, Channel threadChannel) {
        for (Map.Entry<String, ArrayList<Channel>> entry : ExtensionsAboPerChannel.entrySet()) {
            if (entry.getKey().equals(param)) {
                entry.getValue().remove(threadChannel);
            }
        }
    }


    public void unsubscribeStatusForAllExtensions(Channel threadChannel) {
        for (Map.Entry<String, ArrayList<Channel>> entry : ExtensionsAboPerChannel.entrySet()) {
            entry.getValue().remove(threadChannel);
        }
    }


    public void createCall(String param) {
        String[] partner = param.split(";");
        loadedPlugin.dial(partner[0], partner[1]);
    }


    public void subscribeCdrForExtension(String param, Channel threadChannel) {
        if (!cdrPerChannel.containsKey(param)) {
            ArrayList<Channel> channels = new ArrayList<>();
            channels.add(threadChannel);
            cdrPerChannel.put(param, channels);
        } else {
            cdrPerChannel.get(param).add(threadChannel);
        }
    }


    @Subscribe
    public void notifyAllAboAboutExtensionState(NotifyExtensionAbosEvent event) {
        String extension = event.getExtension();
        int state = event.getState();
        internStatus.put(extension, state);
        for (Map.Entry<String, ArrayList<Channel>> entry : ExtensionsAboPerChannel.entrySet()) {
            if (entry.getKey().equals(extension)) {
                for (Channel ch : entry.getValue()) {
                    ch.writeAndFlush("000" + extension + ";" + state + "\r\n");
                }
            }
        }

    }


    @Subscribe
    public void notifyAboutCdrChange(NotifyNewCdrEvent event) {
        CdrPacket cdr = event.getCdrPacket();
        String extensionSource = cdr.getSource();
        String extensionDestination = cdr.getDestination();
        String databaseId = sqlDb.insertCdrInDatabase(cdr);
        for (Map.Entry<String, ArrayList<Channel>> entry : cdrPerChannel.entrySet()) {
            if (entry.getKey().equals(extensionSource)|| entry.getKey().equals(extensionDestination)) {
                for (Channel channel : entry.getValue()) {
                    channel.writeAndFlush("010" + cdr.getSource() + ";" + cdr.getDestination()
                            + ";" + cdr.getStartTime() + ";" + cdr.getDuration() + ";" + cdr.getDisposition()
                            + ";false;;;" + (cdr.isInternalCall() ? 1 : 0) + ";" + cdr.getCountryCode()
                            + ";" + cdr.getPrefix() + "\r\n");
                    isMoreCdrAvailable(extensionSource, channel);
                }
            }
        }
    }

    public void getArchivedCdrsPage(String extension, String param, Channel chan) {
        String[] val = param.split(";");
        List<CdrPacket> cdrs = sqlDb.getLastNCallsStartingFrom(extension, Integer.valueOf(val[0]), Integer.valueOf(val[1]));
        for (CdrPacket cdr : cdrs) {
            chan.writeAndFlush("010" + cdr.getSource() + ";" + cdr.getDestination()
                    + ";" + cdr.getStartTime() + ";" + cdr.getDuration() + ";" + cdr.getDisposition()
                    + ";true;;;" + (cdr.isInternalCall() ? 1 : 0) + ";" + cdr.getCountryCode()
                    + ";" + cdr.getPrefix() + "\r\n");        }
        isMoreCdrAvailable(extension,chan);
    }

    public void removeCdrFromDatabase(String extension, String param, Channel ch) {
        String[] parameters = param.split(";");
        if(parameters.length<5) {
            return;
        }
        long timeStamp = Long.valueOf(parameters[0]);
        sqlDb.removeCdr(timeStamp,parameters[1],parameters[2]);

        //Keep the client up to date about the current database
        getArchivedCdrsPage(extension,parameters[3]+";"+parameters[4],ch);
        isMoreCdrAvailable(extension,ch);
    }

    public void getSearchedCdr(String extension, String param, Channel chan) {
        String[] val = param.split(";");
        List<CdrPacket> cdrs = sqlDb.getSearchedCdr(extension, val[0], Integer.valueOf(val[1]), Boolean.valueOf(val[3]));
        for (CdrPacket cdr : cdrs) {
            chan.writeAndFlush("010" + cdr.getSource() + ";" + cdr.getDestination() + ";" + cdr.getStartTime() + ";"
                    + cdr.getDuration() + ";" + cdr.getDisposition() +";true;"+val[0]+";"+val[2]+";"
                    + (cdr.isInternalCall() ? 1 : 0) + ";" + cdr.getCountryCode()
                    + ";" + cdr.getPrefix() + "\r\n");
        }
        if(cdrs.size()==0 && val.length>2) {
            chan.writeAndFlush("012"+val[2]+"\r\n");
            Logger.getLogger(getClass().getName()).info("VERSEUCHE:  "+ val[0]);
        }
    }

    // For synchronizing the users call record view with the actual data of the server this is required whenever a user interacts with cdr (or server with user)
    public void isMoreCdrAvailable(String extension, Channel ch){
        int available = sqlDb.count(extension);
        ch.writeAndFlush("011"+String.valueOf(available)+"\r\n");
    }


    public void checkPluginLicense(String param, String extension, Channel ch) {
        String[] parameters = param.split(";");
        if (parameters.length == 2) {
            PluginLicenseState licenseValid = clientPluginChecker.checkClientPlugin(parameters[1], extension);
            if (licenseValid.getState() == 1) {
                if (licenseValid.getDaysLeft() > 0) {
                    ch.writeAndFlush("019" + parameters[0] + ";" + licenseValid.getDaysLeft() + "\r\n");
                } else {
                    ch.writeAndFlush("015" + parameters[0] + "\r\n");
                }
            } else if (licenseValid.getState() == 0) {
                ch.writeAndFlush("016" + parameters[0] + "\r\n");
            } else if (licenseValid.getState() == 2) {
                ch.writeAndFlush("017" + parameters[0] + "\r\n");
            } else if (licenseValid.getState() == 3) {
                ch.writeAndFlush("018" + parameters[0] + "\r\n");
            }
        }
    }
}
