/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server.busevents;

import lsctic.communication.server.packet.CdrPacket;

public class NotifyNewCdrEvent {
    private CdrPacket cdrPacket;

    public NotifyNewCdrEvent(CdrPacket cdrPacket) {
        this.cdrPacket = cdrPacket;
    }

    public CdrPacket getCdrPacket() {
        return cdrPacket;
    }
}
