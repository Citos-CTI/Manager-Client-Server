/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server.busevents;

import lsctic.communication.server.packet.CdrPacket;

public class CheckCdrValidityEvent {
    private int state;
    private CdrPacket cdrPacket;

    public CheckCdrValidityEvent(int state, CdrPacket cdrPacket) {
        this.state = state;
        this.cdrPacket = cdrPacket;
    }

    public int getState() {
        return state;
    }

    public CdrPacket getCdrPacket() {
        return cdrPacket;
    }
}
