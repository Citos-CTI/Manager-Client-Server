/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server;

import citos.server.busevents.NotifyExtensionAbosEvent;
import citos.server.busevents.NotifyNewCdrEvent;
import citos.server.packet.CdrPacket;
import com.google.common.eventbus.EventBus;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginDummyHeadless implements PluginInterface {

    private EventBus eventBus;
    private HashMap<String, Integer> internStatus;

    /**
     * Only for debugging purpose. With this you can check whether the connection to the clients work. Headless mode for servers without GUI.
     */
    public PluginDummyHeadless() {
        // not needed
    }

    @Override
    public void loginOnTelephonyServer(EventBus eventBus) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Login versucht an der AMI");
        // if login success
        this.eventBus = eventBus;
        // else use local variable to write back error
        internStatus = new HashMap<>();
        for(int i = 1; i<20; i++) {
            if (i < 10) {
                internStatus.put("20"+i,0);
            } else {
                internStatus.put("2"+i, 0);
            }
        }
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                randomOperation();
            }
        }, 0, 500);

    }

    private void randomOperation() {

        int act = (int) (Math.random() * 100);

        if (act < 20) {
            Random r = new Random();
            List<String> keyList = new ArrayList<>(internStatus.keySet());
            int i = r.nextInt(internStatus.size());
            int pot = r.nextInt(4);
            int[] actArr = {-1, 0, 1, 8};
            int action = actArr[pot];
            internStatus.put(keyList.get(i), action);
            eventBus.post(new NotifyExtensionAbosEvent(keyList.get(i), action));
       // } else if (act < 21) {
         //  generateRandomCdrPacket(1, "200");
        }

    }

    @Override
    public void requestStatus(String extension) {
        if (internStatus.containsKey(extension)) {
            eventBus.post(new NotifyExtensionAbosEvent(extension, internStatus.get(extension)));
        }
    }

    public void dial(String extension, String number) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Verbindung initialisiert zwischen: {0} und: {1}", new Object[]{extension, number});
    }

    public void generateRandomCdrPacket(int anzahl, String extension) {
        Random r = new Random();
        for(int i = 0; i <= anzahl; ++i) {
            long time_ago = r.nextInt(10080000);
            int duration = r.nextInt(200);
            boolean internalCall = r.nextBoolean();
            String number;
            int countryCode;
            if (internalCall) {
                List<String> keyList = new ArrayList<>(internStatus.keySet());
                int ipos = r.nextInt(internStatus.size());
                number = keyList.get(ipos);
                countryCode = -1;
            } else {
                number = generateRandomPhoneNumber();
                countryCode = 49;
            }
            generateAndPublishCdrPacketRandomOrder(extension, number, System.currentTimeMillis()-time_ago, duration, 4, internalCall, 0, countryCode);
        }

    }

    private void generateAndPublishCdrPacketRandomOrder(String source, String destination, long startTime, long duration, int disposition, boolean internalCall, int prefix, int countryCode) {
        Random r = new Random();
        boolean sort = r.nextBoolean();
        CdrPacket cdrPa;
        if (sort) {
            cdrPa = new CdrPacket(source, destination, startTime, duration, disposition, internalCall, prefix, countryCode);
        } else {
            cdrPa = new CdrPacket(destination, source, startTime, duration, disposition, internalCall, prefix, countryCode);
        }
        eventBus.post(new NotifyNewCdrEvent(cdrPa));
    }

    private String generateRandomPhoneNumber(){
        String phonenumber = "017";
        Random r = new Random();
        for(int i = 0; i<=8; ++i) {
            int ext = r.nextInt(10);
            phonenumber = phonenumber + ext;
        }
        return phonenumber;
    }

}
