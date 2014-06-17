package wjw.psqueue.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {
	private App _app;

	public HttpServerChannelInitializer(App app) {
		_app = app;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		// Create a default pipeline implementation.
		ChannelPipeline pipeline = ch.pipeline();

		// Uncomment the following line if you want HTTPS
		//SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
		//engine.setUseClientMode(false);
		//p.addLast("ssl", new SslHandler(engine));

		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
		pipeline.addLast("encoder", new HttpResponseEncoder());

		// Remove the following line if you don't want automatic content compression.
		//pipeline.addLast("deflater", new HttpContentCompressor());

		pipeline.addLast("handler", new HttpRequestHandler(_app));
	}
}
