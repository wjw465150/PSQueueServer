package wjw.psqueue.msg;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class ResSubStatus implements Serializable {
	public final ResultCode status;
	public final String queueName;
	public final String subName;
	public final long size;
	public final long head;
	public final long tail;

	@ConstructorProperties({ "status", "queueName", "subName" })
	public ResSubStatus(ResultCode status, String queueName, String subName) {
		this.status = status;
		this.queueName = queueName;
		this.subName = subName;
		this.size = 0;
		this.head = 0;
		this.tail = 0;
	}

	@ConstructorProperties({ "status", "queueName", "subName", "size", "head", "tail" })
	public ResSubStatus(ResultCode status, String queueName, String subName, long size, long head, long tail) {
		this.status = status;
		this.queueName = queueName;
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
