package cn.itcast.rpc.client;

import cn.itcast.rpc.common.RpcRequest;
import cn.itcast.rpc.common.RpcResponse;
import cn.itcast.rpc.registry.ServiceDisvover;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

public class RpcProxy {
    private String serverAddress;
    private ServiceDisvover serviceDisvover;

    public RpcProxy(ServiceDisvover serviceDisvover) {
        this.serviceDisvover = serviceDisvover;
        System.out.println(serviceDisvover);
    }

    public RpcProxy(String serverAddress) {

        this.serverAddress = serverAddress;
    }
    @SuppressWarnings("unchecked")
    public <T> T create(Class<?>interfaceClass){
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RpcRequest rpcRequest= new RpcRequest();
                        rpcRequest.setRequesteId(UUID.randomUUID().toString());
                        rpcRequest.setClassName(method.getDeclaringClass().getName());
                        rpcRequest.setMethodName(method.getName());
                        rpcRequest.setParameterTypes(method.getParameterTypes());
                        rpcRequest.setParameters(args);
                        //查找服务
                        if (serviceDisvover != null) {
                            serverAddress = serviceDisvover.discover();
                        }
                        //随机获取服务的地址
                       // System.out.println( serverAddress);
                        String[] array = serverAddress.split(":");
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        //创建Netty实现的RpcClient，链接服务端
                        RpcClient client = new RpcClient(host, port);
                        //通过netty向服务端发送请求

                        RpcResponse response = client.send(rpcRequest);
                        //返回信息
                        if (response.isError() ){
                            System.out.println("出错了");
                            throw response.getError();
                        } else {
                            return response.getResult();
                        }


                    }
                });

    }

}
