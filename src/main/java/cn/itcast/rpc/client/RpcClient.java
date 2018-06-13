package cn.itcast.rpc.client;

import cn.itcast.rpc.common.RpcDecoder;
import cn.itcast.rpc.common.RpcEncoder;
import cn.itcast.rpc.common.RpcRequest;
import cn.itcast.rpc.common.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);
    private String host;
    private  int port;
    private RpcResponse rpcResponse;
    private final Object obj=new Object();
    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public RpcResponse send(RpcRequest request){
        EventLoopGroup group=new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RpcEncoder(RpcRequest.class))
                                    .addLast(new RpcDecoder(RpcResponse.class))
                                    .addLast(RpcClient.this);
                        }
                    }).option(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.connect(host, port).sync();
            //将request对象写入outbundle处理后发出（即RpcEncoder编码器）
            future.channel().writeAndFlush(request).sync();
            synchronized (obj) {
                obj.wait();
            }

            if (rpcResponse != null) {
                future.channel().closeFuture().sync();
            }
            return rpcResponse;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
         finally {
            group.shutdownGracefully();
        }
        return rpcResponse;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        this.rpcResponse = rpcResponse;
        //System.out.println(rpcResponse.getResult());

        synchronized (obj) {
            obj.notifyAll();
        }
          }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }
}
