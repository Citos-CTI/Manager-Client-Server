/*
 * Copyright (c) 2018.  Johannes Engler, Citos CTI
 */
package citos.server.packet;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Objects;

/**
 *
 * @author johannesengler
 */
public class CdrPacket extends Packet{
    private String source;
    private String destination;
    private final long startTime;
    private final long duration;
    private final int disposition;
    private final boolean internalCall;
    private final int prefix;
    private int countryCode;

    public CdrPacket(String source, String destination, long startTime, long duration, int disposition, boolean internalCall, int prefix, int countryCode) {
        this.source = source;
        this.destination = destination;
        this.startTime = startTime;
        this.duration = duration;
        this.disposition = disposition;
        this.internalCall = internalCall;
        this.prefix = prefix;
        this.countryCode = countryCode;
    }

    public CdrPacket(String source, String destination, long startTime, long duration, int disposition, boolean internalCall, int prefix, boolean outgoing) {
        this.source = source;
        this.destination = destination;
        this.startTime = startTime;
        this.duration = duration;
        this.disposition = disposition;
        this.internalCall = internalCall;
        this.prefix = prefix;
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        if(outgoing) {
            try {
                // TODO: Let admin set default region
                Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(destination, "DE");
                this.countryCode = phoneUtil.getCountryCodeForRegion(phoneUtil.getRegionCodeForNumber(phoneNumber));
                this.destination = phoneNumber.getNationalNumber() + "";

            } catch (NumberParseException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // TODO: Let admin set default region
                Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(source, "DE");
                this.countryCode = phoneUtil.getCountryCodeForRegion(phoneUtil.getRegionCodeForNumber(phoneNumber));
                this.source = phoneNumber.getNationalNumber() + "";

            } catch (NumberParseException e) {
                e.printStackTrace();
            }

        }
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public int getDisposition() {
        return disposition;
    }

    public boolean isInternalCall() {
        return internalCall;
    }

    public int getPrefix() {
        return prefix;
    }

    public int getCountryCode() {
        return countryCode;
    }

    @Override
    public String toString() {
        return "CdrPacket{" +
                "source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", disposition=" + disposition +
                ", internalCall=" + internalCall +
                ", prefix=" + prefix +
                ", countryCode=" + countryCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CdrPacket cdrPacket = (CdrPacket) o;
        return startTime == cdrPacket.startTime &&
                duration == cdrPacket.duration &&
                disposition == cdrPacket.disposition &&
                internalCall == cdrPacket.internalCall &&
                prefix == cdrPacket.prefix &&
                countryCode == cdrPacket.countryCode &&
                Objects.equals(source, cdrPacket.source) &&
                Objects.equals(destination, cdrPacket.destination);
    }

    @Override
    public int hashCode() {

        return Objects.hash(source, destination, startTime, duration, disposition, internalCall, prefix, countryCode);
    }
}
