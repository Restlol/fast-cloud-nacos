package fast.cloud.nacos.grpc.starter.binding;

import fast.cloud.nacos.grpc.starter.GrpcClient;
import fast.cloud.nacos.grpc.starter.annotation.GrpcService;
import fast.cloud.nacos.grpc.starter.constant.GrpcResponseStatus;
import fast.cloud.nacos.grpc.starter.constant.SerializeType;
import fast.cloud.nacos.grpc.starter.exception.GrpcException;
import fast.cloud.nacos.grpc.starter.service.GrpcRequest;
import fast.cloud.nacos.grpc.starter.service.GrpcResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.springframework.cglib.proxy.InvocationHandler;

public class GrpcServiceProxy<T> implements InvocationHandler {

    private Class<T> grpcService;

    private Object invoker;

    public GrpcServiceProxy(Class<T> grpcService, Object invoker) {
        this.grpcService = grpcService;
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        String className = grpcService.getName();
        if ("toString".equals(methodName) && args.length == 0) {
            return className + "@" + invoker.hashCode();
        } else if ("hashCode".equals(methodName) && args.length == 0) {
            return invoker.hashCode();
        } else if ("equals".equals(methodName) && args.length == 1) {
            Object another = args[0];
            return proxy == another;
        }
        GrpcService annotation = grpcService.getAnnotation(GrpcService.class);
        String server = annotation.server();
        GrpcRequest request = new GrpcRequest();
        request.setClazz(className);
        request.setMethod(methodName);
        request.setArgs(args);
        SerializeType[] serializeTypeArray = annotation.serialization();
        SerializeType serializeType = null;
        if (serializeTypeArray.length > 0) {
            serializeType = serializeTypeArray[0];
        }
        GrpcResponse response = GrpcClient.connect(server).handle(serializeType, request);
        if (GrpcResponseStatus.ERROR.getCode() == response.getStatus()) {
            Throwable throwable = response.getException();
            GrpcException exception = new GrpcException(throwable.getClass().getName() + ": " + throwable.getMessage());
            StackTraceElement[] exceptionStackTrace = exception.getStackTrace();
            StackTraceElement[] responseStackTrace = response.getStackTrace();
            StackTraceElement[] allStackTrace = Arrays.copyOf(exceptionStackTrace, exceptionStackTrace.length + responseStackTrace.length);
            System.arraycopy(responseStackTrace, 0, allStackTrace, exceptionStackTrace.length, responseStackTrace.length);
            exception.setStackTrace(allStackTrace);
            throw exception;
        }
        return response.getResult();
    }

}
