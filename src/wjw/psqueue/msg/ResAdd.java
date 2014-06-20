package wjw.psqueue.msg;

import java.beans.ConstructorProperties;
import java.io.Serializable;

public class ResAdd implements Serializable {
	public ResultCode status;
	public long idx;

	public ResAdd() {

	}

	@ConstructorProperties({ "status" })
	public ResAdd(ResultCode status) {
		this.status = status;
		this.idx = -1;
	}

	@ConstructorProperties({ "status", "idx" })
	public ResAdd(ResultCode status, long idx) {
		this.status = status;
		this.idx = idx;
	}

	public ResultCode getStatus() {
		return status;
	}

	public long getIdx() {
		return idx;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResAdd [status=").append(status).append(", idx=").append(idx).append("]");
		return builder.toString();
	}

}
