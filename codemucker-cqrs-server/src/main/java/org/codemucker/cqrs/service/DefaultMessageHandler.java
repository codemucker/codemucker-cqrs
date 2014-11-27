package org.codemucker.cqrs.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.codemucker.cqrs.Message;
import org.codemucker.cqrs.MessageException;

import com.google.common.base.Joiner;
import com.google.inject.Provider;

class DefaultMessageHandler implements IMessageHandler {

    private final Class<?> messageType;
    private final Provider<?> handlerProvider;
    final Method method;

    private final int numArgs;
    private final int argCmdIdx;
    private final int argRequestIdx;

    public static List<DefaultMessageHandler> createMessageHandlers(Object handlerInstance) {
        return createMessageHandlers(handlerInstance.getClass(), new InstanceProvider(handlerInstance));
    }

    public static List<DefaultMessageHandler> createMessageHandlers(Object handlerInstance, Class<?> messageType) {
        return createMessageHandlers(handlerInstance.getClass(), new InstanceProvider(handlerInstance),messageType);
    }

    public static <THandler> List<DefaultMessageHandler> createMessageHandlers(Class<THandler> handlerClass, Provider<THandler> handlerProvider) {
        return createMessageHandlers(handlerClass,handlerProvider,Message.class);
    }
    
    public static <THandler> List<DefaultMessageHandler> createMessageHandlers(Class<THandler> handlerClass, Provider<THandler> handlerProvider, Class<?> messageType) {
        List<DefaultMessageHandler> invokers = new ArrayList<>();
        for (Method m : handlerClass.getClass().getMethods()) {
            if (m.getParameterTypes().length > 0 && m.isAnnotationPresent(MessageHandler.class)) {
                for (Class<?> paramType : m.getParameterTypes()) {
                    if (paramType != Object.class && paramType.isAssignableFrom(messageType)) {
                        DefaultMessageHandler invoker = createMessageHandler(handlerProvider, m, paramType);
                        invokers.add(invoker);
                    }
                }
            }
        }
        return invokers;
    }

    public static <THandler> DefaultMessageHandler createMessageHandler(Provider<THandler> handlerProvider, Method m, Class<?> messageType) {

        // find handler method containing any of the following (and nothing
        // else) in any order, with atleast the cmd object as an arg
        final Class<?>[] supportArgType = new Class<?>[] { messageType, HttpRequest.class };// ,
                                                                                            // Callback.class
                                                                                            // };

        if (m.getParameterTypes().length > 0) {
            // int callbackIdx = -1;
            int requestIdx = -1;
            int cmdIdx = -1;
            int unknownArgCount = 0;
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                Class<?> param = params[i];
                // if (param.isAssignableFrom(Callback.class)) {
                // checkNotSet(callbackIdx, Callback.class, m);
                // callbackIdx = i;
                // } else
                if (param.isAssignableFrom(HttpRequest.class)) {
                    checkNotSet(requestIdx, HttpRequest.class, m);
                    requestIdx = i;
                } else if (param.isAssignableFrom(messageType)) {
                    checkNotSet(cmdIdx, messageType, m);
                    cmdIdx = i;
                } else {
                    // bad arg
                    unknownArgCount++;
                }
            }
            if (cmdIdx != -1) {
                // found matching method
                if (unknownArgCount > 0) {
                    throw new IllegalArgumentException("Method " + m.toGenericString() + " on " + m.getDeclaringClass().getName() + " for request bean "
                            + messageType.getName() + " contains unsupported parameters. Expect only in any order " + Joiner.on(',').join(supportArgType));
                }
                // found our method
                // TODO:look for duplicates?
                return new DefaultMessageHandler(handlerProvider, messageType, m, cmdIdx, requestIdx);
            }
        }

        throw new IllegalArgumentException("Could not find handler method called  for request bean  " + messageType.getName() + " on "
                + m.getDeclaringClass().getName() + ". Expect any or all in any order " + Joiner.on(',').join(supportArgType) + " with at least one of "
                + messageType.getName() + " and method to be marked with annotation " + MessageHandler.class.getName());

    }

    private static void checkNotSet(int idx, Class<?> type, Method m) {
        if (idx != -1) {
            throw new IllegalArgumentException(type.getName() + " can only be declared once on method " + m.getName() + " on class "
                    + m.getDeclaringClass().getName());
        }
    }

    private DefaultMessageHandler(Provider<?> handlerProvider, Class<?> messageType, Method method, int argCmdIdx, int argRequestIdx) {
        super();
        this.handlerProvider = handlerProvider;
        this.method = method;
        this.messageType = messageType;
        this.argCmdIdx = argCmdIdx;
        this.argRequestIdx = argRequestIdx;

        int numArgs = 0;
        if (argCmdIdx != -1) {
            numArgs++;
        }
        if (argRequestIdx != -1) {
            numArgs++;
        }
        this.numArgs = numArgs;
    }

    @Override
    public Object invoke(HttpRequest request, Object requestBean) {

        Object handler = handlerProvider.get();
        try {
            Object[] args = createMethodArgs(request, requestBean);
            Object result = method.invoke(handler, args);
            return result;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new MessageException("Error invoking handler method " + method.getName() + " on handler " + handler.getClass().getName(), e.getCause());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof MessageException) {
                throw (MessageException) e.getCause();
            }
            throw new MessageException("Error invoking handler method " + method.getName() + " on handler " + handler.getClass().getName(), e.getCause());
        }
    }

    private Object[] createMethodArgs(HttpRequest request, Object requestBean) {
        Object[] args = new Object[numArgs];
        if (argRequestIdx != -1) {
            args[argRequestIdx] = requestBean;
        }
        if (argCmdIdx != -1) {
            args[argCmdIdx] = requestBean;
        }
        return args;
    }

    @Override
    public Class<?> getMessageType() {
        return messageType;
    }

    @Override
    public Method getMethod() {
        return method;
    }
    
    private static class InstanceProvider<T> implements Provider<T> {

        private final T instance;

        public InstanceProvider(T instance) {
            super();
            this.instance = instance;
        }

        @Override
        public T get() {
            return instance;
        }
    }

}