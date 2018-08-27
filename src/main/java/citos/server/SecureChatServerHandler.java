/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server;

import citos.server.database.SqliteUserDatabase;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.mindrot.jbcrypt.BCrypt;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SecureChatServerHandler extends SimpleChannelInboundHandler<String> {

    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final static String HASH_CONST = "Select passwordhash from users where username='";
    private final ChannelController channelController;
    private boolean loggedIn;
    private String extension = "";

    protected SecureChatServerHandler(ChannelController channelController) {
        this.channelController = channelController;
        this.loggedIn = false;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {

        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(Future<Channel> future) {
                        //Todo: Maybe Add initial data exchange here
                     /*  ctx.writeAndFlush(
                                "Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n");
                        ctx.writeAndFlush(
                                "Your session is protected by " +
                                        ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +
                                        " cipher suite.\n");
                    */
                        Logger.getLogger(getClass().getName()).log(Level.INFO, "SSL-Connected");
                        channels.add(ctx.channel());
                    }
                });
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) {
        //This will wait until a line of text has been sent
        Channel ch = ctx.channel();
        if (loggedIn) {
            if ("loff".equals(msg)) {
                loggedIn = false;
                ctx.channel().flush();
                ctx.channel().close();
                channels.remove(ctx);
            } else if (msg.startsWith("chpw")) {
                changePw(msg, ch);
            } else if (msg.length() > 2) {
                int op = Integer.parseInt(msg.substring(0, 3));
                String param = msg.substring(3, msg.length());
                switch (op) {
                    case 0:
                        channelController.subscribeStatusForExtension(param, ch);
                        break;
                    case 1:
                        channelController.unsubscribeStatusForExtension(param, ch);
                        break;
                    case 2:
                        channelController.unsubscribeStatusForAllExtensions(ch);
                        break;
                    case 3:
                        channelController.createCall(param);
                        break;
                    case 4:
                        channelController.subscribeCdrForExtension(param, ch);
                        break;
                    case 5:
                        // param start;count
                        channelController.getArchivedCdrsPage(extension, param, ch);
                        break;
                    case 6:
                        channelController.removeCdrFromDatabase(extension, param, ch);
                        break;
                    case 7:
                        channelController.isMoreCdrAvailable(extension, ch);
                        break;
                    case 8:
                        channelController.getSearchedCdr(extension,param, ch);
                        break;
                    case 11:
                        channelController.checkPluginLicense(param, extension, ch);
                        break;
                    default:
                        Logger.getLogger(getClass().getName()).info("Could not recognize order. Is your server software outdated?");
                }
            }
        } else {
            loggedIn = isLoginSuccessForUser(msg, ch);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, cause);
        ctx.close();
    }

    private boolean isLoginSuccessForUser(String param, Channel channel) {
        SqliteUserDatabase sqliteUserDatabase = SqliteUserDatabase.getInstance();
        String[] split = param.split(";");
        if (split[0].startsWith("ndb")) {
            split[0] = split[0].substring(3);
            if (split.length > 1) {
                String salt = sqliteUserDatabase.query("Select salt from users where username='" + split[0] + "'");
                String extension = sqliteUserDatabase.query("Select extension from users where username='" + split[0] + "'");
                if (salt.length() > 0 && extension.length() > 0) {
                    String hashed = BCrypt.hashpw(split[1], salt);
                    String safedHash = sqliteUserDatabase.query(HASH_CONST + split[0] + "'");
                    if (hashed.equals(safedHash)) {
                        channel.writeAndFlush("lsuc" + hashed + "\r\n");
                        this.extension = extension;
                        channel.writeAndFlush("owne" + extension + "\r\n");
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "User Login successful!");
                        return true;
                    }
                }
            }
        } else {
            if (split.length > 1) {
                String safedHash = sqliteUserDatabase.query(HASH_CONST + split[0] + "'");
                if (split[1].equals(safedHash)) {
                    channel.writeAndFlush("lsuc" + safedHash + "\r\n");
                    String extension = sqliteUserDatabase.query("Select extension from users where username='" + split[0] + "'");
                    if (extension.length() > 0) {
                        this.extension = extension;
                        channel.writeAndFlush("owne" + extension + "\r\n");
                        return true;
                    }
                }
            }
        }
        Logger.getLogger(getClass().getName()).info("Inform client about failed login attempt");
        channel.writeAndFlush("lfai" + "\r\n");
        return false;
    }

    private void changePw(String param, Channel channel) {
        SqliteUserDatabase sqliteUserDatabase = SqliteUserDatabase.getInstance();
        String parameters = param.substring(4);
        String[] split = parameters.split(";");
        if (split.length > 2) {
            String salt = sqliteUserDatabase.query("Select salt from users where username='" + split[0] + "'");
            String safedHash = sqliteUserDatabase.query("Select passwordhash from users where username='" + split[0] + "'");
            if(safedHash.length()>0 && salt.length()>0) {
                String hashed = BCrypt.hashpw(split[1], salt);
                if (hashed.equals(safedHash)) {
                    salt = BCrypt.gensalt();
                    hashed = BCrypt.hashpw(split[2], salt);
                    sqliteUserDatabase.updateStatementForHash(split[0], hashed, salt);
                    channel.writeAndFlush("lsuc" + hashed + "\r\n");
                    return;
                }
            }
        }
        channel.writeAndFlush("chfa" + "\r\n");
    }

}