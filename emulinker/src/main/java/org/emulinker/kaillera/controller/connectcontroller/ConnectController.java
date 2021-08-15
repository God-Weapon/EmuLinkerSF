package org.emulinker.kaillera.controller.connectcontroller;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.connectcontroller.protocol.*;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.net.*;
import org.emulinker.util.EmuUtil;

public class ConnectController extends UDPServer
{
	private static Log						log						= LogFactory.getLog(ConnectController.class);

	private ThreadPoolExecutor				threadPool;
	private AccessManager					accessManager;
	private Map<String, KailleraServerController>	controllersMap;

	private int								bufferSize				= 0;

	private long							startTime;
	private int								requestCount			= 0;
	private int								messageFormatErrorCount	= 0;
	private int								protocolErrorCount		= 0;
	private int								deniedServerFullCount	= 0;
	private int								deniedOtherCount		= 0;
	private String               			lastAddress = null;
	private int                             lastAddressCount = 0;
	private int								failedToStartCount		= 0;
	private int								connectedCount			= 0;
	private int								pingCount				= 0;

	public ConnectController(ThreadPoolExecutor threadPool, KailleraServerController[] controllersArray, AccessManager accessManager, Configuration config) throws NoSuchElementException, ConfigurationException, BindException
	{
		super(true);

		this.threadPool = threadPool;
		this.accessManager = accessManager;

		int port = config.getInt("controllers.connect.port");
		bufferSize = config.getInt("controllers.connect.bufferSize");
		if (bufferSize <= 0)
			throw new ConfigurationException("controllers.connect.bufferSize must be > 0");

		controllersMap = new HashMap<String, KailleraServerController>();
		for (KailleraServerController controller : controllersArray)
		{
			String[] clientTypes = controller.getClientTypes();
			for (int j = 0; j < clientTypes.length; j++)
			{
				log.debug("Mapping client type " + clientTypes[j] + " to " + controller);
				controllersMap.put(clientTypes[j], controller);
			}
		}

		super.bind(port);

		System.out.println("Ready to accept connections on port " + port);
		log.info("Ready to accept connections on port " + port);
	}

	public KailleraServerController getController(String clientType)
	{
		return controllersMap.get(clientType);
	}

	public Collection<KailleraServerController> getControllers()
	{
		return controllersMap.values();
	}

	public long getStartTime()
	{
		return startTime;
	}

	public int getBufferSize()
	{
		return bufferSize;
	}

	public int getRequestCount()
	{
		return requestCount;
	}

	public int getMessageFormatErrorCount()
	{
		return messageFormatErrorCount;
	}

	public int getProtocolErrorCount()
	{
		return protocolErrorCount;
	}

	public int getDeniedServerFullCount()
	{
		return deniedServerFullCount;
	}

	public int getDeniedOtherCount()
	{
		return deniedOtherCount;
	}

	public int getFailedToStartCount()
	{
		return failedToStartCount;
	}

	public int getConnectCount()
	{
		return connectedCount;
	}

	public int getPingCount()
	{
		return pingCount;
	}

	protected ByteBuffer getBuffer()
	{
		return ByteBufferMessage.getBuffer(bufferSize);
	}

	protected void releaseBuffer(ByteBuffer buffer)
	{
		ByteBufferMessage.releaseBuffer(buffer);
	}

	public String toString()
	{
		//return "ConnectController[port=" + getBindPort() + " isRunning=" + isRunning() + "]";
		//return "ConnectController[port=" + getBindPort() + "]";
		if (getBindPort() > 0)
			return "ConnectController(" + getBindPort() + ")";
		else
			return "ConnectController(unbound)";
	}

	public synchronized void start()
	{
		startTime = System.currentTimeMillis();
		log.debug(toString() + " Thread starting (ThreadPool:" + threadPool.getActiveCount() + "/" + threadPool.getPoolSize() + ")");
		threadPool.execute(this);
		Thread.yield();
		log.debug(toString() + " Thread started (ThreadPool:" + threadPool.getActiveCount() + "/" + threadPool.getPoolSize() + ")");
	}

	public synchronized void stop()
	{
		super.stop();
		for (KailleraServerController controller : controllersMap.values())
			controller.stop();
	}

	protected synchronized void handleReceived(ByteBuffer buffer, InetSocketAddress fromSocketAddress)
	{
		requestCount++;

		ConnectMessage inMessage = null;

		try
		{
			inMessage = ConnectMessage.parse(buffer);
		}
		catch (MessageFormatException e)
		{
			messageFormatErrorCount++;
			buffer.rewind();
			log.warn("Received invalid message from " + EmuUtil.formatSocketAddress(fromSocketAddress) + ": " + EmuUtil.dumpBuffer(buffer));
			return;
		}

		// the message set of the ConnectController isn't really complex enough to warrant a complicated request/action class 
		// structure, so I'm going to handle it  all in this class alone

		if (inMessage instanceof ConnectMessage_PING)
		{
			pingCount++;
			log.debug("Ping from: " + EmuUtil.formatSocketAddress(fromSocketAddress));
			send(new ConnectMessage_PONG(), fromSocketAddress);
			return;
		}

		if (!(inMessage instanceof ConnectMessage_HELLO))
		{
			messageFormatErrorCount++;
			log.warn("Received unexpected message type from " + EmuUtil.formatSocketAddress(fromSocketAddress) + ": " + inMessage);
			return;
		}

		ConnectMessage_HELLO connectMessage = (ConnectMessage_HELLO) inMessage;

		// now we need to find the specific server this client is request to
		// connect to using the client type
		KailleraServerController protocolController = getController(connectMessage.getProtocol());
		if (protocolController == null)
		{
			protocolErrorCount++;
			log.error("Client requested an unhandled protocol " + EmuUtil.formatSocketAddress(fromSocketAddress) + ": " + connectMessage.getProtocol());
			return;
		}

		if (!accessManager.isAddressAllowed(fromSocketAddress.getAddress()))
		{
			deniedOtherCount++;
			log.warn("AccessManager denied connection from " + EmuUtil.formatSocketAddress(fromSocketAddress));
			return;
		}
		else
		{
			int privatePort = -1;
			int access = accessManager.getAccess(fromSocketAddress.getAddress());

			try
			{
				//SF MOD - Hammer Protection
				if(access < AccessManager.ACCESS_ADMIN && connectedCount > 0){
					if(lastAddress.equals(fromSocketAddress.getAddress().getHostAddress())){
						lastAddressCount++;
						if(lastAddressCount >= 4){
							lastAddressCount = 0;
							failedToStartCount++;
							log.debug("SF MOD: HAMMER PROTECTION (2 Min Ban): " + EmuUtil.formatSocketAddress(fromSocketAddress));
							accessManager.addTempBan(fromSocketAddress.getAddress().getHostAddress(), 2);
							return;
						}
					}
					else{
						lastAddress = fromSocketAddress.getAddress().getHostAddress();
						lastAddressCount = 0;
					}		
				}
				else
					lastAddress = fromSocketAddress.getAddress().getHostAddress();
				
				privatePort = protocolController.newConnection(fromSocketAddress, connectMessage.getProtocol());

				if (privatePort <= 0)
				{
					failedToStartCount++;
					log.error(protocolController + " failed to start for " + EmuUtil.formatSocketAddress(fromSocketAddress));
					return;
				}

				connectedCount++;
				log.debug(protocolController + " allocated port " + privatePort + " to client from " + fromSocketAddress.getAddress().getHostAddress());
				send(new ConnectMessage_HELLOD00D(privatePort), fromSocketAddress);
			}
			catch (ServerFullException e)
			{
				deniedServerFullCount++;
				log.debug("Sending server full response to " + EmuUtil.formatSocketAddress(fromSocketAddress));
				send(new ConnectMessage_TOO(), fromSocketAddress);
				return;
			}
			catch (NewConnectionException e)
			{
				deniedOtherCount++;
				log.warn(protocolController + " denied connection from " + EmuUtil.formatSocketAddress(fromSocketAddress) + ": " + e.getMessage());
				return;
			}
		}
	}

	protected void send(ConnectMessage outMessage, InetSocketAddress toSocketAddress)
	{
		send(outMessage.toBuffer(), toSocketAddress);
		outMessage.releaseBuffer();
	}
}
