package jms.mutualexclusion.assignment3;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 *
 * Class providing an implementation of a receiver
 * that uses a queue listener.
 *
**/

public class QueueListener implements MessageListener
{
  private static final String BROKER_URL   = "tcp://localhost:61616";
  private static final String BROKER_PROPS = "persistent=false&useJmx=false";
  private static final String QUEUE_NAME   = "queue";

  private ActiveMQConnection connection = null;

  /**
   * Class constructor.
   *
  **/
  public QueueListener()
  {
    try
    {
      BrokerService broker = BrokerFactory.createBroker(
          "broker:(" + BROKER_URL + ")?" + BROKER_PROPS);

      broker.start();

      ActiveMQConnectionFactory cf =
        new ActiveMQConnectionFactory(QueueListener.BROKER_URL);

      connection = (ActiveMQConnection) cf.createConnection();

      connection.start();

      QueueSession session =
        connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

      Queue queue = session.createQueue(QueueListener.QUEUE_NAME);

      MessageConsumer consumer = session.createConsumer(queue);

      consumer.setMessageListener(this);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} **/
  @Override
  public void onMessage(final Message m)
  {
    if (m instanceof TextMessage)
    {
      try
      {
        System.out.println("Message: " + ((TextMessage) m).getText());
      }
      catch (JMSException e)
      {
        e.printStackTrace();
      }
    }
    else if (connection != null)
    {
      try
      {
        connection.close();
      }
      catch (JMSException e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * Starts the receiver.
   *
   * @param args
   *
   * It does not need arguments.
   *
  **/
  public static void main(final String[] args)
  {
    new QueueListener();
  }
}
