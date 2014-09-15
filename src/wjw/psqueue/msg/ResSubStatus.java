package wjw.psqueue.msg;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class ResSubStatus implements Serializable {
	public ResultCode status;
	public String queueName;
	public long dbFileMaxSize;
	public String subName;
	public long size;
	public long head;
	public long tail;

	public ResSubStatus() {

	}

	@ConstructorProperties({ "status", "queueName", "subName" })
	public ResSubStatus(ResultCode status, String queueName, String subName) {
		this.status = status;
		this.queueName = queueName;
		this.subName = subName;
		this.dbFileMaxSize = 0;
		this.size = 0;
		this.head = 0;
		this.tail = 0;
	}

	@ConstructorProperties({ "status", "queueName", "dbFileMaxSize", "subName", "size", "head", "tail" })
	public ResSubStatus(ResultCode status, String queueName, long dbFileMaxSize, String subName, long size, long head, long tail) {
		this.status = status;
		this.queueName = queueName;
		this.dbFileMaxSize = dbFileMaxSize;
		this.subName = subName;
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

	public String getSubName() {
		return subName;
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
		builder.append("ResSubStatus [status=")
		    .append(status)
		    .append(", queueName=")
		    .append(queueName)
		    .append(", dbFileMaxSize=")
		    .append(dbFileMaxSize)
		    .append(", subName=")
		    .append(subName)
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
