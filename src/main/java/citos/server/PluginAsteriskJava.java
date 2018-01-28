/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server;

import com.google.common.eventbus.EventBus;
import citos.server.busevents.NotifyExtensionAbosEvent;
import citos.server.busevents.NotifyNewCdrEvent;
import citos.server.packet.CdrPacket;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.*;
import org.asteriskjava.manager.event.*;
import org.asteriskjava.manager.response.GetConfigResponse;
import org.asteriskjava.manager.response.ManagerResponse;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PluginAsteriskJava implements PluginInterface, ManagerEventListener {

    private final String amiServer;
    private final String amiUser;
    private final String amiUserPw;
    private final int amiPort;
    private EventBus eventBus;
    private ManagerConnection managerConnection;
    private List<String> trunks;
    private HashMap<String, Integer> prefixes;
    HashMap<String, String> userExtensions = new HashMap<>();

    HashMap<String, CdrPacket> cdrPacketHashList = new HashMap<>();


    protected PluginAsteriskJava(String amiServer, String amiUser, String amiUserPw, int amiPort) {
        this.amiServer = amiServer;
        this.amiUser = amiUser;
        this.amiUserPw = amiUserPw;
        this.amiPort = amiPort;

        ManagerConnectionFactory factory = new ManagerConnectionFactory(
                amiServer, amiPort, amiUser, amiUserPw);

        this.managerConnection = factory.createManagerConnection();

    }

    private void setDontDisturb(String extension, boolean value) {
        /**  if(value) {

         RedirectAction redirectAction = new RedirectAction();
         redirectAction.setChannel("Local/"+extension);
         redirectAction.setContext("default");
         redirectAction.setExten("1");
         redirectAction.setPriority(1);
         redirectAction.setT

         Logger.getLogger(getClass().getName()).info("redirection");
         try {
         this.managerConnection.sendAction(redirectAction, 5000);
         } catch (IOException e) {
         e.printStackTrace();
         } catch (TimeoutException e) {
         e.printStackTrace();
         }

         } else {

         }**/
    }

    @Override
    public void dial(String partner1, String partner2) {

        // Todo Fix this to let the client choose also the trunk
        Optional<Map.Entry<String, Integer>> opt = prefixes.entrySet().stream().findFirst();
        if(opt.isPresent() && partner2.startsWith("0")) {
            partner2 = opt.get().getValue() + partner2;
        }

        OriginateAction originateAction = new OriginateAction();
        originateAction.setChannel("Local/" + partner1);
        originateAction.setCallerId(partner1);
        originateAction.setExten(partner2);
        originateAction.setContext("default");
        originateAction.setPriority(1);
        originateAction.setTimeout(5000L);

        Logger.getLogger(getClass().getName()).info("Dialing between: " + partner1 + " and " + partner2);

        try {
            ManagerResponse response = managerConnection.sendAction(originateAction, 5000);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loginOnTelephonyServer(EventBus eventBus) throws IOException {
        // register for events
        managerConnection.addEventListener(this);

        this.eventBus = eventBus;

        // connect to Asterisk and log in
        try {
            managerConnection.login();
            managerConnection.sendAction(new StatusAction());
            managerConnection.sendAction(new ExtensionStateAction());
        } catch (IOException | TimeoutException | AuthenticationFailedException e) {
            throw new IOException(e);
        }

        // Get list of trunks to detect the cdr's and from which they came
        GetConfigAction getConfigAction = new GetConfigAction("sip.conf");
        try {
            ManagerResponse response = managerConnection.sendAction(getConfigAction, 5000);
            if (response instanceof GetConfigResponse) {
                GetConfigResponse response1 = (GetConfigResponse) response;

                trunks = response1.getCategories().entrySet().stream().filter((keys) -> response1.getLines(
                        keys.getKey()).values().stream().filter((str) -> str.contains("fromdomain=")).count() > 0)
                        .collect(Collectors.toList()).stream().map(Map.Entry::getValue).collect(Collectors.toList());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        // Get the prefix for the trunks -> Needed for further processing of the number
        getConfigAction = new GetConfigAction("extensions.conf");
        prefixes = new HashMap<>();

        try {
            ManagerResponse response = managerConnection.sendAction(getConfigAction, 5000);
            if (response instanceof GetConfigResponse) {
                GetConfigResponse response1 = (GetConfigResponse) response;

                List<Integer> er = response1.getCategories()
                        .entrySet().stream().filter(entry -> entry.getValue().equals("to-pstn")).collect(Collectors.toList())
                        .stream().map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                ArrayList<Pair> prefixesArr = new ArrayList<>();

                outer:
                for (String trunkPro : trunks) {
                    for (Integer category : er) {
                        for (String line : response1.getLines(category).values()) {
                            if (line.startsWith("exten=_") && line.contains("[+*#0-9]") && line.contains("@" + trunkPro) && line.contains("DIAL")) {
                                line = line.substring("exten=_".length());
                                String res[] = line.split("\\[");
                                prefixesArr.add(new Pair(Integer.parseInt(res[0]), trunkPro));
                                continue outer;
                            }
                        }
                    }
                }
                for (Pair pair : prefixesArr) {
                    prefixes.put(pair.getString(), pair.getInteger());
                }

                // Part 2 get the channels for the extensions

                Optional<Integer> er2 = response1.getCategories()
                        .entrySet().stream().filter(entry -> entry.getValue().equals("globals")).collect(Collectors.toList())
                        .stream().map(Map.Entry::getKey).findFirst();


                if (er2.isPresent()) {
                    for (String entry : response1.getLines(er2.get()).values()) {
                        if (entry.contains("=")) {
                            String[] s = entry.split("=");
                            if (s.length == 2) {
                                userExtensions.put(s[1], s[0]);
                            }
                        }
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void requestStatus(String extension) {
        try {
            // If in the future contexts should be added, do that here
            managerConnection.sendAction(new ExtensionStateAction(extension, ""), new SendActionCallback() {
                @Override
                public void onResponse(ManagerResponse managerResponse) {
                    eventBus.post(new NotifyExtensionAbosEvent(extension, Integer.parseInt(managerResponse.getAttribute("status"))));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onManagerEvent(ManagerEvent event) {
        if (event instanceof ExtensionStatusEvent) {
            ExtensionStatusEvent extensionStatusEvent = (ExtensionStatusEvent) event;
            int status = extensionStatusEvent.getStatus();
            String hint = extensionStatusEvent.getHint();
            String[] temps = hint.split(":");
            String callerId = temps[temps.length - 1];
            eventBus.post(new NotifyExtensionAbosEvent(callerId, status));
            if(0 == status && cdrPacketHashList.containsKey(callerId)) {
                eventBus.post(new NotifyNewCdrEvent(cdrPacketHashList.get(callerId)));
                cdrPacketHashList.remove(callerId);
            }
        } else if (event instanceof CdrEvent) {
            CdrEvent cdrEvent = (CdrEvent) event;
            String sipChanSrc = "";
            String sipChanDes = "";
            String localChanSrc = "";
            if (cdrEvent.getChannel().startsWith("SIP")) {
                String[] first = cdrEvent.getChannel().split("/");
                String[] second = first[1].split("-");
                sipChanSrc = second[0];
            }
            if (cdrEvent.getDestinationChannel().startsWith("SIP")) {
                String[] first = cdrEvent.getDestinationChannel().split("/");
                String[] second = first[1].split("-");
                sipChanDes = second[0];
            }
            if (cdrEvent.getChannel().startsWith("Local")) {
                String[] first = cdrEvent.getDestinationChannel().split("/");
                first[1] = first[1].replace("@", "-");
                String[] second = first[1].split("-");
                localChanSrc = second[0];
            }

            String str = cdrEvent.getDisposition();
            int disposition = 0;
            switch (str) {
                case "NO ANSWER":
                    break;
                case "CONGESTION":
                    disposition = 1;
                    break;
                case "FAILED":
                    disposition = 2;
                    break;
                case "BUSY":
                    disposition = 3;
                    break;
                case "ANSWERED":
                    disposition = 4;
                    break;
            }

            CdrPacket cdrPacket = null;
            if (trunks.contains(sipChanSrc) && cdrEvent.getChannel().startsWith("SIP")) {
                cdrPacket = new CdrPacket(cdrEvent.getSrc().substring((int) Math.log10(prefixes.get(sipChanSrc)) + 1), userExtensions.get(sipChanDes),
                        cdrEvent.getStartTimeAsDate().getTime(), cdrEvent.getBillableSeconds().longValue(), disposition, false, Integer.parseInt(cdrEvent.getSrc().substring(0, (int) Math.log10(prefixes.get(sipChanSrc)) + 1)), false);
                requestStatusAndDeliverCDR(cdrPacket, userExtensions.get(sipChanDes));
            } else if (trunks.contains(sipChanDes) && cdrEvent.getChannel().startsWith("SIP")) {
                cdrPacket = new CdrPacket(userExtensions.get(sipChanSrc), cdrEvent.getDestination().substring((int) Math.log10(prefixes.get(sipChanDes)) + 1),
                        cdrEvent.getStartTimeAsDate().getTime(), cdrEvent.getBillableSeconds().longValue(), disposition, false, Integer.parseInt(cdrEvent.getDestination().substring(0, (int) Math.log10(prefixes.get(sipChanDes)) + 1)), true);
                requestStatusAndDeliverCDR(cdrPacket, userExtensions.get(sipChanSrc));
            } else {
                if (cdrEvent.getChannel().startsWith("SIP")) {
                    cdrPacket = new CdrPacket(userExtensions.get(sipChanSrc), userExtensions.get(sipChanDes), cdrEvent.getStartTimeAsDate().getTime(), cdrEvent.getBillableSeconds().longValue(), disposition, true, -1, -1);
                    requestStatusAndDeliverCDR(cdrPacket, cdrEvent.getSrc());
                } else if (cdrEvent.getChannel().startsWith("Local") && userExtensions.containsKey(sipChanDes) && !localChanSrc.equals(userExtensions.get(sipChanDes))) {


                    cdrPacket = new CdrPacket(localChanSrc, userExtensions.get(sipChanDes), cdrEvent.getStartTimeAsDate().getTime(), cdrEvent.getBillableSeconds().longValue(), disposition, true, -1, -1);

                    requestStatusAndDeliverCDR(cdrPacket, localChanSrc);
                } else {
                    return;
                }
            }



            /**     if(cdrPacketHashList.containsKey(userExtensions.get(sipChanSrc))) {
             cdrPacketHashList.put(sipChanSrc, cdrPacket);
             if (0 == requestStatusCode(userExtensions.get(sipChanSrc))) {
             eventBus.post(new NotifyNewCdrEvent(cdrPacket));
             }

             } else if(cdrPacketHashList.containsKey(userExtensions.get(sipChanDes))) {
             if (0 == requestStatusCode(userExtensions.get(sipChanDes))) {
             eventBus.post(new NotifyNewCdrEvent(cdrPacket));
             }
             } else if(cdrPacketHashList.containsKey(localChanSrc)) {
             if (0 == requestStatusCode(userExtensions.get(localChanSrc))) {
             eventBus.post(new NotifyNewCdrEvent(cdrPacket));
             }
             } else {
             if(sipChanSrc.length()>0) {
             cdrPacketHashList.put(sipChanSrc, cdrPacket);
             if (0 == requestStatusCode(userExtensions.get(sipChanDes))) {
             eventBus.post(new NotifyNewCdrEvent(cdrPacket));
             }
             } else if(localChanSrc.length()>0) {
             cdrPacketHashList.put(localChanSrc, cdrPacket);
             if(0 == )
             }
             }
             */


        }
    }

    public void logoff() {
        managerConnection.logoff();
    }

    public void requestStatusAndDeliverCDR(CdrPacket cdrPacket, String extension) {
        try {
            // If in the future contexts should be added, do that here
            managerConnection.sendAction(new ExtensionStateAction(extension, ""), new SendActionCallback() {
                @Override
                public void onResponse(ManagerResponse managerResponse) {
                    int status = Integer.parseInt(managerResponse.getAttribute("status"));
                    if(0 == status && cdrPacket.getDisposition() == 4){
                        cdrPacketHashList.remove(extension);
                        eventBus.post(new NotifyNewCdrEvent(cdrPacket));
                    } else {
                        cdrPacketHashList.put(extension, cdrPacket);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
