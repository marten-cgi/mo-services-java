/* ----------------------------------------------------------------------------
 * Copyright (C) 2016      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : CCSDS MO MAL Java Implementation
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
package esa.mo.mal.impl.state;

import esa.mo.mal.impl.MALContextFactoryImpl;
import java.util.Map;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALProgressOperation;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.transport.MALErrorBody;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;

/**
 * Handles the state machine for a consumer for a PROGRESS operation.
 */
public final class ProgressOperationHandler extends BaseOperationHandler {

    private boolean receivedAck = false;
    private boolean receivedResponse = false;

    /**
     * Constructor.
     *
     * @param syncOperation true if this is a isSynchronous call.
     * @param responseHolder The response holder.
     */
    public ProgressOperationHandler(final boolean syncOperation,
            final OperationResponseHolder responseHolder) {
        super(syncOperation, responseHolder);
    }

    /**
     * Constructor.
     *
     * @param lastInteractionStage Last seen interaction stage.
     * @param responseHolder The response holder.
     */
    public ProgressOperationHandler(final UOctet lastInteractionStage,
            final OperationResponseHolder responseHolder) {
        super(false, responseHolder);
        final int interactionStage = lastInteractionStage.getValue();
        if ((interactionStage == MALProgressOperation._PROGRESS_ACK_STAGE)
                || (interactionStage == MALProgressOperation._PROGRESS_UPDATE_STAGE)) {
            receivedAck = true;
        }
    }

    @Override
    public MessageHandlerDetails handleStage(final MALMessage msg) throws MALInteractionException {
        final int interactionType = msg.getHeader().getInteractionType().getOrdinal();
        final int interactionStage = msg.getHeader().getInteractionStage().getValue();
        boolean isError = msg.getHeader().getIsErrorMessage();
        synchronized (this) {
            if (!receivedAck) {
                if ((interactionType == InteractionType._PROGRESS_INDEX)
                        && (interactionStage == MALProgressOperation._PROGRESS_ACK_STAGE)) {
                    receivedAck = true;
                    if (isError) {
                        receivedResponse = true;
                    }
                    return new MessageHandlerDetails(true, msg);
                } else {
                    receivedResponse = true;
                    logUnexpectedTransitionError(interactionType, interactionStage);
                    return new MessageHandlerDetails(true, msg, MALHelper.INCORRECT_STATE_ERROR_NUMBER);
                }
            } else if ((!receivedResponse) && (interactionType == InteractionType._PROGRESS_INDEX)
                    && ((interactionStage == MALProgressOperation._PROGRESS_UPDATE_STAGE)
                    || (interactionStage == MALProgressOperation._PROGRESS_RESPONSE_STAGE))) {
                if (interactionStage == MALProgressOperation._PROGRESS_UPDATE_STAGE) {
                    if (isError) {
                        receivedResponse = true;
                    }
                } else {
                    receivedResponse = true;
                }
                return new MessageHandlerDetails(false, msg);
            } else {
                receivedResponse = true;
                logUnexpectedTransitionError(interactionType, interactionStage);
                return new MessageHandlerDetails(!receivedAck, msg, MALHelper.INCORRECT_STATE_ERROR_NUMBER);
            }
        }
    }

    @Override
    public void processStage(final MessageHandlerDetails details) throws MALInteractionException {
        final int interactionStage = details.getMessage().getHeader().getInteractionStage().getValue();
        boolean isError = details.getMessage().getHeader().getIsErrorMessage();
        try {
            if (details.isAckStage()) {
                if (isSynchronous) {
                    responseHolder.signalResponse(isError, details.getMessage());
                } else if (isError) {
                    responseHolder.getListener().progressAckErrorReceived(
                            details.getMessage().getHeader(),
                            (MALErrorBody) details.getMessage().getBody(),
                            details.getMessage().getQoSProperties());
                } else {
                    responseHolder.getListener().progressAckReceived(
                            details.getMessage().getHeader(),
                            details.getMessage().getBody(),
                            details.getMessage().getQoSProperties());
                }
            } else if (interactionStage == MALProgressOperation._PROGRESS_UPDATE_STAGE) {
                if (isError) {
                    responseHolder.getListener().progressUpdateErrorReceived(
                            details.getMessage().getHeader(),
                            (MALErrorBody) details.getMessage().getBody(),
                            details.getMessage().getQoSProperties());
                } else {
                    responseHolder.getListener().progressUpdateReceived(
                            details.getMessage().getHeader(),
                            details.getMessage().getBody(),
                            details.getMessage().getQoSProperties());
                }
            } else if (isError) {
                responseHolder.getListener().progressResponseErrorReceived(
                        details.getMessage().getHeader(),
                        (MALErrorBody) details.getMessage().getBody(),
                        details.getMessage().getQoSProperties());
            } else {
                responseHolder.getListener().progressResponseReceived(
                        details.getMessage().getHeader(),
                        details.getMessage().getBody(),
                        details.getMessage().getQoSProperties());
            }

            if (details.isNeedToReturnAnException()) {
                throw new MALInteractionException(((MALErrorBody) details.getMessage().getBody()).getError());
            }
        } catch (MALException ex) {
            // nothing we can do with this
            MALContextFactoryImpl.LOGGER.log(Level.WARNING,
                    "Exception thrown handling stage {0}", ex);
        }
    }

    @Override
    public synchronized void handleError(final MALMessageHeader hdr,
            final MALStandardError err, final Map qosMap) {
        if (isSynchronous) {
            DummyErrorBody errBody = new DummyErrorBody(err);
            responseHolder.signalResponse(true, new DummyMessage(hdr, errBody, qosMap));
        } else {
            try {
                if (!receivedAck) {
                    responseHolder.getListener().progressAckErrorReceived(
                            hdr, new DummyErrorBody(err), qosMap);
                } else {
                    responseHolder.getListener().progressResponseErrorReceived(
                            hdr, new DummyErrorBody(err), qosMap);
                }
            } catch (MALException ex) {
                // not a lot we can do with this at this stage apart from log it
                MALContextFactoryImpl.LOGGER.log(Level.WARNING,
                        "Error received from consumer error handler in response to a provider error! {0}", ex);
            }
        }
    }

    @Override
    public synchronized boolean finished() {
        return receivedResponse;
    }
}
