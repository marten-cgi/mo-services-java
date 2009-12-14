/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ccsds.moims.mo.mal.impl;

import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALPubSubOperation;
import org.ccsds.moims.mo.mal.impl.profile.MALProfiler;
import org.ccsds.moims.mo.mal.provider.MALPublishInteractionListener;
import org.ccsds.moims.mo.mal.provider.MALPublisher;
import org.ccsds.moims.mo.mal.provider.MALSubscriberEventListener;
import org.ccsds.moims.mo.mal.structures.DomainIdentifier;
import org.ccsds.moims.mo.mal.structures.EntityKey;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.SessionType;
import org.ccsds.moims.mo.mal.structures.StandardError;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UpdateList;


/**
 *
 * @author cooper_sf
 */
public class MALBrokerPublisher implements MALPublisher
{
  private final MALProviderImpl parent;
  private final MALServiceSend handler;
  private final MALPubSubOperation operation;
  private Map<BrokerKey, Identifier> transId = new TreeMap<BrokerKey, Identifier>();

  public MALBrokerPublisher(MALProviderImpl parent, MALServiceSend handler, MALPubSubOperation operation)
  {
    this.parent = parent;
    this.handler = handler;
    this.operation = operation;
  }

  @Override
  public void register(EntityKeyList entityKeys, MALPublishInteractionListener listener, DomainIdentifier domain, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel remotePublisherQos, Hashtable remotePublisherQosProps, Integer remotePublisherPriority) throws MALException
  {
    MALProfiler.instance.sendMarkMALReception(domain);

    MALMessageDetails details = new MALMessageDetails(parent.getPublishEndpoint(), parent.getURI(), null, parent.getBrokerURI(), operation.getService(), parent.authenticationId, domain, networkZone, sessionType, sessionName, remotePublisherQos, remotePublisherQosProps, remotePublisherPriority);

    MALProfiler.instance.sendMALTransferObject(domain, details);

    setTransId(parent.getPublishEndpoint().getURI(), domain, networkZone.getValue(), sessionType, sessionName.getValue(), handler.publishRegister(details, operation, entityKeys, listener));
  }

  @Override
  public void asyncRegister(EntityKeyList entityKeys, MALPublishInteractionListener listener, DomainIdentifier domain, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel remotePublisherQos, Hashtable remotePublisherQosProps, Integer remotePublisherPriority) throws MALException
  {
    MALProfiler.instance.sendMarkMALReception(domain);

    MALMessageDetails details = new MALMessageDetails(parent.getPublishEndpoint(), parent.getURI(), null, parent.getBrokerURI(), operation.getService(), parent.authenticationId, domain, networkZone, sessionType, sessionName, remotePublisherQos, remotePublisherQosProps, remotePublisherPriority);

    MALProfiler.instance.sendMALTransferObject(domain, details);

    setTransId(parent.getPublishEndpoint().getURI(), domain, networkZone.getValue(), sessionType, sessionName.getValue(), handler.publishRegisterAsync(details, operation, entityKeys, listener));
  }

  @Override
  public void publish(UpdateList updateList, DomainIdentifier domain, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel publishQos, Hashtable publishQosProps, Integer publishPriority) throws MALException
  {
    MALProfiler.instance.sendMarkMALReception(domain);

    MALMessageDetails details = new MALMessageDetails(parent.getPublishEndpoint(), parent.getURI(), null, parent.getBrokerURI(), operation.getService(), parent.authenticationId, domain, networkZone, sessionType, sessionName, publishQos, publishQosProps, publishPriority);

    MALProfiler.instance.sendMALTransferObject(domain, details);

    System.out.println("INFO: Publisher using transaction Id of: " + transId);
      
    handler.publish(details, getTransId(parent.getPublishEndpoint().getURI(), domain, networkZone.getValue(), sessionType, sessionName.getValue()), operation, updateList);
  }

  @Override
  public void publish(StandardError error, DomainIdentifier domain, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel remotePublisherQos, Hashtable remotePublisherQosProps, Integer remotePublisherPriority) throws MALException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void addPublishedEntityKeys(EntityKey[] keys) throws MALException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void removePublishedEntityKeys(EntityKey[] keys) throws MALException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void deregister(DomainIdentifier domain, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel remotePublisherQos, Hashtable remotePublisherQosProps, Integer remotePublisherPriority) throws MALException
  {
    MALProfiler.instance.sendMarkMALReception(domain);

    MALMessageDetails details = new MALMessageDetails(parent.getPublishEndpoint(), parent.getURI(), null, parent.getBrokerURI(), operation.getService(), parent.authenticationId, domain, networkZone, sessionType, sessionName, remotePublisherQos, remotePublisherQosProps, remotePublisherPriority);

    MALProfiler.instance.sendMALTransferObject(domain, details);

    handler.publishDeregister(details, operation);

    clearTransId(parent.getPublishEndpoint().getURI(), domain, networkZone.getValue(), sessionType, sessionName.getValue());
  }

  @Override
  public void asyncDeregister(MALPublishInteractionListener listener, DomainIdentifier domain, Identifier networkZone, SessionType sessionType, Identifier sessionName, QoSLevel remotePublisherQos, Hashtable remotePublisherQosProps, Integer remotePublisherPriority) throws MALException
  {
    MALProfiler.instance.sendMarkMALReception(domain);

    MALMessageDetails details = new MALMessageDetails(parent.getPublishEndpoint(), parent.getURI(), null, parent.getBrokerURI(), operation.getService(), parent.authenticationId, domain, networkZone, sessionType, sessionName, remotePublisherQos, remotePublisherQosProps, remotePublisherPriority);

    MALProfiler.instance.sendMALTransferObject(domain, details);

    handler.publishDeregisterAsync(details, operation, listener);

    clearTransId(parent.getPublishEndpoint().getURI(), domain, networkZone.getValue(), sessionType, sessionName.getValue());
  }

  @Override
  public boolean isListened(EntityKey entityKey) throws MALException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void setSubscriberEventListener(MALSubscriberEventListener listener) throws MALException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  private synchronized void setTransId(URI brokerUri, DomainIdentifier domain, String networkZone, SessionType session, String sessionName, Identifier id)
  {
    BrokerKey key = new BrokerKey(brokerUri, domain, networkZone, session, sessionName);

    if (!transId.containsKey(key))
    {
      System.out.println("INFO: Publisher setting transaction Id to: " + id);
      System.out.flush();
      transId.put(key, id);
    }
  }

  private synchronized void clearTransId(URI brokerUri, DomainIdentifier domain, String networkZone, SessionType session, String sessionName)
  {
    BrokerKey key = new BrokerKey(brokerUri, domain, networkZone, session, sessionName);

    Identifier id = transId.get(key);
    if (null != id)
    {
      System.out.println("INFO: Publisher removing transaction Id of: " + id);
      System.out.flush();
      transId.remove(key);
    }
  }

  private synchronized Identifier getTransId(URI brokerUri, DomainIdentifier domain, String networkZone, SessionType session, String sessionName)
  {
    return transId.get(new BrokerKey(brokerUri, domain, networkZone, session, sessionName));
  }
}
