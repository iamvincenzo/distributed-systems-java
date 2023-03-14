package jms.mutualexclusion.assignment3;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * Class providing an implementation of a sender.
 *
**/

public class Sender
{
  private static final String BROKER_URL = "tcp://localhost:61616";
  private static final String QUEUE_NAME = "queue";

  /**
   * Sends a sequence of messages.
   *
   * @param n  the number of messages.
   *
  **/
  public void send(final int n)
  {
    ActiveMQConnection connection = null;

    try
    {
      ActiveMQConnectionFactory cf =
        new ActiveMQConnectionFactory(Sender.BROKER_URL);

      connection = (ActiveMQConnection) cf.createConnection();

      connection.start();

      QueueSession session =
        connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

      Queue queue         = session.createQueue(Sender.QUEUE_NAME);
      QueueSender sender  = session.createSender(queue);
      TextMessage message = session.createTextMessage();

      for (int i = 0; i < n; i++)
      {
        message.setText("This is message " + (i + 1));
        sender.send(message);
      }

      sender.send(session.createMessage());

    }
    catch (JMSException e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (connection != null)
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
  }

  /**
   * Starts the sender.
   *
   * @param args
   *
   * It does not need arguments.
   *
  **/
  public static void main(final String[] args)
  {
    final int n = 3;

    new Sender().send(n);
  }
}
