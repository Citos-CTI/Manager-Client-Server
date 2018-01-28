/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server;

import com.google.common.eventbus.EventBus;
import citos.server.busevents.NotifyExtensionAbosEvent;
import citos.server.busevents.NotifyNewCdrEvent;
import citos.server.packet.CdrPacket;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginDummy implements PluginInterface {

    private EventBus eventBus;
    private HashMap<String, Integer> internStatus;

    /**
     * Only for debugging purpose. With this you can check whether the connection to the clients work.
     */
    public PluginDummy() {
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

        JFrame f = new JFrame();

        JPanel pane = new JPanel();
        f.add(pane);
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        for (String k : internStatus.keySet()) {
            JPanel p = new JPanel();
            final JLabel l = new JLabel(k);
            p.setLayout(new FlowLayout());
            final JTextField t = new JTextField();
            t.setText(String.valueOf(internStatus.get(k)));
            JButton b = new JButton("Send");
            b.addActionListener(e -> {
                eventBus.post(new NotifyExtensionAbosEvent(l.getText(), Integer.valueOf(t.getText())));
                internStatus.put(l.getText(),Integer.valueOf(t.getText()));
            });
            p.add(l);
            p.add(t);
            p.add(b);
            pane.add(p);
        }
        JPanel p = new JPanel();
        final JLabel l = new JLabel("Neu CDR");
        p.setLayout(new FlowLayout());
        final JTextField t = new JTextField();
        t.setText("201");
        final JTextField t2 = new JTextField();
        t2.setText("201");
        JButton b = new JButton("Send");
        b.addActionListener(e -> {
            CdrPacket cdrPa = new CdrPacket(t.getText(), t2.getText(), System.currentTimeMillis(), 5, 4, true, 0, -1);
            eventBus.post(new NotifyNewCdrEvent(cdrPa));

        });
        p.add(l);
        p.add(t);
        p.add(t2);
        p.add(b);
        pane.add(p);

        f.setSize(300, 300);
        f.setVisible(true);
    }

    @Override
    public void requestStatus(String extension) {
        if(internStatus.containsKey(extension)) {
            eventBus.post(new NotifyExtensionAbosEvent(extension, internStatus.get(extension)));
        }
    }

    public void dial(String extension, String number) {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Verbindung initialisiert zwischen: {0} und: {1}", new Object[]{extension, number});
    }












}
