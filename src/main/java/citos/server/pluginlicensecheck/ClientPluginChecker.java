/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.pluginlicensecheck;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ClientPluginChecker {
    private Decryptor decryptor;
    private final HashMap<Long, ArrayList<String>> clientsDataPluginRegistered;

    public ClientPluginChecker() {
        this.decryptor = new Decryptor("pub.key");
        this.clientsDataPluginRegistered = new HashMap<>();
    }

    public PluginLicenseState checkClientPlugin(String license, String extension) {

        // If the server certificate is not found there will be no correct handled plugin.
        if (!decryptor.isSetUpSuccess()) {
            return new PluginLicenseState(0);
        }
        String message = decryptor.decryptString(license);
        if (message.length() == 0) {
            return new PluginLicenseState(0);
        }
        String[] data = message.split(";");

        long amount = Long.parseLong(data[1]);
        long id = Long.parseLong(data[2]);
        long validUntil = Long.parseLong(data[3]);

        Logger.getLogger(getClass().getName()).info("id: " + id + " amount: " + amount + "valid Until: " + new Date(new Timestamp(validUntil).getTime()));

        if (data.length < 4) {
            return new PluginLicenseState(0);
        }
        //Check if the time constraints are held for the test phase. In the test phase there is no user restriction
        if (validUntil != 0) {
            Long currentTime = System.currentTimeMillis();
            if (validUntil < currentTime) {
                return new PluginLicenseState(0);
            } else {
                long diff = validUntil - currentTime;
                long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                return new PluginLicenseState(1, days);
            }
        }
        return addToHashMapIfEnoughLicenses(amount, id, extension);
    }

    private PluginLicenseState addToHashMapIfEnoughLicenses(long amount, long id, String extension) {
        Logger.getLogger(getClass().getName()).info("Advanced License Check");
        if (clientsDataPluginRegistered.containsKey(id)) {
            if (clientsDataPluginRegistered.get(id).contains(extension)) {
                Logger.getLogger(getClass().getName()).info("Has already registered for this plugin");
                return new PluginLicenseState(1);
            }
            else if (clientsDataPluginRegistered.get(id).size() < amount) {
                if (!clientsDataPluginRegistered.get(id).contains(extension)) {
                    clientsDataPluginRegistered.get(id).add(extension);
                    Logger.getLogger(getClass().getName()).info("Added " + extension + " to users");
                    return new PluginLicenseState(1);
                }
            }  else {
                return new PluginLicenseState(2);
            }
        } else {
            if (amount > 0) {
                clientsDataPluginRegistered.put(id, new ArrayList<>());
                clientsDataPluginRegistered.get(id).add(extension);
                Logger.getLogger(getClass().getName()).info("Added " + extension + " to users lower");
                return new PluginLicenseState(1);
            }
        }
        return new PluginLicenseState(0);

    }
}
