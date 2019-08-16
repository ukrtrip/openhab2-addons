/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.broadlink.handler;

//import java.util.Arrays;
import java.math.BigDecimal;
import java.math.MathContext;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.broadlink.internal.BroadlinkProtocol;
import org.slf4j.LoggerFactory;


/**
 * Smart power socket handler
 *
 * @author John Marshall/Cato Sognen - Initial contribution
 */
public class BroadlinkSocketModel3Handler extends BroadlinkSocketModel2Handler {

    public BroadlinkSocketModel3Handler(Thing thing) {
        super(thing, LoggerFactory.getLogger(BroadlinkSocketModel3Handler.class));
    }

    protected boolean getStatusFromDevice() {
        int sw = 0;
        int on = 0;
        try {
            byte payload[] = new byte[16];
            payload[0] = 1;
            byte message[] = buildMessage((byte) 106, payload);
            byte response[] = sendAndReceiveDatagram(message, "status for socket");
            byte decodedPayload[] = BroadlinkProtocol.decodePacket(response, thingConfig, editProperties());
            if (decodedPayload[0] != payload[0] ) {
              while ( decodedPayload[0] != payload[0] ) {
                response = sendAndReceiveDatagram(message, "status for socket");
                decodedPayload = BroadlinkProtocol.decodePacket(response, thingConfig, editProperties());
                if (sw > 10) {
                  thingLogger.logError("maximum number of attempts to send identification packets has been reached: " + Integer.toString(sw) );
                  return false;
                }
                sw = sw + 1;
              }
            }
              // thingLogger.logInfo("[BL]STATUS ARR (" + Integer.toString(sw) + "): " + Arrays.toString(Arrays.copyOfRange(decodedPayload, 0, 20)) );
              if (decodedPayload[4] == 1 || decodedPayload[4] == 3 ) {
                updateState("powerOn", OnOffType.ON);
                on = 1;
              } else  if (decodedPayload[4] == 0 || decodedPayload[4] == 2 ) {
                updateState("powerOn", OnOffType.OFF);
              }
        } catch (Exception ex) {
            thingLogger.logError("Exception while getting status from device", ex);
            return false;
        }
        if(on == 1) {
         State state = null;
         try {
           byte payload2[] = new byte[16];
           payload2[0] = 8;
           payload2[2] = (byte) 254;
           payload2[3] = 1;
           payload2[4] = 5;
           payload2[5] = 1;
           payload2[9] = (byte) 45;
           byte message2[] = buildMessage((byte) 106, payload2);
           byte response2[] = sendAndReceiveDatagram(message2, "get PowerConsumption");
           byte decodedPayload2[] = BroadlinkProtocol.decodePacket(response2, thingConfig, editProperties());
           sw = 0;
           if (decodedPayload2[0] != payload2[0] ) {
              while ( decodedPayload2[0] != payload2[0] ) {
                response2 = sendAndReceiveDatagram(message2, "get PowerConsumption");
                decodedPayload2 = BroadlinkProtocol.decodePacket(response2, thingConfig, editProperties());
                if (sw > 9) {
                  thingLogger.logError("maximum number of attempts to send identification packets has been reached: " + Integer.toString(sw) );
                  return false;
                }
                sw = sw + 1;
              }
           }
           double num1 = Integer.parseInt( Integer.toHexString(decodedPayload2[7] & 0xff), 10);
           double num2 = Integer.parseInt( Integer.toHexString(decodedPayload2[6] & 0xff), 10 );
           double num3 = Integer.parseInt( Integer.toHexString(decodedPayload2[5] & 0xff), 10 );
           double sum = ( num1 * 10000 + num2 * 100 + num3 ) / 100;
           BigDecimal Energy = new BigDecimal(sum).round(new MathContext(3));
           state = new DecimalType((BigDecimal) Energy );
           // thingLogger.logInfo("[BL]EN.METER " + Energy.toString() + " Watt (" + Integer.toString(sw) + "): "    + Arrays.toString(Arrays.copyOfRange(decodedPayload2, 0, 20)) );
           updateState("powerConsumption", state);
         } catch (Exception ex) {
            thingLogger.logError("Exception while getting [powerConsumption] status from device", ex);
            return false;
         }
        } else {
            updateState("powerConsumption", UnDefType.UNDEF);
        }
        return true;
    }


    protected OnOffType getOnOffState(byte powerByte) {
      if (powerByte == 1 || powerByte == 3 || powerByte == 0xFD) {
        return OnOffType.ON;
      }
      return OnOffType.OFF;
    }


    protected boolean onBroadlinkDeviceBecomingReachable() {
       return getStatusFromDevice();
   }
}
