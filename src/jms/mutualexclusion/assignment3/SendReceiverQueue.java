package jms.mutualexclusion.assignment3;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

public class SendReceiverQueue 
{
    private QueueSession session;
    private Destination serverQueue;
    private Destination tempDest;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private Queue queue; 
    private QueueReceiver receiver;
    
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

    public void createQueue(String id) throws JMSException
    {
        // once client has the ID can communicate with other peers
		this.queue = this.session.createQueue(id);
		this.receiver = this.session.createReceiver(this.queue);
    }

    public void sendRequest(MessageProducer prod, String text, String type, String corrId) throws JMSException
    {
        TextMessage msg = session.createTextMessage();
        msg.setText(text);
        msg.setJMSReplyTo(this.getTempDest());
        msg.setJMSType(type);
        msg.setJMSCorrelationID(corrId);
        prod.send(msg);
    }

    /**
     * 
     * @param text
     * @param msg - It is the message that the client received.
     * @throws JMSException
     */
    public void sendResponse(Message msg, String text, String type) throws JMSException
    {
        TextMessage response = session.createTextMessage();
        MessageProducer producerTemp = session.createProducer(null);
        response.setText(text);
        response.setJMSType(type);
        response.setJMSCorrelationID(msg.getJMSCorrelationID());
        producerTemp.send(msg.getJMSReplyTo(), response);
    }

    

    public Destination getServerQueue()
    {
        return this.serverQueue;
    }

    public Destination getTempDest()
    {
        return this.tempDest;
    }

    public MessageProducer getProducer()
    {
        return this.producer;
    }

    public MessageConsumer getConsumer()
    {
        return this.consumer;
    }

    public Queue getQueue()
    {
        return this.queue;
    } 

    public QueueReceiver getQueueReceiver()
    {
        return this.receiver;
    }   
    
    
    public QueueSession getSession()
    {
        return this.session;
    }
}
