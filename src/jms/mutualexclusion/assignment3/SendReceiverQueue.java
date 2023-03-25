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
 * 
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

    /*************************** USED ONLY FOR THE FIRST INTERACTION ***************************/
    
    public SendReceiverQueue(QueueSession session) throws JMSException
    {
        // Communication settings: the client sends messages to server
        this.session = session;
        this.serverQueue = session.createQueue(GenericClient.BROKER_QUEUE_NAME);
        this.producer = session.createProducer(this.serverQueue);

        // Communication settings: the client defines an endpoint used by the server to reply
        this.tempDest = session.createTemporaryQueue();
        this.consumer = session.createConsumer(this.tempDest);
    }

    /**
     * This method is used by the client to sent the ID request to the client-broker.
     * 
     * @param text
     * @param type
     * @param corrId
     * @throws JMSException
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
     * @param text
     * @param msg - It is the message that the client received.
     * @throws JMSException
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
     * 
     * @return
     */
    public MessageProducer getProducer()
    {
        return this.producer;
    }

    /**
     * 
     * @return
     */
    public MessageConsumer getConsumer()
    {
        return this.consumer;
    }

    /**
     * 
     * @return
     */
    public Destination getTempDest()
    {
        return this.tempDest;
    }

    /*******************************************************************************************/

    /**
     * This method is used to create the peer communication queue.
     * Once client has the ID can communicate with other peers.
     * 
     * @param id
     * @throws JMSException
     */
    public void createQueue(String id) throws JMSException
    {
		this.queue = this.session.createQueue(id);
		this.receiver = this.session.createReceiver(this.queue);
    }

    /**
     * 
     * @return
     */
    public QueueReceiver getQueueReceiver()
    {
        return this.receiver;
    }   

    /**
     * 
     * @param text The text of the message
     * @param type The type of the message
     * @param senderId The id of the sender
     * @param receiverId The id of the receiver
     * @throws JMSException
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

    /*public void sendGenericRequest(String text, String type, String senderId, String receiverId) throws JMSException
    {
        Destination peerQueue = session.createQueue(receiverId);
		MessageProducer producer = session.createProducer(peerQueue);

        TextMessage request = session.createTextMessage();
        request.setText(text);
        request.setJMSType(type);
        request.setJMSCorrelationID(senderId);
        producer.send(request);
    }

    public void sendGenericResponse(String text, String type, String senderId, String receiverId) throws JMSException
    {
        Destination peerQueue = session.createQueue(receiverId);
		MessageProducer producer = session.createProducer(peerQueue);

        TextMessage response = session.createTextMessage();
        response.setText(text);
        response.setJMSType(type);
        response.setJMSCorrelationID(senderId);
        producer.send(response);
    }

    public QueueSession getSession()
    {
        return this.session;
    }

    public Destination getServerQueue()
    {
        return this.serverQueue;
    }

    public Queue getQueue()
    {
        return this.queue;
    }*/