/* ----------------------------------------------------------------------------
 * Copyright (C) 2023      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO Generic Transport Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.mal.transport.gen.receivers;

import esa.mo.mal.transport.gen.GENReceptionHandler;
import esa.mo.mal.transport.gen.GENTransport;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.transport.MALTransmitErrorException;

/**
 * This Runnable task is responsible for decoding newly arrived MAL Messages and
 * passing to the transport executor.
 */
public class GENIncomingMessageReceiver implements Runnable {

    protected final GENTransport transport;
    protected final GENReceptionHandler receptionHandler;
    protected final GENIncomingMessageDecoder decoder;

    /**
     * Constructor
     *
     * @param transport Containing transport.
     * @param receptionHandler The reception handler to pass them to.
     * @param decoder The class responsible for decoding the message from the
     * incoming connection
     */
    public GENIncomingMessageReceiver(final GENTransport transport,
            final GENReceptionHandler receptionHandler,
            final GENIncomingMessageDecoder decoder) {
        this.transport = transport;
        this.receptionHandler = receptionHandler;
        this.decoder = decoder;
    }

    /**
     * This method processes an incoming message and then forwards it for
     * routing to the appropriate message queue. The processing consists of
     * transforming the raw message to the appropriate format and then
     * registering if necessary the communication channel.
     */
    @Override
    public void run() {
        try {
            GENIncomingMessageHolder msg = decoder.decodeAndCreateMessage();

            // the decoder may return null for transports that support fragmentation
            if (null != msg) {
                GENTransport.LOGGER.log(Level.FINE,
                        "Receving message : {0} : {1}",
                        new Object[]{msg.malMsg.getHeader().getTransactionId(), msg.smsg});

                //register communication channel if needed
                transport.manageCommunicationChannel(msg.malMsg, true, receptionHandler);
                transport.receiveIncomingMessage(msg);
            }
        } catch (MALException e) {
            GENTransport.LOGGER.log(Level.WARNING,
                    "Error occurred when decoding data : {0}", e);

            transport.communicationError(null, receptionHandler);
        } catch (MALTransmitErrorException e) {
            GENTransport.LOGGER.log(Level.WARNING,
                    "Error occurred when decoding data : {0}", e);

            transport.communicationError(null, receptionHandler);
        }
    }
}
