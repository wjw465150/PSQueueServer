package wjw.psqueue.msg;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResList implements Serializable {
	public ResultCode status;
	public List<String> data;

	public ResList() {

	}

	@ConstructorProperties({ "status" })
	public ResList(ResultCode status) {
		this.status = status;
		this.data = new ArrayList<String>();
	}

	@ConstructorProperties({ "status", "data" })
	public ResList(ResultCode status, List<String> data) {
		this.status = status;
		this.data = data;
	}

	public ResultCode getStatus() {
		return status;
	}

	public List<String> getData() {
		return data;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResList [status=").append(status).append(", data=").append(data).append("]");
		return builder.toString();
	}

}