package wjw.psqueue.msg;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class ResData implements Serializable {
	public final ResultCode status;
	public final String data;

	@ConstructorProperties({ "status" })
	public ResData(ResultCode status) {
		this.status = status;
		this.data = "";
	}

	@ConstructorProperties({ "status", "data" })
	public ResData(ResultCode status, String data) {
		this.status = status;
		this.data = data;
	}

	public ResultCode getStatus() {
		return status;
	}

	public String getData() {
		return data;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResData [status=").append(status).append(", data=").append(data).append("]");
		return builder.toString();
	}

}
