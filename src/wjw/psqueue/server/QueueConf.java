package wjw.psqueue.server;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class QueueConf {
	public String name; //队列名

	public long dbFileMaxSize; //队列数据文件最大大小(字节)

	public QueueConf() {
		super();
	}

	public QueueConf(String name, long dbFileMaxSize) {
		super();
		this.name = name;
		this.dbFileMaxSize = dbFileMaxSize;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[name=").append(name).append(", dbFileMaxSize=").append(dbFileMaxSize).append("]");
		return builder.toString();
	}
}
