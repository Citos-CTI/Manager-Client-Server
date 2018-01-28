/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.packet;

import java.util.Objects;

public class NewestCdrPacket {
    private String internalUser;
    private CdrPacket cdrPacket;

    public NewestCdrPacket(String internalUser, CdrPacket cdrPacket) {
        this.internalUser = internalUser;
        this.cdrPacket = cdrPacket;
    }

    public String getInternalUser() {
        return internalUser;
    }

    public CdrPacket getCdrPacket() {
        return cdrPacket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewestCdrPacket that = (NewestCdrPacket) o;
        return Objects.equals(internalUser, that.internalUser) &&
                Objects.equals(cdrPacket, that.cdrPacket);
    }

    @Override
    public int hashCode() {

        return Objects.hash(internalUser, cdrPacket);
    }
}
