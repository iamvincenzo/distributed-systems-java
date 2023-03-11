package socket.clientserver.assignment2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * The class {@code Server} defines a server that waits
 * for a message and then sends an answer.
 *
**/
public class Server
{
  private static final int COREPOOL = 5;
  private static final int MAXPOOL = 100;
  private static final long IDLETIME = 5000;

  private static final int SPORT = 4445;

  private ServerSocket socket;
  private ThreadPoolExecutor pool;
  
  private ArrayList<Socket> clientSockets = new ArrayList<>();

  /**
   * 
   * Class constructor.
   *
   * @throws IOException if the creation of the server socket fails.
   *
  **/
  public Server() throws IOException
  {
    this.socket = new ServerSocket(SPORT);
  }

  /**
   * 
   * Runs the server code.
   * The server waits for the connection of at least 3 clients and then start the application.
   *
  **/
  private void run()
  {
    this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME,
        TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    
    System.out.println("Server is listening on PORT " + SPORT);

    while (clientSockets.size() < 3)
    {
      try
      {
        Socket s = this.socket.accept();
        clientSockets.add(s);
      }
      catch (Exception e)
      {
        break;
      }
    }
    
    for(Socket sk: clientSockets)
    {
    	this.pool.execute(new ServerThread(this, sk));
    }
    
    while (true)
    {
      try
      {
        Socket s = this.socket.accept();
        this.pool.execute(new ServerThread(this, s));
      }
      catch (Exception e)
      {
        break;
      }
    }

    this.pool.shutdown();
  }

  /**
   * 
   * Gets the server pool.
   *
   * @return the thread pool.
   *
  **/
  public ThreadPoolExecutor getPool()
  {
    return this.pool;
  }

  /**
   * 
   * Closes the server execution.
   *
  **/
  public void close()
  {
    try
    {
      this.socket.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * 
   * Starts the demo.
   *
   * @param args  the method does not requires arguments.
   *
   * @throws IOException if the execution fails.
   *
  **/
  public static void main(final String[] args) throws IOException
  {
    new Server().run();
  }
}