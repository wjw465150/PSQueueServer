package wjw.psqueue.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.StandardMBean;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.tanukisoftware.wrapper.WrapperManager;

import wjw.psqueue.msg.ResAdd;
import wjw.psqueue.msg.ResData;
import wjw.psqueue.msg.ResList;
import wjw.psqueue.msg.ResQueueStatus;
import wjw.psqueue.msg.ResSubStatus;
import wjw.psqueue.msg.ResultCode;
import wjw.psqueue.server.jmx.AppMXBean;
import wjw.psqueue.server.jmx.JMXTools;
import wjw.psqueue.server.jmx.annotation.Description;
import wjw.psqueue.server.jmx.annotation.MBean;
import wjw.psqueue.server.jmx.annotation.ManagedOperation;

import com.leansoft.bigqueue.FanOutQueueImplEx;

/**
 * 开源轻量级"扇出队列"服务. User: wstone Date: 2014-06-11 Time: 11:11:11
 */
@MBean(objectName = "wjw.psqueue:type=PSQueueServer", description = "PubSub Queue Server")
public class App extends StandardMBean implements AppMXBean, Runnable {
	public static final String DB_CHARSET = "UTF-8"; //数据库字符集
	public static final long DBFILE_MAXSIZE = 2147483648L; //队列数据文件最大大小(字节,缺省2G)

	org.slf4j.Logger _log = org.slf4j.LoggerFactory.getLogger(this.getClass());
	Conf _conf; //配置文件

	private boolean _rmiCreated;
	private Registry _rmiRegistry; //RIM 注册表
	private JMXConnectorServer _jmxCS; //JMXConnectorServer

	private static FanOutQueueImplEx.QueueFilenameFilter queueDirFilter = new FanOutQueueImplEx.QueueFilenameFilter();
	private static FanOutQueueImplEx.SubFilenameFilter subDirFilter = new FanOutQueueImplEx.SubFilenameFilter();
	private Map<String, FanOutQueueImplEx> _mapQueue;

	//GC的Scheduled
	public ScheduledExecutorService _scheduleGc = Executors.newSingleThreadScheduledExecutor();

	private io.netty.channel.Channel _channel; //Socket通道
	private EventLoopGroup _bossGroup;
	private EventLoopGroup _workerGroup;

	private static Lock _lock = new ReentrantLock(); //创建,删除队列,订阅者的锁

	//初始化
	static {
		try {
			File file = new File(System.getProperty("user.dir", ".") + "/conf/");
			if (!file.exists() && !file.mkdirs()) {
				throw new IOException("Can not create:" + file.getCanonicalPath());
			}

			file = new File(System.getProperty("user.dir", ".") + "/db/");
			if (!file.exists() && !file.mkdirs()) {
				throw new IOException("Can not create:" + file.getCanonicalPath());
			}

			final String logPath = System.getProperty("user.dir", ".") + "/conf/log4j.xml";
			if (logPath.toLowerCase().endsWith(".xml")) {
				DOMConfigurator.configure(logPath);
			} else {
				PropertyConfigurator.configure(logPath);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String args[]) {
		@SuppressWarnings("unused")
		final App app = new App(args);
	}

	@Override
	//定时GC
	public void run() {
		this.gc();
	}

	/**
	 * 构造函数
	 * 
	 * @param args
	 */
	public App(String args[]) {
		super(AppMXBean.class, true);

		java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				doStop();
			}
		}));

		if (!this.doStart()) {
			System.exit(-1);
		}

	}

	private String sanitizeFilename(final String unsanitized) {
		return unsanitized.replaceAll("[\\?\\\\/:|<>\\*]", " ") // filter out ? \ / : | < > *
		    .replaceAll("\\s", "_"); // white space as underscores
	}

	private boolean validUser(final String user, final String pass) {
		if (this._conf.adminUser.equals(user) && this._conf.adminPass.equals(pass)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean validQueueName(final String queueName) {
		return queueName.equals(sanitizeFilename(queueName));
	}

	private boolean validSubName(final String subName) {
		return subName.equals(sanitizeFilename(subName));
	}

	@Override
	protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
		try {
			MBeanParameterInfo[] mBeanParameterInfos = op.getSignature();
			Class<?>[] cls = new Class[mBeanParameterInfos.length];
			for (int i = 0; i < mBeanParameterInfos.length; i++) {
				if (JMXTools.mapJMXPrimitive.get(mBeanParameterInfos[i].getType()) == null) {
					cls[i] = Class.forName(mBeanParameterInfos[i].getType());
				} else {
					cls[i] = JMXTools.mapJMXPrimitive.get(mBeanParameterInfos[i].getType());
				}
			}

			String name = null;

			Method mm = AppMXBean.class.getMethod(op.getName(), cls);
			Annotation[][] a2 = mm.getParameterAnnotations();
			name = ((Description) a2[sequence][0]).name();

			return name == null ? super.getDescription(op, param, sequence) : name;
		} catch (Exception e) {
			return super.getDescription(op, param, sequence);
		}
	}

	@Override
	protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
		try {
			MBeanParameterInfo[] mBeanParameterInfos = op.getSignature();
			Class<?>[] cls = new Class[mBeanParameterInfos.length];
			for (int i = 0; i < mBeanParameterInfos.length; i++) {
				if (JMXTools.mapJMXPrimitive.get(mBeanParameterInfos[i].getType()) == null) {
					cls[i] = Class.forName(mBeanParameterInfos[i].getType());
				} else {
					cls[i] = JMXTools.mapJMXPrimitive.get(mBeanParameterInfos[i].getType());
				}
			}

			String des = null;

			Method mm = AppMXBean.class.getMethod(op.getName(), cls);
			Annotation[][] a2 = mm.getParameterAnnotations();
			des = ((Description) a2[sequence][0]).description();

			return des == null ? super.getDescription(op, param, sequence) : des;
		} catch (Exception e) {
			return super.getDescription(op, param, sequence);
		}

	}

	@Override
	protected String getDescription(MBeanOperationInfo op) {
		try {
			MBeanParameterInfo[] mBeanParameterInfos = op.getSignature();
			Class<?>[] cls = new Class[mBeanParameterInfos.length];
			for (int i = 0; i < mBeanParameterInfos.length; i++) {
				if (JMXTools.mapJMXPrimitive.get(mBeanParameterInfos[i].getType()) == null) {
					cls[i] = Class.forName(mBeanParameterInfos[i].getType());
				} else {
					cls[i] = JMXTools.mapJMXPrimitive.get(mBeanParameterInfos[i].getType());
				}
			}

			String des = null;

			Method mm = AppMXBean.class.getMethod(op.getName(), cls);
			if (mm.getAnnotation(ManagedOperation.class) != null) {
				des = mm.getAnnotation(ManagedOperation.class).description();
			}

			return des == null ? super.getDescription(op) : des;
		} catch (Exception e) {
			return super.getDescription(op);
		}
	}

	@Override
	protected String getDescription(MBeanInfo info) {
		return this.getClass().getAnnotation(MBean.class).description();
	}

	public ResultCode gc() {
		for (FanOutQueueImplEx fq : _mapQueue.values()) {
			try {
				fq.limitBackFileSize(_conf.queues.get(fq.getQueueName()).dbFileMaxSize);
			} catch (Exception ex) {
				_log.error(ex.getMessage(), ex);
			}
		}

		return ResultCode.SUCCESS;
	}

	public ResultCode createQueue(String queueName, long dbFileMaxSize, final String user, final String pass) {
		_lock.lock();
		try {
			queueName = queueName.toUpperCase();
			if (validUser(user, pass) == false) {
				return ResultCode.AUTHENTICATION_FAILURE;
			}

			if (validQueueName(queueName) == false) {
				return ResultCode.QUEUE_NAME_INVALID;
			}

			if (_mapQueue.get(queueName) != null) {
				return ResultCode.QUEUE_IS_EXIST;
			}

			if (dbFileMaxSize < 1L * (1024 * 1024 * 1024) || dbFileMaxSize > 50L * (1024 * 1024 * 1024)) {
				return ResultCode.QUEUE_SZIE_INVALID;
			}

			try {
				_mapQueue.put(queueName, new FanOutQueueImplEx(_conf.dbPath, queueName));
				
				_conf.queues.put(queueName, new QueueConf(queueName, dbFileMaxSize));
				String confFileName = System.getProperty("user.dir", ".") + "/conf/conf.xml";
				_conf.store(confFileName);

				_log.info("createQueue():" + queueName);
				return ResultCode.SUCCESS;
			} catch (Exception ex) {
				try {
					_mapQueue.remove(queueName).erase();
				} catch (Exception e) {
				}

				_log.error(ex.getMessage(), ex);
				return ResultCode.QUEUE_CREATE_ERROR;
			}
		} finally {
			_lock.unlock();
		}
	}

	public ResultCode createSub(String queueName, String subName, final String user, final String pass) {
		_lock.lock();
		try {
			queueName = queueName.toUpperCase();
			subName = subName.toUpperCase();
			if (validUser(user, pass) == false) {
				return ResultCode.AUTHENTICATION_FAILURE;
			}

			if (validQueueName(queueName) == false) {
				return ResultCode.QUEUE_NAME_INVALID;
			}
			if (validSubName(subName) == false) {
				return ResultCode.SUB_NAME_INVALID;
			}

			FanOutQueueImplEx queue = _mapQueue.get(queueName);
			if (queue == null) {
				return ResultCode.QUEUE_NOT_EXIST;
			}

			if (queue.containFanout(subName) == true) {
				return ResultCode.SUB_IS_EXIST;
			}

			try {
				queue.addFanout(subName);

				_log.info("createSub() Queue:" + queueName + ",Created Sub:" + subName);

				return ResultCode.SUCCESS;
			} catch (Exception ex) {
				try {
					queue.removeFanout(subName);
				} catch (Exception e) {
				}

				_log.error(ex.getMessage(), ex);
				return ResultCode.QUEUE_CREATE_ERROR;
			}
		} finally {
			_lock.unlock();
		}
	}

	public ResultCode removeQueue(String queueName, final String user, final String pass) {
		_lock.lock();
		try {
			queueName = queueName.toUpperCase();
			if (validUser(user, pass) == false) {
				return ResultCode.AUTHENTICATION_FAILURE;
			}

			FanOutQueueImplEx queue = _mapQueue.get(queueName);
			if (queue == null) {
				return ResultCode.QUEUE_NOT_EXIST;
			}

			try {
				queue = _mapQueue.remove(queueName);
				queue.erase();

				_conf.queues.remove(queueName);
				String confFileName = System.getProperty("user.dir", ".") + "/conf/conf.xml";
				_conf.store(confFileName);

				_log.info("removeQueue():" + queueName);
				return ResultCode.SUCCESS;
			} catch (Exception ex) {
				_log.error(ex.getMessage(), ex);
				return ResultCode.SUB_REMOVE_ERROR;
			}
		} finally {
			_lock.unlock();
		}
	}

	public ResultCode removeSub(String queueName, String subName, final String user, final String pass) {
		_lock.lock();
		try {
			queueName = queueName.toUpperCase();
			subName = subName.toUpperCase();
			if (validUser(user, pass) == false) {
				return ResultCode.AUTHENTICATION_FAILURE;
			}

			FanOutQueueImplEx queue = _mapQueue.get(queueName);
			if (queue == null) {
				return ResultCode.QUEUE_NOT_EXIST;
			}

			if (queue.containFanout(subName) == false) {
				return ResultCode.SUB_NOT_EXIST;
			}

			try {
				queue.removeFanout(subName);

				_log.info("removeSub() Queue:" + queueName + ",Removed Sub:" + subName);
				return ResultCode.SUCCESS;
			} catch (Exception ex) {
				_log.error(ex.getMessage(), ex);
				return ResultCode.SUB_REMOVE_ERROR;
			}
		} finally {
			_lock.unlock();
		}
	}

	public ResQueueStatus status(String queueName) {
		queueName = queueName.toUpperCase();
		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return new ResQueueStatus(ResultCode.QUEUE_NOT_EXIST, queueName);
		}

		try {
			return queue.getQueueInfo(_conf.queues.get(queueName).dbFileMaxSize);
		} catch (Exception ex) {
			_log.error(ex.getMessage(), ex);
			return new ResQueueStatus(ResultCode.INTERNAL_ERROR, queueName);
		}
	}

	public ResSubStatus statusForSub(String queueName, String subName) {
		queueName = queueName.toUpperCase();
		subName = subName.toUpperCase();
		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return new ResSubStatus(ResultCode.QUEUE_NOT_EXIST, queueName, subName);
		}

		if (queue.containFanout(subName) == false) {
			return new ResSubStatus(ResultCode.SUB_NOT_EXIST, queueName, subName);
		}

		try {
			return queue.getFanoutInfo(subName, _conf.queues.get(queueName).dbFileMaxSize);
		} catch (Exception ex) {
			_log.error(ex.getMessage(), ex);
			return new ResSubStatus(ResultCode.INTERNAL_ERROR, queueName, subName);
		}
	}

	public ResList queueNames() {
		List<String> llst = new ArrayList<String>(_mapQueue.size());
		for (String name : _mapQueue.keySet()) {
			llst.add(name);
		}

		return new ResList(ResultCode.SUCCESS, llst);
	}

	public ResList subNames(String queueName) {
		queueName = queueName.toUpperCase();
		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return new ResList(ResultCode.QUEUE_NOT_EXIST);
		}

		List<String> llst = queue.getAllFanoutNames();
		return new ResList(ResultCode.SUCCESS, llst);
	}

	public ResultCode resetQueue(String queueName, final String user, final String pass) {
		queueName = queueName.toUpperCase();
		if (validUser(user, pass) == false) {
			return ResultCode.AUTHENTICATION_FAILURE;
		}

		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return ResultCode.QUEUE_NOT_EXIST;
		}

		try {
			queue.removeAll();

			_log.info("resetQueue():" + queueName);

			return ResultCode.SUCCESS;
		} catch (Exception ex) {
			_log.error(ex.getMessage(), ex);
			return ResultCode.QUEUE_REMOVE_ERROR;
		}
	}

	public ResAdd add(String queueName, final String data) {
		queueName = queueName.toUpperCase();
		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return new ResAdd(ResultCode.QUEUE_NOT_EXIST);
		}

		try {
			long idx = queue.enqueue(data.getBytes(DB_CHARSET));

			return new ResAdd(ResultCode.SUCCESS, idx);
		} catch (Exception ex) {
			_log.error(ex.getMessage(), ex);
			return new ResAdd(ResultCode.QUEUE_ADD_ERROR);
		}
	}

	public ResData poll(String queueName, String subName) {
		queueName = queueName.toUpperCase();
		subName = subName.toUpperCase();
		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return new ResData(ResultCode.QUEUE_NOT_EXIST);
		}

		if (queue.containFanout(subName) == false) {
			return new ResData(ResultCode.SUB_NOT_EXIST);
		}

		try {
			byte[] bb = queue.dequeue(subName);
			if (bb == null) {
				return new ResData(ResultCode.ALL_MESSAGE_CONSUMED);
			} else {
				return new ResData(ResultCode.SUCCESS, new String(bb, DB_CHARSET));
			}
		} catch (NullPointerException ex) {
			return new ResData(ResultCode.QUEUE_NOT_EXIST);
		} catch (IndexOutOfBoundsException ex) {
			return new ResData(ResultCode.INDEX_OUT_OF_BOUNDS);
		} catch (Exception ex) {
			_log.error(ex.getMessage(), ex);
			return new ResData(ResultCode.QUEUE_POLL_ERROR);
		}
	}

	public ResData view(String queueName, final long pos) {
		queueName = queueName.toUpperCase();
		FanOutQueueImplEx queue = _mapQueue.get(queueName);
		if (queue == null) {
			return new ResData(ResultCode.QUEUE_NOT_EXIST);
		}

		try {
			byte[] bb = queue.get(pos);
			return new ResData(ResultCode.SUCCESS, new String(bb, DB_CHARSET));
		} catch (IndexOutOfBoundsException ex) {
			return new ResData(ResultCode.INDEX_OUT_OF_BOUNDS);
		} catch (Exception ex) {
			_log.error(ex.getMessage(), ex);
			return new ResData(ResultCode.INDEX_OUT_OF_BOUNDS);
		}
	}

	public boolean doStart() {
		try {
			String confFileName = System.getProperty("user.dir", ".") + "/conf/conf.xml";
			try {
				_conf = Conf.load(confFileName);
			} catch (Exception ex) {
				//_log.error(ex.getMessage(), ex);
				_conf = new Conf();
				_conf.store(confFileName);
			}
			if (null == _conf.dbPath || 0 == _conf.dbPath.length()) {
				_conf.dbPath = System.getProperty("user.dir", ".") + "/db";
			}
			File dbBaseDir = new File(_conf.dbPath);
			if (!dbBaseDir.exists() && !dbBaseDir.mkdirs()) {
				throw new IOException("Can not create:" + dbBaseDir.getCanonicalPath());
			}

			if (_mapQueue == null) {
				String[] queuesDirName = dbBaseDir.list(queueDirFilter);
				if (_conf.queues == null) {
					_conf.queues = new HashMap<String, QueueConf>(queuesDirName.length);
				}

				_mapQueue = new ConcurrentHashMap<>(queuesDirName.length);
				for (String queueName : queuesDirName) {
					FanOutQueueImplEx queue = new FanOutQueueImplEx(_conf.dbPath, queueName);

					File queueDir = new File(dbBaseDir, queueName);
					String[] subsDirName = queueDir.list(subDirFilter);
					for (String subName : subsDirName) {
						queue.addFanout(subName.substring(FanOutQueueImplEx.getFanoutFolderPrefix().length()));
					}

					_mapQueue.put(queueName, queue);
					if (_conf.queues.containsKey(queueName) == false) { //校验配置文件1
						_conf.queues.put(queueName, new QueueConf(queueName, DBFILE_MAXSIZE));
					}
				}

				String[] confQueues = _conf.queues.keySet().toArray(new String[0]);
				for (String queueName : confQueues) { //校验配置文件2
					if (_mapQueue.containsKey(queueName) == false) {
						_conf.queues.remove(queueName);
					}
				}
				_conf.store(confFileName);

				for (Map.Entry<String, FanOutQueueImplEx> entry : _mapQueue.entrySet()) {
					_log.info("Inited Queue:" + entry.getKey() + ",dbFileMaxSize:" + _conf.queues.get(entry.getKey()).dbFileMaxSize + ",Subs:" + entry.getValue().getAllFanoutNames());
				}
			}

			_scheduleGc.scheduleWithFixedDelay(this, 1, _conf.gcInterval, TimeUnit.MINUTES);

			if (null == _channel) {
				InetSocketAddress addr;
				if (_conf.bindAddress.equals("*")) {
					addr = new InetSocketAddress(_conf.bindPort);
				} else {
					addr = new InetSocketAddress(_conf.bindAddress, _conf.bindPort);
				}

				_bossGroup = new NioEventLoopGroup();
				_workerGroup = new NioEventLoopGroup();
				io.netty.bootstrap.ServerBootstrap server = new io.netty.bootstrap.ServerBootstrap();
				server.group(_bossGroup, _workerGroup)
				    .channel(NioServerSocketChannel.class)
				    .option(ChannelOption.TCP_NODELAY, true)
				    .option(ChannelOption.SO_REUSEADDR, true)
				    .option(ChannelOption.SO_TIMEOUT, _conf.soTimeout * 1000)
				    .option(ChannelOption.SO_BACKLOG, _conf.backlog)
				    .childOption(ChannelOption.TCP_NODELAY, true)
				    .childOption(ChannelOption.SO_REUSEADDR, true)
				    .childOption(ChannelOption.SO_KEEPALIVE, true)
				    .childHandler(new HttpServerChannelInitializer(this));

				_channel = server.bind(addr).sync().channel();

				_log.info(String.format("PStQueue Server is listening on Address:%s Port:%d\n%s", _conf.bindAddress, _conf.bindPort, _conf.toString()));
			}

			if (_jmxCS == null) {
				final Map<String, Object> env = new HashMap<String, Object>();
				env.put(JMXConnectorServer.AUTHENTICATOR, new JMXAuthenticator() {
					public Subject authenticate(Object credentials) {
						final String[] sCredentials = (String[]) credentials;
						final String userName = sCredentials[0];
						final String password = sCredentials[1];
						if (_conf.adminUser.equals(userName) && _conf.adminPass.equals(password)) {
							final Set<Principal> principals = new HashSet<Principal>();
							principals.add(new JMXPrincipal(userName));
							return new Subject(true, principals, Collections.EMPTY_SET, Collections.EMPTY_SET);
						}

						throw new SecurityException("Authentication failed! ");
					}
				});

				final String localHostname = InetAddress.getLocalHost().getHostName();
				LocateRegistry.createRegistry(_conf.jmxPort);
				_log.info("Getting the platform's MBean Server");
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

				JMXServiceURL localUrl = new JMXServiceURL("service:jmx:rmi://" + localHostname + ":" + _conf.jmxPort + "/jndi/rmi://" + localHostname + ":" + _conf.jmxPort + "/jmxrmi");
				JMXServiceURL hostUrl = new JMXServiceURL("service:jmx:rmi://" + "127.0.0.1" + ":" + _conf.jmxPort + "/jndi/rmi://" + "127.0.0.1" + ":" + _conf.jmxPort + "/jmxrmi");
				_log.info("InetAddress.getLocalHost().getHostName() Connection URL: " + localUrl);
				_log.info("Used host Connection URL: " + hostUrl);

				_log.info("Creating RMI connector server");
				_jmxCS = JMXConnectorServerFactory.newJMXConnectorServer(hostUrl, env, mbs);
				_jmxCS.start();

				JMXTools.registerMBean(this, this.getClass().getAnnotation(MBean.class).objectName());
			}

			if (!WrapperManager.isControlledByNativeWrapper()) {
				System.out.println("Started Standalone PSQueue Server!");
			}

			return true;
		} catch (Throwable ex) {
			_log.error(ex.getMessage(), ex);
			return false;
		}
	}

	public boolean doStop() {
		_scheduleGc.shutdown();

		if (_channel != null) {
			try {
				_log.info("Now stoping PSQueue Server ......");

				final ChannelFuture channelFuture = _channel.close();
				channelFuture.awaitUninterruptibly();
			} catch (Throwable ex) {
				_log.error(ex.getMessage(), ex);
			} finally {
				_channel = null;
				_log.info("PSQueue Server is stoped!");
			}

			try {
				_bossGroup.shutdownGracefully();
			} catch (Throwable ex) {
				_log.error(ex.getMessage(), ex);
			}
			try {
				_workerGroup.shutdownGracefully();
			} catch (Throwable ex) {
				_log.error(ex.getMessage(), ex);
			}
		}

		if (_jmxCS != null) {
			try {
				_jmxCS.stop();
			} catch (Throwable ex) {
				_log.error(ex.getMessage(), ex);
			} finally {
				_jmxCS = null;
			}
		}

		if (_mapQueue != null) {
			for (FanOutQueueImplEx fq : _mapQueue.values()) {
				try {
					fq.close();
				} catch (Exception ex) {
					_log.error(ex.getMessage(), ex);
				}
			}
			_mapQueue.clear();
			_mapQueue = null;
		}

		if (_rmiCreated && _rmiRegistry != null) {
			try {
				UnicastRemoteObject.unexportObject(_rmiRegistry, true);
			} catch (Throwable ex) {
				_log.error(ex.getMessage(), ex);
			} finally {
				_rmiRegistry = null;
			}
		}

		if (!WrapperManager.isControlledByNativeWrapper()) {
			System.out.println("Stoped Standalone PSQueue!");
		}
		return true;
	}

}
