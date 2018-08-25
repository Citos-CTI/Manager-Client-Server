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
        internStatus.put("201", 0);
        internStatus.put("202", 0);
        internStatus.put("203", 0);
        internStatus.put("204", 0);

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
        } else if (act < 25) {
            Random r = new Random();
            List<String> keyList = new ArrayList<>(internStatus.keySet());
            int i = r.nextInt(internStatus.size());
            CdrPacket cdrPa = new CdrPacket(keyList.get(i), "01723123451", System.currentTimeMillis(), 5, 4, true, 0, -1);
            eventBus.post(new NotifyNewCdrEvent(cdrPa));
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


}
