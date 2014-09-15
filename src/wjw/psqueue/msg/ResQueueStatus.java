package wjw.psqueue.msg;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class ResQueueStatus implements Serializable {
	public ResultCode status;
	public String queueName;
	public long dbFileMaxSize;
	public long size;
	public long head;
	public long tail;

	public ResQueueStatus() {

	}

	@ConstructorProperties({ "status", "queueName" })
	public ResQueueStatus(ResultCode status, String queueName) {
		this.status = status;
		this.queueName = queueName;
		this.dbFileMaxSize = 0;
		this.size = 0;
		this.head = 0;
		this.tail = 0;
	}

	@ConstructorProperties({ "status", "queueName", "dbFileMaxSize", "size", "head", "tail" })
	public ResQueueStatus(ResultCode status, String queueName, long dbFileMaxSize, long size, long head, long tail) {
		this.status = status;
		this.queueName = queueName;
		this.dbFileMaxSize = dbFileMaxSize;
		this.size = size;
		this.head = head;
		this.tail = tail;
	}

	public ResultCode getStatus() {
		return status;
	}

	public String getQueueName() {
		return queueName;
	}

	public long getDbFileMaxSize() {
		return dbFileMaxSize;
	}

	public long getSize() {
		return size;
	}

	public long getHead() {
		return head;
	}

	public long getTail() {
		return tail;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResQueueStatus [status=")
		    .append(status)
		    .append(", queueName=")
		    .append(queueName)
		    .append(", dbFileMaxSize=")
		    .append(dbFileMaxSize)
		    .append(", size=")
		    .append(size)
		    .append(", head=")
		    .append(head)
		    .append(", tail=")
		    .append(tail)
		    .append("]");
		return builder.toString();
	}
}
