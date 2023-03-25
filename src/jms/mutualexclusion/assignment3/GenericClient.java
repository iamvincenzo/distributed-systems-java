package jms.mutualexclusion.assignment3;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.util.comparator.BooleanComparator;

import javax.jms.Session;
import javax.jms.QueueSession;

import java.util.Random;

import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.MessageConsumer;
import jms.mutualexclusion.assignment3.GenericClient;


/**
 * 
 */
public class GenericClient
{
	/**
	 * 
	 */
	protected enum State 
	{
		IDLE,
		CANDIDATE,
		DEAD,
		COORDINATOR,
		REQUESTER,
		WAITER
	}

	protected State myState;

	/**
	 * H is the probability to stay active 
	 * K is the probability to crash
	 */
	protected static final float H = 0.02f;
	protected static final float K = 0.001f;

	/**
	 * MIN & MAX defines an interval used to sample
	 * random numbers
	 */
	protected static final int MIN = 0;
	protected static final int MAX = 100;

	protected static final int RES_REQ_TH = 70;

	/**
	 * 
	 */
	protected static final String ID_REQUEST = "ID_REQUEST";
	protected static final String ID_RESPONSE = "ID_RESPONDE";
	protected static final String RESOURCE_REQUEST = "RESOURCE_REQUEST";
	protected static final String ELECTION_REQ = "ELECTION_REQ";
	protected static final String ELECTION_ACK = "ELECTION_ACK"; 
	protected static final String COORDINATOR = "COORDINATOR";
	protected static final String COORD_PING = "COORD_PING";
	protected static final String COORD_ALIVE = "COORD_ALIVE";
	protected static final String RESOURCE_REQ = "RESOURCE_REQ";
	protected static final String RESOURCE_RESP = "RESOURCE_RESP";

	/**
	 * client-broker parameters
	 * client-broker-coordinator queue: used by clients to get id
	 */
	protected static final String BROKER_URL   = "tcp://localhost:61616";
	protected static final String BROKER_PROPS = "persistent=false&useJmx=false";
	public static final String BROKER_QUEUE_NAME   = "0";

	public String COORD_ID = "-1";

	/**
	 * 
	 */
	protected static final int ITER_TH_TOLERANCE = 2;

	/**
	 * client id used to identify the process and the queue. INCR_ID is used by the 
	 * coordinator to assign an incremental id to other clients
	 */
	protected static int INCR_ID = 1;

	// this is the client id
	protected int CLIENT_ID = -1;

	// number of client firstly connected
	protected static final int N_CONNECTED = 3;

	/**
	 * 
	 */
	protected SendReceiverQueue myQueue;

	/**
	 * 
	 */
	private QueueSession session;
	private ActiveMQConnection connection;


	/**
	 * 
	 */
	private boolean busy = false;


	/**
	 * 
	 * @return
	 */
	protected boolean checkChangeState()
	{
		Random random = new Random();

		/* Random number used check if "stay active" */
		int n1 = random.nextInt(MAX - MIN) + MIN;

		/* Random number used check if "crash" */
		int n2 = random.nextInt(MAX - MIN) + MIN;

		// System.out.println("N1: " + n1 + ", N2: " + n2 + " n1*H: " 
		// 	+ n1 * GenericClient.H  + ", n2*K: " + n2 * GenericClient.K);

		return (n1 * GenericClient.H > n2 * GenericClient.K);
	} 


	/**
	 * 
	 * @return
	 */
	protected boolean checkResourceRequest()
	{
		Random random = new Random();

		/* Random number used check if "resource request" */
		int n = random.nextInt(MAX - MIN) + MIN;

		return n > GenericClient.RES_REQ_TH;
	}



	/**
	 * @throws JMSException
	 * 
	 */
	protected void resourceRequest() throws JMSException
	{
		System.out.println("\n(C-" + this.getCLIENT_ID() + 
			") I'm requesting the resource to the coordinator...");

		this.myQueue.sendMessage("Resource requets...", GenericClient.RESOURCE_REQ, 
			this.getCLIENT_ID(), this.COORD_ID);
	}


	/**
	 * 
	 */
	protected void assignResource()
	{
		// to do
	}


	/**
	 * 
	 */
	protected void checkIfAlive()
	{
		// to do
	}

	/**
	 * 
	 * @throws JMSException
	 * @throws InterruptedException
	 */
	protected void clientOperations() throws JMSException, InterruptedException
	{
		int ackToReceive = 0;
		int iterTolerance = 0;
		boolean elecSent = false;
		boolean times = true;

		while(true)
		{
			/* all clients except broker-client only once */
			if(!this.getCLIENT_ID().equals("0") && times)
			{
				this.myQueue = new SendReceiverQueue(this.session);

				/**
				 * INITIALIZATION PHASE: firstly the client gets the ID   
				 */			
				MessageConsumer consumer = this.myQueue.getConsumer(); 
				while(true)
				{
					this.myQueue.sendIdRequest("Client ID request message.", GenericClient.ID_REQUEST);
					Message reply = consumer.receive(); // the client waits for request	

					if(reply.getJMSType().compareTo(GenericClient.ID_RESPONSE) == 0)
					{
						this.setCLIENT_ID(Integer.parseInt(((TextMessage) reply).getText()));
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message ID-response: " 
							+ ((TextMessage) reply).getText());
						break;
					}
				}

				this.myQueue.createQueue(this.getCLIENT_ID());
			}


			/* The peer checks if he has to change its state */
			if(this.checkChangeState() || (this.getCOORD_ID().equals("-1") 
				&& this.getMyState() == GenericClient.State.CANDIDATE))
			{
				System.out.println("\nI'm ALIVE...");

				// ????
				if (this.getMyState() == GenericClient.State.DEAD)
				{
					this.setMyState(GenericClient.State.IDLE);
				}
			}
			else
			{
				System.out.println("\nI'm DEAD...");
				this.setMyState(GenericClient.State.DEAD);
			}


			// gestione se sei candidato indici l'elezione
			if(this.getMyState() == GenericClient.State.CANDIDATE)
			{
				times = false;
				ackToReceive = this.convocateElection();
				elecSent = true;

				this.setMyState(GenericClient.State.IDLE);
			}
			else if(this.getMyState() == GenericClient.State.IDLE)
			{
				times = false;

				// ping the coordinator and the coordinator exists
				if(!this.getCOORD_ID().equals("-1"))
				{
					System.out.println("Client-" + this.getCLIENT_ID() 
						+ " Check if coordinator is alive (COORD_PING)...");

					this.myQueue.sendMessage("Client-" + this.getCLIENT_ID() 
							+ " Check if coordinator is alive (COORD_PING)...", 
							GenericClient.COORD_PING, this.getCLIENT_ID(), this.getCOORD_ID());
				}

				while(true)
				{
					Message msg = this.myQueue.getQueueReceiver().receive(3000);	
					
					if(msg == null)
					{
						iterTolerance++;

						/* the client is the new coordinator (case: the highest processes are down) */
						if(elecSent && ackToReceive > 0 && iterTolerance >= GenericClient.ITER_TH_TOLERANCE)
						{
							System.out.println("\nI'm C-" + this.getCLIENT_ID() + " the new coordinator...");
							ackToReceive = 0; // not sure ???
							iterTolerance = 0; // not sure ???
							elecSent = false; // not sure ???
							
							this.sendCoordinatorMsg();
							break;
						}
					}				
					else if(msg.getJMSType().compareTo(GenericClient.COORDINATOR) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") There's a new coordinator with ID: " 
							+ msg.getJMSCorrelationID());
							
						this.setCOORD_ID(msg.getJMSCorrelationID());
						break; // there is a new coordinator so: restart the loop
					}
					else if(msg.getJMSType().compareTo(GenericClient.ELECTION_REQ) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") Message: " 
							+ ((TextMessage) msg).getText() + " received by: " + msg.getJMSCorrelationID());

						this.myQueue.sendMessage("Client-" + this.getCLIENT_ID() 
							+ " ACK", GenericClient.ELECTION_ACK, this.getCLIENT_ID(), msg.getJMSCorrelationID());

						ackToReceive = this.convocateElection();
						elecSent = true;
					}
					else if(msg.getJMSType().compareTo(GenericClient.ELECTION_ACK) == 0)
					{
						System.out.println("\nreply to (C-" + this.getCLIENT_ID() + ") Message: " 
							+ ((TextMessage) msg).getText());
						
						ackToReceive--;
					}
					else if(msg.getJMSType().compareTo(GenericClient.COORD_ALIVE) == 0)
					{
						System.out.println("\nreply to (C-" + this.getCLIENT_ID() + ") Message: " 
							+ ((TextMessage) msg).getText());

						// Probably Resource request

						if(this.checkResourceRequest())
						{
							this.resourceRequest();
						}
						else
						{
							System.out.println("\n(C-" + this.getCLIENT_ID() + 
								") I'm NOT requesting the resource to the coordinator...");
							
							break; // se non chiede la risorsa, allora deve ricontrollare il suo stato per capire se deve rimanere vivo o morire, poi eventualmente se rimane vivo, verifica se il coordinatore è alive per richiedere nuovamente la risorsa
						}
					}
					else if(msg.getJMSType().compareTo(GenericClient.RESOURCE_RESP) == 0)
					{
						if(((TextMessage) msg).getText().equals("Y"))
						{
							System.out.println("BAM I'm getting the resource...");
						}
						else
						{
							System.out.println("I'm NOT getting the resource...");
						}
					}
				}
			}
			else if(this.getMyState() == GenericClient.State.DEAD)
			{
				Thread.sleep(6000);
			}
			else if(this.getMyState() == GenericClient.State.COORDINATOR)
			{
				// assegnare l’uso della risorsa a un esecutore
				// individuare se l’esecutore che ha in uso la risorsa non è più attivo

				while(true)
				{
					Message msg = this.myQueue.getQueueReceiver().receive(3000);	
					
					if(msg == null)
					{
						// to do ???
						System.out.println("MSG NULL");
						Thread.sleep(2000);
					}				
					else if(msg.getJMSType().compareTo(GenericClient.COORD_PING) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, C-" 
							+ msg.getJMSCorrelationID() + " contacted me for PING...");
							
						this.myQueue.sendMessage("I'm ALIVE...", GenericClient.COORD_ALIVE, 
							this.getCLIENT_ID(), msg.getJMSCorrelationID());
					}
					else if(msg.getJMSType().compareTo(GenericClient.RESOURCE_REQ) == 0)
					{
						System.out.println("\n(C-" + this.getCLIENT_ID() + ") I'm the coordinator, C-" 
							+ msg.getJMSCorrelationID() + " contacted me for RESOURCE REQUET...");
							
						// if risorsa libera ok else no
						
						String result = "";

						if(!busy)
						{
							result = "Y";
							this.setBusy(true);
						}
						else
						{
							result = "N";
						}

						this.myQueue.sendMessage(result, GenericClient.RESOURCE_RESP, 
							this.getCLIENT_ID(), msg.getJMSCorrelationID());
					}
					else
					{
						System.out.println("YOU ARE WRONGGG!!");
					}
				}
			}
		}
	}


	/**
	 * 
	 * @param receiver
	 * @throws JMSException
	 */
	public void idAssignment(QueueReceiver receiver) throws JMSException
	{
		// The server waits for requests by any client
		Message request = receiver.receive();
		
		System.out.println("\nMessage: " + ((TextMessage) request).getText());

		if(request.getJMSType().compareTo(GenericClient.ID_REQUEST) == 0)
		{
			System.out.println("\nID_REQUEST. Assigned: " + GenericClient.getINCR_ID() + "\n");
			this.myQueue.sendIdResponse(request, Integer.toString(GenericClient.getINCR_ID()), GenericClient.ID_RESPONSE);
			GenericClient.setINCR_ID();
		}
	}


	/**
	 * 
	 * @return
	 * @throws JMSException
	 */
	protected int convocateElection() throws JMSException
	{
		int start = Integer.parseInt(this.getCLIENT_ID()) + 1;
		int end = GenericClient.N_CONNECTED;
		int count = 1; // positivo così nel caso in cui start > end allora il client con id più grande è automaticamente il coordinatore

		if (start <= end)
		{
			count = end - start;

			for (int i = start; i <= end; i++) 
			{
				System.out.println("\nI'm the process with ID-" + this.getCLIENT_ID() 
					+ " and I'm convocating an election to client-" + i);

				// Communication settings: the client-broker sends messages to other peers
				this.myQueue.sendMessage("Election convocated...", GenericClient.ELECTION_REQ, 
					this.getCLIENT_ID(), Integer.toString(i));
			}
		}

		return count;
	}


	/**
	 * 
	 * @throws JMSException
	 */
	protected void sendCoordinatorMsg() throws JMSException
	{
		this.setMyState(GenericClient.State.COORDINATOR);

		int last = Integer.parseInt(this.getCLIENT_ID()) - 1;

		for (int i = 0; i <= last; i++) 
		{
			System.out.println("\nI'm the process with ID-" + this.getCLIENT_ID() 
				+ " and I'm sending coordinator maeesage to client-" + i);

			// Communication settings: the client-broker sends messages to other peers
			this.myQueue.sendMessage("Coordinator elected...", GenericClient.COORDINATOR, 
				this.getCLIENT_ID(), Integer.toString(i));
		}
	}


	/**
	 * 
	 * @param state
	 */
	protected void setMyState(State state) 
	{
		this.myState = state;		
	}


	/**
	 * 
	 * @return
	 */
	public State getMyState() 
	{
		return this.myState;		
	}


	/**
	 * 
	 */
	protected static void setINCR_ID() 
	{
		INCR_ID++;	
	}


	/**
	 * 
	 * @return
	 */
	public static int getINCR_ID()
	{
		return INCR_ID;
	}


	/**
	 * 
	 * @param CLIENT_ID
	 */
	protected void setCLIENT_ID(int CLIENT_ID) 
	{
		this.CLIENT_ID = CLIENT_ID;
	}


	/**
	 * 
	 * @return
	 */
	public String getCLIENT_ID() 
	{
		return Integer.toString(this.CLIENT_ID);
	}

	/**
	 * 
	 * @return
	 */
	public String getCOORD_ID()
	{
		return this.COORD_ID;
	}


	/**
	 * 
	 * @param id
	 */
	protected void setCOORD_ID(String id)
	{
		this.COORD_ID = id;
	}

	/**
	 * 
	 * @return
	 */
	protected QueueSession createSession()
	{
		try 
		{
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(ClientBroker.BROKER_URL);
			ActiveMQConnection connection = (ActiveMQConnection) cf.createConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			this.session = session; // setSession()
			this.connection = connection; // setConnection()
		} 
		catch (JMSException e) 
		{
			e.printStackTrace();
		}
		
		return this.session;
	}


	/**
	 * 
	 * @return
	 */
	protected ActiveMQConnection getConnection()
	{
		return this.connection;
	}

	/**
	 * 
	 * @return
	 */
	protected QueueSession getSession()
	{
		return this.session;
	}

	/**
	 * 
	 * @param val
	 */
	private void setBusy(boolean val)
	{
		this.busy = val;
	}

	/**
	 * 
	 * @return
	 */
	private boolean getBusy()
	{
		return this.busy;
	}
}
