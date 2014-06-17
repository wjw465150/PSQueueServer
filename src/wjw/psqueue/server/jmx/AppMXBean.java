package wjw.psqueue.server.jmx;


import wjw.psqueue.msg.ResAdd;
import wjw.psqueue.msg.ResData;
import wjw.psqueue.msg.ResList;
import wjw.psqueue.msg.ResQueueStatus;
import wjw.psqueue.msg.ResSubStatus;
import wjw.psqueue.msg.ResultCode;
import wjw.psqueue.server.jmx.annotation.Description;
import wjw.psqueue.server.jmx.annotation.ManagedOperation;

public interface AppMXBean {
	@ManagedOperation(description = "回收垃圾文件")
	public ResultCode gc();

	@ManagedOperation(description = "创建队列")
	public ResultCode createQueue(@Description(name = "queueName", 
	description = "队列名") String queueName,
	@Description(name = "user", description = "用户名") final String user,
	@Description(name = "pass", description = "口令") final String pass);

	@ManagedOperation(description = "创建指定队列的指定消费者")
	public ResultCode createSub(@Description(name = "queueName", description = "队列名") String queueName, 
			@Description(name = "subName", description = "订阅者名") String subName,
	@Description(name = "user", description = "用户名") final String user,
	@Description(name = "pass", description = "口令") final String pass);

	@ManagedOperation(description = "删除指定队列")
	public ResultCode removeQueue(@Description(name = "queueName", description = "队列名") String queueName,
			@Description(name = "user", description = "用户名") final String user,
			@Description(name = "pass", description = "口令") final String pass);

	@ManagedOperation(description = "删除指定队列的指定消费者")
	public ResultCode removeSub(@Description(name = "queueName", description = "队列名") String queueName, 
			@Description(name = "subName", description = "订阅者名") String subName,
			@Description(name = "user", description = "用户名") final String user,
			@Description(name = "pass", description = "口令") final String pass);

	@ManagedOperation(description = "队列状态")
	public ResQueueStatus status(@Description(name = "queueName", description = "队列名") String queueName);

	@ManagedOperation(description = "队列状态")
	public ResSubStatus statusForSub(@Description(name = "queueName", description = "队列名") String queueName, 
			@Description(name = "subName", description = "订阅者名") String subName);

	@ManagedOperation(description = "获取全部队列名")
	public ResList queueNames();

	@ManagedOperation(description = "获取指定队列名的全部订阅者")
	public ResList subNames(@Description(name = "queueName", description = "队列名") String queueName);

	@ManagedOperation(description = "重置队列")
	public ResultCode resetQueue(@Description(name = "queueName", 
	    description = "队列名") String queueName,
			@Description(name = "user", description = "用户名") final String user,
			@Description(name = "pass", description = "口令") final String pass);

	@ManagedOperation(description = "添加数据-到指定队列")
	public ResAdd add(@Description(name = "queueName", description = "队列名") String queueName, 
			@Description(name = "data", description = "数据") final String data);

	@ManagedOperation(description = "获取数据-从指定队列")
	public ResData poll(@Description(name = "queueName", description = "队列名") String queueName, 
			@Description(name = "subName", description = "订阅者名") String subName);

	@ManagedOperation(description = "查看指定队列内容")
	public ResData view(@Description(name = "queueName", description = "队列名") String queueName, 
			@Description(name = "pos", description = "查看的位置") final long pos);
}
