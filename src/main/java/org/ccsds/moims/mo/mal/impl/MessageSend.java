/* ----------------------------------------------------------------------------
 * (C) 2010      European Space Agency
 *               European Space Operations Centre
 *               Darmstadt Germany
 * ----------------------------------------------------------------------------
 * System       : CCSDS MO MAL Implementation
 * Author       : cooper_sf
 *
 * ----------------------------------------------------------------------------
 */
package org.ccsds.moims.mo.mal.impl;

import java.util.Hashtable;
import org.ccsds.moims.mo.mal.MALInvokeOperation;
import org.ccsds.moims.mo.mal.MALOperation;
import org.ccsds.moims.mo.mal.MALProgressOperation;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.MALRequestOperation;
import org.ccsds.moims.mo.mal.MALSubmitOperation;
import org.ccsds.moims.mo.mal.consumer.MALInteractionListener;
import org.ccsds.moims.mo.mal.structures.Element;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.InteractionType;
import org.ccsds.moims.mo.mal.structures.MessageHeader;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.Time;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.transport.MALEndPoint;
import org.ccsds.moims.mo.mal.transport.MALMessage;
import org.ccsds.moims.mo.mal.impl.util.Logging;
import org.ccsds.moims.mo.mal.provider.MALPublishInteractionListener;
import org.ccsds.moims.mo.mal.accesscontrol.MALAccessControl;
import org.ccsds.moims.mo.mal.structures.Blob;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.StandardError;
import org.ccsds.moims.mo.mal.structures.Union;

/**
 * This class is the central point for sending messages out.
 */
public class MessageSend
{
  private final MALAccessControl securityManager;
  private final InteractionMap imap;
  private final PubSubMap pmap;

  MessageSend(MALAccessControl securityManager, InteractionMap imap, PubSubMap pmap)
  {
    this.securityManager = securityManager;
    this.imap = imap;
    this.pmap = pmap;
  }

  /**
   * Synchronous register send.
   * @param details Message details structure.
   * @param op The operation.
   * @param subscription Consumer subscription.
   * @param listener Update callback interface.
   * @throws MALException on error.
   */
  public void register(MessageDetails details,
          MALPubSubOperation op,
          Subscription subscription,
          MALInteractionListener listener) throws MALException
  {
    pmap.registerNotifyListener(details, op, subscription, listener);
    synchronousInteraction(details,
            op,
            MALPubSubOperation.REGISTER_STAGE,
            (MALPublishInteractionListener) null,
            subscription);
  }

  /**
   * Synchronous publish register send.
   * @param details Message details structure.
   * @param op The operation.
   * @param entityKeys List of keys that can be published.
   * @param listener Error callback interface.
   * @return Publish transaction identifier.
   * @throws MALException on error.
   */
  public Identifier publishRegister(MessageDetails details,
          MALPubSubOperation op,
          EntityKeyList entityKeys,
          MALPublishInteractionListener listener) throws MALException
  {
    pmap.registerPublishListener(details, listener);
    return (Identifier) synchronousInteraction(details,
            op,
            MALPubSubOperation.PUBLISH_REGISTER_STAGE,
            (MALPublishInteractionListener) null,
            entityKeys);
  }

  /**
   * Synchronous publish deregister send.
   * @param details Message details structure.
   * @param op The operation.
   * @throws MALException on error.
   */
  public void publishDeregister(MessageDetails details, MALPubSubOperation op) throws MALException
  {
    synchronousInteraction(details,
            op,
            MALPubSubOperation.PUBLISH_DEREGISTER_STAGE,
            (MALPublishInteractionListener) null,
            null);
    pmap.getPublishListenerAndRemove(details.endpoint.getURI(), details.sessionName);
  }

  /**
   * Synchronous deregister send.
   * @param details Message details structure.
   * @param op The operation.
   * @param unsubscription consumer unsubscription.
   * @throws MALException on error.
   */
  public void deregister(MessageDetails details,
          MALPubSubOperation op,
          IdentifierList unsubscription) throws MALException
  {
    synchronousInteraction(details,
            op,
            MALPubSubOperation.DEREGISTER_STAGE,
            (MALPublishInteractionListener) null,
            unsubscription);
    pmap.deregisterNotifyListener(details, op, unsubscription);
  }

  /**
   * Asynchronous publish register send.
   * @param details Message details structure.
   * @param op The operation.
   * @param entityKeys List of keys that can be published.
   * @param listener Response callback interface.
   * @return Publish transaction identifier.
   * @throws MALException on error.
   */
  public Identifier publishRegisterAsync(MessageDetails details,
          MALPubSubOperation op,
          EntityKeyList entityKeys,
          MALPublishInteractionListener listener) throws MALException
  {
    pmap.registerPublishListener(details, listener);
    return (Identifier) asynchronousInteraction(details,
            op,
            MALPubSubOperation.PUBLISH_REGISTER_STAGE,
            listener,
            entityKeys);
  }

  /**
   * Asynchronous register send.
   * @param details Message details structure.
   * @param op The operation.
   * @param subscription Consumer subscription.
   * @param listener Response callback interface.
   * @throws MALException on error.
   */
  public void registerAsync(MessageDetails details,
          MALPubSubOperation op,
          Subscription subscription,
          MALInteractionListener listener) throws MALException
  {
    pmap.registerNotifyListener(details, op, subscription, listener);
    asynchronousInteraction(details, op, MALPubSubOperation.REGISTER_STAGE, listener, subscription);
  }

  /**
   * Asynchronous publish deregister send.
   * @param details Message details structure.
   * @param op The operation.
   * @param listener Response callback interface.
   * @throws MALException on error.
   */
  public void publishDeregisterAsync(MessageDetails details,
          MALPubSubOperation op,
          MALPublishInteractionListener listener) throws MALException
  {
    pmap.getPublishListenerAndRemove(details.endpoint.getURI(), details.sessionName);
    asynchronousInteraction(details, op, MALPubSubOperation.PUBLISH_DEREGISTER_STAGE, listener, null);
  }

  /**
   *  Asynchronous deregister send.
   * @param details Message details structure.
   * @param op The operation.
   * @param unsubscription consumer unsubscription.
   * @param listener Response callback interface.
   * @throws MALException on error.
   */
  public void deregisterAsync(MessageDetails details,
          MALPubSubOperation op,
          IdentifierList unsubscription,
          MALInteractionListener listener) throws MALException
  {
    asynchronousInteraction(details, op, MALPubSubOperation.DEREGISTER_STAGE, listener, unsubscription);
    pmap.deregisterNotifyListener(details, op, unsubscription);
  }

  /**
   * Send return response method.
   * @param msgAddress Address structure to use for return message.
   * @param internalTransId Internal transaction identifer.
   * @param srcHdr Message header to use as reference for return messages header.
   * @param rspnInteractionStage Interaction stage to use on the response.
   * @param rspn Response message body.
   */
  public void returnResponse(Address msgAddress,
          Identifier internalTransId,
          MessageHeader srcHdr,
          Byte rspnInteractionStage,
          Element rspn)
  {
    try
    {
      MALEndPoint endpoint = msgAddress.endpoint;
      MALMessage msg = endpoint.createMessage(createReturnHeader(msgAddress.uri,
              msgAddress.authenticationId,
              srcHdr,
              srcHdr.getQoSlevel(),
              rspnInteractionStage,
              false), rspn, new Hashtable());

      endpoint.sendMessage(msg);
    }
    catch (MALException ex)
    {
      Logging.logMessage("ERROR: Error returning response to consumer : " + srcHdr.getURIfrom());
    }
  }

  void returnErrorAndCalculateStage(Address msgAddress,
          Identifier internalTransId,
          MessageHeader srcHdr,
          StandardError error)
  {
    Byte rspnInteractionStage = -1;
    final int srcInteractionStage = srcHdr.getInteractionStage().intValue();

    switch (srcHdr.getInteractionType().getOrdinal())
    {
      case InteractionType._SUBMIT_INDEX:
      {
        if (MALSubmitOperation._SUBMIT_STAGE == srcInteractionStage)
        {
          rspnInteractionStage = MALSubmitOperation.SUBMIT_ACK_STAGE;
        }
        break;
      }
      case InteractionType._REQUEST_INDEX:
      {
        if (MALRequestOperation._REQUEST_STAGE == srcInteractionStage)
        {
          rspnInteractionStage = MALRequestOperation.REQUEST_RESPONSE_STAGE;
        }
        break;
      }
      case InteractionType._INVOKE_INDEX:
      {
        if (MALInvokeOperation._INVOKE_STAGE == srcInteractionStage)
        {
          rspnInteractionStage = MALInvokeOperation.INVOKE_ACK_STAGE;
        }
        break;
      }
      case InteractionType._PROGRESS_INDEX:
      {
        if (MALProgressOperation._PROGRESS_STAGE == srcInteractionStage)
        {
          rspnInteractionStage = MALProgressOperation.PROGRESS_ACK_STAGE;
        }
        break;
      }
      case InteractionType._PUBSUB_INDEX:
      {
        switch (srcInteractionStage)
        {
          case MALPubSubOperation._REGISTER_STAGE:
          {
            rspnInteractionStage = MALPubSubOperation.REGISTER_ACK_STAGE;
            break;
          }
          case MALPubSubOperation._PUBLISH_REGISTER_STAGE:
          {
            rspnInteractionStage = MALPubSubOperation.PUBLISH_REGISTER_ACK_STAGE;
            break;
          }
          case MALPubSubOperation._PUBLISH_STAGE:
          {
            //rspnInteractionStage = MALPubSubOperation.PUBLISH_STAGE;
            throw new UnsupportedOperationException("Not supported yet.");
            //break;
          }
          case MALPubSubOperation._DEREGISTER_STAGE:
          {
            rspnInteractionStage = MALPubSubOperation.DEREGISTER_ACK_STAGE;
            break;
          }
          case MALPubSubOperation._PUBLISH_DEREGISTER_STAGE:
          {
            rspnInteractionStage = MALPubSubOperation.PUBLISH_DEREGISTER_ACK_STAGE;
            break;
          }
          default:
          {
            // no op
          }
        }
        break;
      }
      default:
      {
        // no op
      }
    }

    if (0 > rspnInteractionStage)
    {
      Logging.logMessage("ERROR: Unable to return error, already a return message (" + error + ")");
    }
    else
    {
      returnError(msgAddress, internalTransId, srcHdr, rspnInteractionStage, error);
    }
  }

  /**
   * Send return error method.
   * @param msgAddress Address structure to use for return message.
   * @param internalTransId Internal transaction identifer.
   * @param srcHdr Message header to use as reference for return messages header.
   * @param rspnInteractionStage Interaction stage to use on the response.
   * @param error Response message error.
   */
  public void returnError(Address msgAddress,
          Identifier internalTransId,
          MessageHeader srcHdr,
          Byte rspnInteractionStage,
          StandardError error)
  {
    returnError(msgAddress, internalTransId, srcHdr, srcHdr.getQoSlevel(), rspnInteractionStage, error);
  }

  void returnError(Address msgAddress,
          Identifier internalTransId,
          MessageHeader srcHdr,
          QoSLevel level,
          Byte rspnInteractionStage,
          StandardError error)
  {
    try
    {
      if (null == level)
      {
        level = srcHdr.getQoSlevel();
      }

      MALMessage msg = msgAddress.endpoint.createMessage(createReturnHeader(msgAddress.uri,
              msgAddress.authenticationId, srcHdr, level, rspnInteractionStage, true), error, new Hashtable());

      msgAddress.endpoint.sendMessage(msg);
    }
    catch (MALException ex)
    {
      Logging.logMessage("ERROR: Error returning exception to consumer : " + srcHdr.getURIfrom());
    }
  }

  /**
   * Performs a oneway interaction, sends the message and then returns.
   * @param details Message details structure.
   * @param transId The transaction identifier to use.
   * @param op The operation.
   * @param stage The interaction stage to use.
   * @param msgBody The message body.
   * @throws MALException on Error.
   */
  public void onewayInteraction(MessageDetails details,
          Identifier transId,
          MALOperation op,
          Byte stage,
          Element msgBody) throws MALException
  {
    MALMessage msg = details.endpoint.createMessage(createHeader(details,
            op, transId, stage), msgBody, details.qosProps);

    try
    {
      msg = securityManager.check(msg);

      details.endpoint.sendMessage(msg);
    }
    catch (MALException ex)
    {
      Logging.logMessage("ERROR: Error with one way send : " + msg.getHeader().getURIto());
      throw ex;
    }
  }

  /**
   * Performs a two way interaction, sends the message and then waits for the specified stage before returning.
   * @param details Message details structure.
   * @param op The operation.
   * @param syncStage The interaction stage to wait for before returning.
   * @param listener Interaction listener to use for the reception of other stages.
   * @param msgBody The message body.
   * @return The return value.
   * @throws MALException on Error.
   */
  public Element synchronousInteraction(MessageDetails details,
          MALOperation op,
          Byte syncStage,
          MALInteractionListener listener,
          Element msgBody) throws MALException
  {
    Identifier transId = imap.createTransaction(op, true, syncStage, listener);

    return synchronousInteraction(transId, details, op, syncStage, msgBody);
  }

  /**
   * Performs a two way publisher interaction,
   * sends the message and then waits for the specified stage before returning.
   * @param details Message details structure.
   * @param op The operation.
   * @param syncStage The interaction stage to wait for before returning.
   * @param listener Interaction listener to use for the reception of other stages.
   * @param msgBody The message body.
   * @return The return value.
   * @throws MALException on Error.
   */
  public Element synchronousInteraction(MessageDetails details,
          MALOperation op,
          Byte syncStage,
          MALPublishInteractionListener listener,
          Element msgBody) throws MALException
  {
    Identifier transId = imap.createTransaction(op, true, syncStage, listener);

    Element rv = synchronousInteraction(transId, details, op, syncStage, msgBody);

    if (MALPubSubOperation._PUBLISH_REGISTER_STAGE == syncStage)
    {
      return transId;
    }
    else
    {
      return rv;
    }
  }

  private Element synchronousInteraction(Identifier transId,
          MessageDetails details,
          MALOperation op,
          Byte syncStage,
          Element msgBody) throws MALException
  {
    MALMessage msg = details.endpoint.createMessage(createHeader(details, op, transId, syncStage), msgBody, details.qosProps);

    try
    {
      msg = securityManager.check(msg);

      details.endpoint.sendMessage(msg);

      MALMessage rtn = imap.waitForResponse(transId);

      handlePossibleReturnError(rtn);

      return rtn.getBody();
    }
    catch (MALException ex)
    {
      Logging.logMessage("ERROR: Error with consumer : " + msg.getHeader().getURIto());
      throw ex;
    }
  }

  /**
   * Performs a two way interaction, sends the message.
   * @param details Message details structure.
   * @param op The operation.
   * @param initialStage The initial interaction stage.
   * @param listener Interaction listener to use for the reception of other stages.
   * @param msgBody The message body.
   * @throws MALException on Error.
   */
  public void asynchronousInteraction(MessageDetails details,
          MALOperation op,
          Byte initialStage,
          MALInteractionListener listener,
          Element msgBody) throws MALException
  {
    Identifier transId = imap.createTransaction(op, false, initialStage, listener);

    asynchronousInteraction(transId, details, op, initialStage, msgBody);
  }

  /**
   * Performs a two way interaction, sends the message.
   * @param details Message details structure.
   * @param op The operation.
   * @param initialStage The initial interaction stage.
   * @param listener Interaction listener to use for the reception of other stages.
   * @param msgBody The message body.
   * @return The transaction identifier is this is a PUBLISH REGISTER message, else null.
   * @throws MALException on Error.
   */
  public Element asynchronousInteraction(MessageDetails details,
          MALOperation op,
          Byte initialStage,
          MALPublishInteractionListener listener,
          Element msgBody) throws MALException
  {
    Identifier transId = imap.createTransaction(op, false, initialStage, listener);

    asynchronousInteraction(transId, details, op, initialStage, msgBody);

    if (MALPubSubOperation._PUBLISH_REGISTER_STAGE == initialStage)
    {
      return transId;
    }
    else
    {
      return null;
    }
  }

  private void asynchronousInteraction(Identifier transId,
          MessageDetails details,
          MALOperation op,
          Byte initialStage,
          Element msgBody) throws MALException
  {
    MALMessage msg = details.endpoint.createMessage(createHeader(details, op, transId, initialStage),
            msgBody,
            details.qosProps);

    try
    {
      msg = securityManager.check(msg);

      details.endpoint.sendMessage(msg);
    }
    catch (MALException ex)
    {
      Logging.logMessage("ERROR: Error with consumer : " + msg.getHeader().getURIto());
      throw ex;
    }
  }

  private void handlePossibleReturnError(MALMessage rtn) throws MALException
  {
    if ((null != rtn) && (rtn.getHeader().isError()))
    {
      if (rtn.getBody() instanceof StandardError)
      {
        throw new MALException((StandardError) rtn.getBody());
      }

      throw new MALException(new StandardError(MALHelper.BAD_ENCODING_ERROR_NUMBER,
              new Union("Return message marked as error but did not contain a MALException")));
    }
  }

  /**
   * Creates a message header.
   * @param details Message details structure.
   * @param op The operation.
   * @param transactionId The transaction identifier to use.
   * @param interactionStage The interaction stage.
   * @return the new message header.
   */
  public static MessageHeader createHeader(MessageDetails details,
          MALOperation op,
          Identifier transactionId,
          Byte interactionStage)
  {
    MessageHeader hdr = new MessageHeader();

    if (null != details.uriFrom)
    {
      hdr.setURIfrom(details.uriFrom);
    }
    else
    {
      hdr.setURIfrom(details.endpoint.getURI());
    }

    if (op.getInteractionType() == InteractionType.PUBSUB)
    {
      hdr.setURIto(details.brokerUri);
    }
    else
    {
      hdr.setURIto(details.uriTo);
    }
    hdr.setAuthenticationId(details.authenticationId);
    hdr.setTimestamp(new Time(new java.util.Date().getTime()));
    hdr.setQoSlevel(details.qosLevel);
    hdr.setPriority(details.priority);
    hdr.setDomain(details.domain);
    hdr.setNetworkZone(details.networkZone);
    hdr.setSession(details.sessionType);
    hdr.setSessionName(details.sessionName);
    hdr.setInteractionType(op.getInteractionType());
    hdr.setInteractionStage(interactionStage);
    hdr.setTransactionId(transactionId);
    hdr.setArea(op.getService().getArea().getName());
    hdr.setService(op.getService().getName());
    hdr.setOperation(op.getName());
    hdr.setVersion(op.getService().getVersion());
    hdr.setError(Boolean.FALSE);

    return hdr;
  }

  MessageHeader createReturnHeader(URI uriFrom,
          Blob authId,
          MessageHeader srcHdr,
          QoSLevel level,
          Byte interactionStage,
          boolean isError)
  {
    MessageHeader hdr = new MessageHeader();

    hdr.setURIfrom(uriFrom);
    hdr.setURIto(srcHdr.getURIfrom());
    hdr.setAuthenticationId(authId);
    hdr.setTimestamp(new Time(new java.util.Date().getTime()));
    hdr.setQoSlevel(level);
    hdr.setPriority(srcHdr.getPriority());
    hdr.setDomain(srcHdr.getDomain());
    hdr.setNetworkZone(srcHdr.getNetworkZone());
    hdr.setSession(srcHdr.getSession());
    hdr.setSessionName(srcHdr.getSessionName());
    hdr.setInteractionType(srcHdr.getInteractionType());
    hdr.setInteractionStage(interactionStage);
    hdr.setTransactionId(srcHdr.getTransactionId());
    hdr.setArea(srcHdr.getArea());
    hdr.setService(srcHdr.getService());
    hdr.setOperation(srcHdr.getOperation());
    hdr.setVersion(srcHdr.getVersion());
    hdr.setError(Boolean.valueOf(isError));

    return hdr;
  }
}
