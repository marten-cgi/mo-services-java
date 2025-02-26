/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO JMS Transport Framework
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
package esa.mo.mal.transport.jms;

import esa.mo.mal.transport.gen.GENMessage;
import esa.mo.mal.transport.gen.GENMessageHeader;
import esa.mo.mal.transport.gen.PacketToString;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageDecoder;
import esa.mo.mal.transport.gen.receivers.GENIncomingMessageHolder;
import java.util.HashMap;
import org.ccsds.moims.mo.mal.MALException;

/**
 * Responsible for decoding newly arrived MAL Messages.
 */
final class JMSIncomingMessageDecoder implements GENIncomingMessageDecoder {

    private final JMSTransport transport;
    private final JMSUpdate jmsUpdate;

    /**
     * Constructor
     *
     * @param transport The transport instance to use.
     * @param jmsUpdate The raw message
     */
    public JMSIncomingMessageDecoder(final JMSTransport transport, JMSUpdate jmsUpdate) {
        this.transport = transport;
        this.jmsUpdate = jmsUpdate;
    }

    @Override
    public GENIncomingMessageHolder decodeAndCreateMessage() throws MALException {
        GENMessage malMsg = new GENMessage(false, true, new GENMessageHeader(), 
                new HashMap(), jmsUpdate.getDat(), transport.getStreamFactory());
        return new GENIncomingMessageHolder(malMsg.getHeader().getTransactionId(), 
                malMsg, new PacketToString(null));
    }
}
