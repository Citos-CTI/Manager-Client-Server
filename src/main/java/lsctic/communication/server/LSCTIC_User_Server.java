/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package lsctic.communication.server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LSCTIC_User_Server
{
    private int port =12345;
    public static void main(String[] args)
    {
        new LSCTIC_User_Server();
    }
    public LSCTIC_User_Server()
    {

        try {

            HashMap<String, String> config = (HashMap<String, String>) ConfigLoader.loadConfig();

            if(config.containsKey("own_server_port")) {
                this.port = Integer.parseInt(config.get("own_server_port"));
            }

            if (!config.containsKey("plugin")) {
                System.out.println("No PBX plugin in server.conf defined. Exiting....");
                System.exit(0);
            }

            PluginInterface amiInt = null;

            switch (config.get("plugin")) {
                case "asterisk":
                    if (!(config.containsKey("port") &&
                            config.containsKey("password") &&
                            config.containsKey("username") &&
                            config.containsKey("server_address"))) {
                        System.out.println("Failure in config for plugin: " + config.get("plugin") + ". Exiting...");
                        System.exit(0);
                    }
                    amiInt = new PluginAsteriskJava(config.get("server_address"),
                            config.get("username"),
                            config.get("password"),
                            Integer.parseInt(config.get("port")));
                    break;
                case "dummy":
                    amiInt = new PluginDummy();
                    break;
                default:
                    System.out.println("The plugin you specified is not installed. Please look for typos or install the plugin. For further information see the documentation. Exiting...");
                    System.exit(0);
            }

            ChannelController channelController = new ChannelController(amiInt);

           //TODO add csv import
            CSVImporter.importCSVtoUserDatabase("/home/johannes", "users.csv");


            // Netty part for communication with clients
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .build();
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new SecureChatServerInitializer(sslCtx, channelController));
                
                b.bind(port).sync().channel().closeFuture().sync();
            } catch (InterruptedException ex) {
                Logger.getLogger(LSCTIC_User_Server.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
            Logger.getLogger(getClass().getName()).info("STARTED");
        } catch (CertificateException | SSLException ex) {
            Logger.getLogger(LSCTIC_User_Server.class.getName()).log(Level.SEVERE, null, ex);
        }
          
    }
}