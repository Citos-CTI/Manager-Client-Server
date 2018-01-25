/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server.packet;

public class StatusPacket extends Packet{
    private final String extension;
    private final int status;
    public StatusPacket(String extension, int status) {
        this.extension = extension;
        this.status = status;
    }

    public String getExtension() {
        return extension;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return 5;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StatusPacket other = (StatusPacket) obj;
        if (!this.extension.equals(other.extension)) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        return true;
    }
    
}
