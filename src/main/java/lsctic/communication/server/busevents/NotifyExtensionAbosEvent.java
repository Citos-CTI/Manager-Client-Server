/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server.busevents;

public class NotifyExtensionAbosEvent {
    private String extension;
    private int state;

    public NotifyExtensionAbosEvent(String extension, int state) {
        this.extension = extension;
        this.state = state;
    }

    public String getExtension() {
        return extension;
    }

    public int getState() {
        return state;
    }
}
