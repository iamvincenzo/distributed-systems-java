package jms.mutualexclusion.assignment3;

import javax.jms.Queue;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.QueueReceiver;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;

/**
 * This class provides the main methods required to handle interaction
 * between nodes in the system.
 * 
 * @author Vincenzo Fraello (339641)
 * @author Giorgia Tedaldi (339642)
 */

public class SendReceiverQueue 
{
    private QueueSession session;
    private Destination serverQueue;
    private Destination tempDest;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private Queue queue; 
    private QueueReceiver receiver;
    private Queue resourceQueue; 
    private QueueReceiver resourceReceiver;

    /*************************** USED ONLY FOR THE FIRST INTERACTION ***************************/
    
    /**
     * 
     * Method to set session and queues.
     * 
     * @param session QueueSession
     * @throws JMSException error in communication settings
     */
    public SendReceiverQueue(QueueSession session) throws JMSException
    {
        /* Communication settings: creation of the queue 
         * used by the client to send messages to the broker to request the ID. */
        this.session = session;
        this.serverQueue = session.createQueue(GenericClient.BROKER_QUEUE_NAME);
        this.producer = session.createProducer(this.serverQueue);

        /* Communication settings: the client defines an 
         * endpoint used by the server to reply. */
        this.tempDest = session.createTemporaryQueue();
        this.consumer = session.createConsumer(this.tempDest);
    }

    /**
     * This method is used by the client to send
     * the ID request to the client-broker.
     * 
     * @param text It is the content-text of the message.
     * @param type It is the type of request.
     * 
     * @throws JMSException It is an exception used to handle exceptions that occur during JMS operations.
     */
    public void sendIdRequest(String text, String type) throws JMSException
    {
        TextMessage msg = session.createTextMessage();
        msg.setText(text);
        msg.setJMSReplyTo(this.getTempDest());
        msg.setJMSType(type);
        
        this.getProducer().send(msg);
    }

    /**
     * This method is used by the broker-client to send the ID to the client that requested it.
     * 
     * @param msg It is the message-request 
     * 			sent by the requester to get its endpoint.
     * @param text It is the content-text of the message.
     * @param type It is the type of request.
     * 
     * @throws JMSException It is an exception used to handle exceptions that occur during JMS operations.
     */
    public void sendIdResponse(Message msg, String text, String type) throws JMSException
    {
        TextMessage response = session.createTextMessage();
        MessageProducer producerTemp = session.createProducer(null);
        response.setText(text);
        response.setJMSType(type);
        response.setJMSCorrelationID(msg.getJMSCorrelationID());
        
        producerTemp.send(msg.getJMSReplyTo(), response);
    }

    /**
     * This method is used to get the MessageProducer.
     * 
     * @return It returns the MessageProducer.
     */
    public MessageProducer getProducer()
    {
        return this.producer;
    }

    /**
     * This method is used to get the MessageConsumer.
     * 
     * @return It returns the MessageConsumer.
     */
    public MessageConsumer getConsumer()
    {
        return this.consumer;
    }

    /**
     * This method is used to get the Destination.
     * 
     * @return It returns the Destination.
     */
    public Destination getTempDest()
    {
        return this.tempDest;
    }

    
    /*************************** USED ONLY FOR OTHER INTERACTIONS ***************************/
    
	/**
	 * This method is used to create the peer communication queue.
	 * Once client has the ID can communicate with other peers.
	 * 
	 * @param id It is the id of the queue.
	 * @param type to distinguish broker from other clients.
	 * @throws JMSException It is an exception used to handle 
	 * 			exceptions that occur during JMS operations.
	 */
    public void createQueue(String id, int type) throws JMSException
    {
        if (type == 0)
        {
            this.queue = this.session.createQueue(id);
            this.receiver = this.session.createReceiver(this.queue);
        }
        else
        {
            this.resourceQueue = this.session.createQueue(id);
            this.resourceReceiver = this.session.createReceiver(this.resourceQueue);
        }
    }

    /**
     * This method is used to get the QueueReceiver.
     * 
     * @return It returns the QueueReceiver.
     */
    public QueueReceiver getQueueReceiver()
    {
        return this.receiver;
    }   

    /**
     * This method is used to get the QueueReceiver.
     * 
     * @return It returns the QueueReceiver.
     */
    protected QueueReceiver getResourceReceiver()
    {
        return this.resourceReceiver;
    }

    /**
     * This method is used by the node to send any type
     * of message.
     * 
     * @param text The text of the message.
     * @param type The type of the message.
     * @param senderId The id of the sender.
     * @param receiverId The id of the receiver.
     * 
     * @throws JMSException It is an exception used to handle 
	 * 			exceptions that occur during JMS operations.
     */
    public void sendMessage(String text, String type, String senderId, String receiverId) throws JMSException
    {
        Destination peerQueue = session.createQueue(receiverId);
		MessageProducer producer = session.createProducer(peerQueue);

        TextMessage request = session.createTextMessage();
        request.setText(text);
        request.setJMSType(type);
        request.setJMSCorrelationID(senderId);
        
        producer.send(request);
    }
}
