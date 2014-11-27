package org.codemucker.cqrs.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.jfind.ClassFilter;
import org.codemucker.jfind.ClassScanner;
import org.codemucker.jfind.FindResult;
import org.codemucker.jfind.matcher.AClass;
import org.codemucker.jfind.matcher.AMethod;
import org.codemucker.jfind.matcher.AResource;
import org.codemucker.jmatch.AString;
import org.codemucker.jmatch.Matcher;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;

public class DefaultMessageHandlerProvider implements IMessageHandlerProvider {

    private final Map<Class<?>, IMessageHandler> handlers = new HashMap<>();
    private final IMessageHandler defaultHandler;

    public static Builder with(){
        return new Builder();
    }
    
    private DefaultMessageHandlerProvider(IMessageHandler defaultHandler, Iterable<IMessageHandler> handlers) {
        this.defaultHandler = defaultHandler;
        for (IMessageHandler h : handlers) {
            this.handlers.put(h.getMessageType(), h);
        }
    }

    public Collection<IMessageHandler> getHandlers(){
        return new ArrayList<>(handlers.values());
    }
    
    @Override
    public IMessageHandler getHandlerFor(Class<?> messageType) {
        IMessageHandler handler = handlers.get(messageType);
        if (handler != null) {
            return handler;
        }
        handler = defaultHandler;
        if (handler != null) {
            return handler;
        }
        throw new NotFoundException("No handler founf for request");
    }

    public static class Builder {

        private static final Logger log = LogManager.getLogger(Builder.class);

        private List<IMessageHandler> handlers = new ArrayList<>();
        private IMessageHandler defaultHandler;

        public DefaultMessageHandlerProvider build() {
            return new DefaultMessageHandlerProvider(defaultHandler, handlers);
        }

        public Builder defaultHandler(IMessageHandler defaultHandler) {
            this.defaultHandler = defaultHandler;
            return this;
        }

        public Builder scanInjectorForMessageHandlers(Injector injector) {
            scanInjectorForMessageHandlers("*",injector);
            return this;
        }
        
        public Builder scanInjectorForMessageHandlers(String handlerPkg, Injector injector) {
            Matcher<String> pkgMatcher = AString.matchingAntPattern(handlerPkg);
            Map<Key<?>, Binding<?>> bindings = injector.getBindings();
            for (Binding<?> b : bindings.values()) {
                Class handlerClass = (Class) b.getKey().getTypeLiteral().getType();
                if (handlerClass.isAnnotationPresent(MessageHandler.class) && pkgMatcher.matches(handlerClass.getPackage().getName())) {
                    handler(handlerClass, b.getProvider());
                }
            }
            return this;
        }

        public Builder scanClasspathForMessageHandlersInPackage(String handlerPkg, Injector injector) {
            FindResult<Class<?>> foundClasses = ClassScanner.with()
                    .filter(ClassFilter.with().resourceMatches(AResource.with().packageName(handlerPkg + "**")).classMatches(AClass.that()
                    // .annotation(CqrsRequestHandler.class)
                            .isNotAbstract().isNotInterface().method(AMethod.with().annotation(MessageHandler.class)))).build().findClasses();

            for (Class type : foundClasses) {
                Provider<?> provider = injector.getProvider(type);
                if (log.isDebugEnabled()) {
                    log.debug("found handler:" + type.getName());
                }
                handler(type, provider);
            }
            return this;
        }

        public <THandler> Builder handler(Class<THandler> handlerClass, Provider<THandler> handlerProvider, Method handlerMethod, Class<?> messageType) {
            DefaultMessageHandler invoker = DefaultMessageHandler.createMessageHandler(handlerProvider, handlerMethod, messageType);
            handler(invoker);
            return this;
        }

        public Builder handler(Object handlerInstance) {
            for (DefaultMessageHandler invoker : DefaultMessageHandler.createMessageHandlers(handlerInstance)) {
                handler(invoker);
            }
            return this;
        }
        
        public Builder handler(Object handlerInstance,Class<?> messageType) {
            for (DefaultMessageHandler invoker : DefaultMessageHandler.createMessageHandlers(handlerInstance,messageType)) {
                handler(invoker);
            }
            return this;
        }
        
        public <THandler> Builder handler(Class<THandler> handlerClass, Provider<THandler> handlerProvider) {
            for (DefaultMessageHandler invoker : DefaultMessageHandler.createMessageHandlers(handlerClass, handlerProvider)) {
                handler(invoker);
            }
            return this;
        }

        public Builder handler(IMessageHandler handler) {
            this.handlers.add(handler);
            return this;
        }

        public Builder handlers(Iterable<IMessageHandler> handlers) {
            if (handlers != null) {
                for (IMessageHandler h : handlers) {
                    this.handlers.add(h);
                }
            }
            return this;
        }
    }

}
