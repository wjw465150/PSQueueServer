package wjw.psqueue.server;

import org.tanukisoftware.wrapper.WrapperManager;
import org.tanukisoftware.wrapper.WrapperSimpleApp;

public class Wrapper extends WrapperSimpleApp {
	@Override
	public Integer start(String[] args) {
		Integer result = super.start(args);

		WrapperManager.log(WrapperManager.WRAPPER_LOG_LEVEL_FATAL, "Started Wrapper PSQueue!");

		return result;
	}

	@Override
	public int stop(int exitCode) {
		int result = super.stop(exitCode);

		WrapperManager.log(WrapperManager.WRAPPER_LOG_LEVEL_FATAL, "Stoped Wrapper PSQueue!");

		return result;
	}

	protected Wrapper(String[] strings) {
		super(strings);
	}

	public static void main(String args[]) {
		new Wrapper(args);
	}

}
