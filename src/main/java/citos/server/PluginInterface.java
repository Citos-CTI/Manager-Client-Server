/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server;

import com.google.common.eventbus.EventBus;

import java.io.IOException;

public interface PluginInterface {

    void dial(String partner1, String partner2);

    void loginOnTelephonyServer(EventBus eventBus) throws IOException ;

    void requestStatus(String extension);
    
}
