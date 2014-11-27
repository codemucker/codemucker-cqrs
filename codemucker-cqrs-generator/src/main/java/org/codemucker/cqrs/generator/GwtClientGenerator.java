package org.codemucker.cqrs.generator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.cqrs.Path;
import org.codemucker.cqrs.PathExpression;
import org.codemucker.cqrs.PathExpression.Var;
import org.codemucker.cqrs.client.gwt.AbstractCqrsGwtClient;
import org.codemucker.cqrs.client.gwt.GenerateCqrsGwtClient;
import org.codemucker.jfind.FindResult;
import org.codemucker.jfind.ReflectedAnnotation;
import org.codemucker.jfind.ReflectedClass;
import org.codemucker.jfind.ReflectedField;
import org.codemucker.jfind.RootResource;
import org.codemucker.jfind.matcher.AField;
import org.codemucker.jfind.matcher.AMethod;
import org.codemucker.jfind.matcher.AnAnnotation;
import org.codemucker.jmatch.AString;
import org.codemucker.jmatch.Matcher;
import org.codemucker.jmutate.ClashStrategy;
import org.codemucker.jmutate.JMutateContext;
import org.codemucker.jmutate.JMutateException;
import org.codemucker.jmutate.SourceTemplate;
import org.codemucker.jmutate.ast.JAnnotation;
import org.codemucker.jmutate.ast.JField;
import org.codemucker.jmutate.ast.JMethod;
import org.codemucker.jmutate.ast.JSourceFile;
import org.codemucker.jmutate.ast.JType;
import org.codemucker.jmutate.ast.JTypeMutator;
import org.codemucker.jmutate.ast.matcher.AJAnnotation;
import org.codemucker.jmutate.ast.matcher.AJField;
import org.codemucker.jmutate.ast.matcher.AJMethod;
import org.codemucker.jmutate.ast.matcher.AJModifier;
import org.codemucker.jmutate.generate.AbstractGenerator;
import org.codemucker.jmutate.transform.CleanImportsTransform;
import org.codemucker.jmutate.transform.InsertFieldTransform;
import org.codemucker.jmutate.transform.InsertMethodTransform;
import org.codemucker.jmutate.transform.InsertTypeTransform;
import org.codemucker.jmutate.util.NameUtil;
import org.codemucker.jpattern.IsGenerated;
import org.codemucker.lang.BeanNameUtil;
import org.codemucker.lang.ClassNameUtil;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.base.Strings;
import com.google.inject.Inject;

public class GwtClientGenerator extends AbstractGenerator<GenerateCqrsGwtClient> {

    private final Logger log = LogManager.getLogger(GwtClientGenerator.class);

    private static final String CODE_GEN_INFO_CLASS_PKG = "org.codemucker.jmutate.generated";
    private static final String CODE_GEN_INFO_CLASS_NAME = "CodeGeneration";
    
    private static final Matcher<String> cmdRequestPattern = AString.matchingAnyAntPattern("**Cmd|**Comand");
    
    private final Matcher<Annotation> reflectedAnnotationIgnore = AnAnnotation.with().fullName(AString.matchingAntPattern("*.Ignore"));
    private final Matcher<Annotation> reflectedAnnotationQueryParam = AnAnnotation.with().fullName(AString.matchingAntPattern("*.QueryParam"));
    private final Matcher<Annotation> reflectedAnnotationHeaderParam = AnAnnotation.with().fullName(AString.matchingAntPattern("*.HeaderParam"));
    private final Matcher<Annotation> reflectedAnnotationCmdParam = AnAnnotation.with().fullName(AString.matchingAntPattern("*.BodyParam"));

    private final Matcher<JAnnotation> sourceAnnotationIgnore = AJAnnotation.with().fullName(AString.matchingAntPattern("*.Ignore"));
    private final Matcher<JAnnotation> souceAnnotationQueryParam = AJAnnotation.with().fullName(AString.matchingAntPattern("*.QueryParam"));
    private final Matcher<JAnnotation> sourceAnnotationHeaderParam = AJAnnotation.with().fullName(AString.matchingAntPattern("*.HeaderParam"));
    private final Matcher<JAnnotation> sourceAnnotationCmdParam = AJAnnotation.with().fullName(AString.matchingAntPattern("*.BodyParam"));

    
    private final JMutateContext ctxt;

    @Inject
    public GwtClientGenerator(JMutateContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void generate(JType optionsDeclaredInNode, GenerateCqrsGwtClient options) {
        RequestBeanScanner requestScanner = new RequestBeanScanner(ctxt,optionsDeclaredInNode, options);
        
        ServiceModel serviceModel = new ServiceModel(optionsDeclaredInNode, options);
        if(options.scanSources() ){
            FindResult<JType> requestTypes = requestScanner.scanSources();
            // add the appropriate methods and types for each request bean
            for (JType requestType : requestTypes) {
                RequestModel requestModel = new RequestModel(serviceModel, requestType);
                requestModel.restPaths = extractRestPaths(requestType);
                extractFields(requestModel, requestType);
                serviceModel.addRequest(requestModel);
            }
        }
        
        if(options.scanDependencies() && options.requestBeanDependencies().length > 0){
            FindResult<Class<?>> requestTypes = requestScanner.scanForReflectedClasses();
            // add the appropriate methods and types for each request bean
            for (Class<?> requestType : requestTypes) {
                RequestModel requestModel = new RequestModel(serviceModel, requestType);
                requestModel.restPaths = extractRestPaths(requestType);   
                extractFields(requestModel, requestType);
                serviceModel.addRequest(requestModel);
            }
        }
        
        generate(optionsDeclaredInNode, options, serviceModel);
    }

    private void generate(JType optionsDeclaredInNode, GenerateCqrsGwtClient options, ServiceModel serviceModel) {
        createGenInfoIfNotExists();
        // ----- generate interface
        JSourceFile interfaceSource = null;
        if (options.generateInterface()) {
            interfaceSource = newServiceInterface(serviceModel);
        }
        JTypeMutator interfaceMutator = interfaceSource == null ? null : interfaceSource.asMutator(ctxt).getMainTypeAsMutable();

        JSourceFile implementationSource;
        // TODO:use existing node or create new if different file
        if (serviceModel.serviceTypeFull.equals(optionsDeclaredInNode.getFullName())) {
            implementationSource = JSourceFile.fromResource(optionsDeclaredInNode.getResource(), ctxt.getParser());
            // remove generated methods?
        } else {
            implementationSource = newServiceImplementation(serviceModel);
        }

        if (options.generateInterface()) {
            implementationSource.getMainType().asMutator(ctxt).addImplements(serviceModel.interfaceTypeFull);
        } // else remove?

        JTypeMutator implementationMutator = implementationSource.asMutator(ctxt).getMainTypeAsMutable();

        implementationMutator.addImport(CODE_GEN_INFO_CLASS_PKG + "." + CODE_GEN_INFO_CLASS_NAME);
        if(interfaceMutator!=null){
            interfaceMutator.addImport(CODE_GEN_INFO_CLASS_PKG + "." + CODE_GEN_INFO_CLASS_NAME);
        }
    
        for(RequestModel requestModel:serviceModel.requests){
            // ------ add json reader/writer classes
            JType jsonWriter = createJsonWriterType(requestModel);
            addOrReplaceType(implementationMutator, jsonWriter);

            JType jsonReader = createJsonReaderType(requestModel);
            addOrReplaceType(implementationMutator, jsonReader);

            // ------ add request builder create method
            JMethod buildMethod = createBuildMethod(requestModel);
            if (interfaceMutator != null && requestModel.serviceModel.generateInterfaceBuilderMethods) {
                addToInterface(interfaceMutator, buildMethod);
            }
            addOrReplaceMethod(implementationMutator, buildMethod);

            // ------ add async request method
            if (options.generateAsync()) {
                JMethod asyncMethod = createAsyncMethod(requestModel, implementationMutator, options.generateInterface());
                if (interfaceMutator != null) {
                    addToInterface(interfaceMutator, asyncMethod);
                }
                addOrReplaceMethod(implementationMutator, asyncMethod);
            }
        }
        writeToDiskIfChanged(interfaceSource);
        writeToDiskIfChanged(implementationSource);
    }

    private JSourceFile newServiceImplementation(ServiceModel serviceDetails) {
        SourceTemplate template = ctxt.newSourceTemplate().var("serviceType", serviceDetails.serviceTypeSimple).pl("package " + serviceDetails.pkg + ";").pl("")
                .pl("/* generated from definitions in " + serviceDetails.optionsDeclaredInTypeFull + "*/");

        addGeneratedMarkers(template);
        template.pl("public class ${serviceType} extends " + serviceDetails.baseTypeFull + "{");

        // copy relevant super class constructors
        for (Constructor<?> ctor : serviceDetails.options.serviceBaseClass().getConstructors()) {
            if (!Modifier.isPrivate(ctor.getModifiers())) {
                addGeneratedMarkers(template);
                copySuperConstructor(serviceDetails.serviceTypeSimple, template, ctor);
            }
        }
        template.pl("}");

        return template.asSourceFileSnippet();
    }

    // TODO:move to separate transform class?
    private void copySuperConstructor(String ctorName, SourceTemplate template, Constructor<?> ctor) {
        if (ctor.getAnnotation(Inject.class) != null) {
            template.pl("@" + NameUtil.compiledNameToSourceName(Inject.class));
        }
        if (ctor.getAnnotation(javax.inject.Inject.class) != null) {
            template.pl("@" + NameUtil.compiledNameToSourceName(javax.inject.Inject.class));
        }

        if (Modifier.isPublic(ctor.getModifiers())) {
            template.p("public ");
        } else if (Modifier.isProtected(ctor.getModifiers())) {
            template.p("protected ");
        }
        template.p(ctorName + " (");
        List<String> argNames = new ArrayList<>();
        boolean comma = false;
        for (Class<?> param : ctor.getParameterTypes()) {
            if (comma) {
                template.p(",");
            }
            comma = true;
            // come up with some sensible name
            String argName = createArgNameFromType(param.getName(), argNames);
            argNames.add(argName);
            template.p(NameUtil.compiledNameToSourceName(param) + " " + argName);
        }
        template.pl("){");
        template.p("    super(");
        comma = false;
        for (String argName : argNames) {
            if (comma) {
                template.p(",");
            }
            comma = true;
            template.p(argName);
        }
        template.pl(");");
        template.pl("}");
    }

    /**
     * Try to come up with a sensible arg name from the given type.
     * 
     * Ensure the argName is unique
     * 
     * Converts FooBar - > bar, or Foo ->foo
     */
    private static String createArgNameFromType(String paramType, List<String> argNames) {
        String argName = paramType.toLowerCase();
        for (int i = paramType.length() - 1; i >= 0; i--) {
            char c = paramType.charAt(i);
            if (Character.isUpperCase(c)) {
                argName = paramType.substring(i).toLowerCase();
                break;
            }
        }
        if (argNames.contains(argName)) { // ensure unique name
            int i = 2;
            while (argNames.contains(argName + i)) {
                i++;
            }
            argName += i;
        }
        return argName;
    }

    private JSourceFile newServiceInterface(ServiceModel serviceModel) {
        SourceTemplate template = ctxt.newSourceTemplate().pl("package " + serviceModel.pkg + ";").pl("")
                .pl("/* generated from definitions in " + serviceModel.optionsDeclaredInTypeFull + "*/");

        addGeneratedMarkers(template);

        template.pl("public interface " + serviceModel.interfaceTypeSimple + "{}");

        return template.asSourceFileSnippet();
    }

    /**
     * Generate the async call
     */
    private JMethod createAsyncMethod(RequestModel requestModel, JTypeMutator serviceInterfaceClass, boolean hasInterface) {
        String methodName = requestModel.isCmd ? "cmd" : "query";

        // build the interface method
        SourceTemplate template = ctxt.newSourceTemplate()
                .var("responseType", requestModel.responseTypeSimple)
                .var("jsonReaderType", requestModel.jsonReaderTypeSimple)
                .var("requestType", requestModel.requestTypeFull).var("requestArg", requestModel.argName)
                .var("methodName", methodName)
                .var("exceptionType", requestModel.serviceModel.errorTypeFull)
                .var("requestHandle", requestModel.asyncRequestHandleTypeFull);

        template.pl("/** Asynchronously invoke the given request */");
        addGeneratedMarkers(template);

        if (hasInterface) {
            template.pl("@Override");
        }
        template
            .pl("public ${requestHandle} ${methodName}(${requestType} ${requestArg},com.google.gwt.core.client.Callback<${responseType}, ${exceptionType}> callback){")
            .pl("   com.github.nmorel.gwtjackson.client.ObjectReader<${responseType}> jsonResponseReader = com.google.gwt.core.client.GWT.create(${jsonReaderType}.class);")
            .pl("   return invokeAsync(${requestArg},buildRequest(${requestArg}),jsonResponseReader,callback);").pl("}");

        return template.asJMethodSnippet();
    }

    /**
     * Generate the method to extract all the info from the request bean
     */
    private JMethod createBuildMethod(RequestModel requestModel){
        // TODO:also generate from source

        log("creating request builder method for type:" + requestModel.requestTypeFull);

        SourceTemplate template = ctxt.newTempSourceTemplate()
                .var("requestType", requestModel.requestTypeFull)
                .var("requestTypeSimple", requestModel.requestTypeSimple)
                .var("requestArg", requestModel.argName).var("builderType", requestModel.serviceModel.builderTypeFull)
                .var("exceptionType", requestModel.serviceModel.errorTypeFull)
                .pl("/** Return a request builder with all the values extracted from the given ${requestArg} */");

        addGeneratedMarkers(template);

        if (requestModel.serviceModel.generateInterface && requestModel.serviceModel.generateInterfaceBuilderMethods) {
            template.pl("@Override");
        }
        template.pl("public ${builderType} buildRequest(${requestType} ${requestArg}){");

        // validator
        if (requestModel.serviceModel.options.validateRequests()) {
            template.pl("checkForValidationErrors(getValidator().validate(${requestArg}),\"${requestTypeSimple}\");");
        }
        // validate() method
        if (requestModel.validateMethodName != null) {
            template.pl(" ${requestArg}." + requestModel.validateMethodName + "();");
        }

        // start building request
        template.pl("  ${builderType} builder = newRequestBuilder();");
        if (requestModel.isCmd) {
            template.pl("  builder.methodPUT();");
        }
        addRequestPath(template, requestModel);

        for (FieldModel fieldModel : requestModel.getFields()) {
            List<String> buildSetters = new ArrayList<>();
            if(fieldModel.isHeaderParam){
                buildSetters.add("setHeaderIfNotNull");
            }
            if(fieldModel.isQueryParam){
                buildSetters.add("setQueryParamIfNotNull");
            }
            if(fieldModel.isBodyParam){
                buildSetters.add("setBodyParamIfNotNull");
            }
            for(String buildSetter:buildSetters){
                template.pl("builder.${buildSetter}(\"${param}\",${requestArg}.${getter});", "param", fieldModel.paramName, "buildSetter", buildSetter, "getter",fieldModel.fieldGetter);
            }
        }
        // json body
        if (requestModel.isCmd) {
            template.pl("com.github.nmorel.gwtjackson.client.ObjectWriter<${requestType}> jsonRequestWriter = com.google.gwt.core.client.GWT.create(" + requestModel.jsonWriterTypeSimple + ".class);");
            template.pl("builder.bodyRaw(jsonRequestWriter.write(${requestArg}));");
        }

        template.pl("  return builder;");
        template.pl("}");

        return template.asJMethodSnippet();
    }

    /**
     * Add the path parts to the builder
     */
    private void addRequestPath(SourceTemplate template, RequestModel requestModel) {
        List<PathExpression> pathExpressions = requestModel.restPaths;
        if (pathExpressions.isEmpty()) {
            throw new JMutateException("No REST path specified for request bean %s", requestModel.requestTypeFull);
        }
        // only allow a single path with no variables
        String defaultPath = null;
        for (PathExpression result : pathExpressions) {
            if (result.getVars().isEmpty()) {
                if (defaultPath != null) {
                    throw new JMutateException(
                            "There can only be one REST path with no variables as there is no way to choose which one to use otherwise. In bean %s, path '%s'",
                            requestModel.requestTypeFull, defaultPath);
                }
                defaultPath = result.expression;
            }
        }
        boolean hasGuardConditions = false;
        boolean firstGuard = true;
        for (PathExpression expression : pathExpressions) {
            boolean hasVars = !expression.getVars().isEmpty();
            if (hasVars) {
                hasGuardConditions = true;
                if (firstGuard) {
                    template.p("if(");
                } else {
                    template.p("else if(");
                }
                firstGuard = false;
                boolean addSep = false;
                for (String fieldName : expression.getVars()) {
                    FieldModel fd = requestModel.getNamedField(fieldName);
                    if (fd == null) {
                        throw new JMutateException("For rest path expression '%s' in %s, could not find field with name or param '%s'", expression.expression,
                                requestModel.requestTypeFull, fieldName);
                    }
                    if (addSep) {
                        template.p(" && ");
                    }
                    addSep = true;
                    template.p("isFieldSet(${requestArg}." + fd.fieldGetter + ")");
                }
                template.pl("){");

                template.p("    builder");
                for (Object part : expression.getAllParts()) {
                    if (part instanceof Var) {
                        Var var = (Var) part;
                        FieldModel fieldModel = requestModel.getNamedField(var.name);
                        template.p(".addPathPart(${requestArg}." + fieldModel.fieldGetter + ")");
                    } else {
                        template.p(".addSafePathPart(\"" + part + "\")");
                    }
                }
                template.pl(";");
                template.p("}");
            }// hasVars
        }// loop vars
        if (hasGuardConditions) {
            template.pl(" else {");
        }
        if (defaultPath != null) {
            template.pl("    builder.addSafePathPart(\"" + defaultPath + "\");");
        } else {
            template.pl("    throw newInvalidRequestFieldsFor(\"${type}\");", "type", requestModel.requestTypeSimple);
        }
        if (hasGuardConditions) {
            template.pl("}");
        }
    }

    private void extractFields(RequestModel requestModel, Class<?> requestType) {
        ReflectedClass requestBean = ReflectedClass.from(requestType);
        FindResult<Field> fields = requestBean.findFieldsMatching(AField.that().isNotStatic().isNotTransient().isNotNative());
        log.trace("found " + fields.toList().size() + " fields");
        for (Field f : fields) {
            ReflectedField field = ReflectedField.from(f);
            if (field.hasAnnotation(reflectedAnnotationIgnore)) {
                log("ignoring field:" + f.getName());
                continue;
            }
            FieldModel fieldModel = new FieldModel(requestModel, f.getName());

            String getterName = BeanNameUtil.toGetterName(field.getName(), field.getType());
            String getter = getterName + "()";
            if (!requestBean.hasMethodMatching(AMethod.with().name(getterName).numArgs(0))) {
                if (!field.isPublic()) {
                    throw new JMutateException(
                            "Field '%s' on request bean %s is not public and does not have a public getter method. Either make public, add a getter, or mark as ignored (make transient, static, native, or add ignore annotation)",
                            f.getName(), requestBean.getUnderlying().getName());
                }
                getter = field.getName();// direct field access
            }
            fieldModel.fieldGetter = getter;
            
            if (field.hasAnnotation(reflectedAnnotationHeaderParam)) {
                fieldModel.paramName = ReflectedAnnotation.from(field.getAnnotation(reflectedAnnotationHeaderParam)).getValueForAttribute("value", field.getName());
                fieldModel.isHeaderParam = true;
            }
            if (field.hasAnnotation(reflectedAnnotationCmdParam)) {
                fieldModel.paramName = ReflectedAnnotation.from(field.getAnnotation(reflectedAnnotationCmdParam)).getValueForAttribute("value", field.getName());
                fieldModel.isBodyParam = true;
            } else if (field.hasAnnotation(reflectedAnnotationQueryParam)) {
                fieldModel.paramName = ReflectedAnnotation.from(field.getAnnotation(reflectedAnnotationQueryParam)).getValueForAttribute("value", field.getName());
                fieldModel.isQueryParam = true;
            } else {
                fieldModel.paramName = field.getName();
                if (!requestModel.isCmd) {
                    fieldModel.isQueryParam = true;
                }//else:use json converter
            }
            requestModel.addField(fieldModel);
        }
    }

    private void extractFields(RequestModel request, JType requestType) {
        // call request builder methods for each field/method exposed
        FindResult<JField> fields = requestType.findFieldsMatching(AJField.with().modifiers(AJModifier.that().isNotStatic().isNotTransient().isNotNative()));
        log("found " + fields.toList().size() + " fields");
        for (JField field: fields) {
            if (field.getAnnotations().contains(sourceAnnotationIgnore)) {
                log("ignoring field:" + field.getName());
                continue;
            }
            FieldModel fieldModel = new FieldModel(request, field.getName());
            String getterName = BeanNameUtil.toGetterName(field.getName(), NameUtil.isBoolean(field.getFullTypeName()));
            String getter = getterName + "()";
            if (!requestType.hasMethodMatching(AJMethod.with().name(getterName).numArgs(0))) {
                log("no method " + getter);
                if (!field.getJModifiers().isPublic()) {
                    throw new JMutateException(
                            "Field '%s' on request bean %s is not public and does not have a public getter method. Either make public, add a getter, or mark as ignored (make transient, static, native, or add ignore annotation)",
                            field.getName(), requestType.getFullName());
                }
                getter = field.getName();// direct field access
            }
            fieldModel.fieldGetter = getter;
            if (field.getAnnotations().contains(sourceAnnotationHeaderParam)) {
                fieldModel.paramName = field.getAnnotations().get(sourceAnnotationHeaderParam).getValueForAttribute("value", field.getName());
                fieldModel.isHeaderParam = true;
            }
            if (field.getAnnotations().contains(sourceAnnotationCmdParam)) {
                fieldModel.paramName = field.getAnnotations().get(sourceAnnotationCmdParam).getValueForAttribute("value", field.getName());
                fieldModel.isBodyParam = true;
            } else if (field.getAnnotations().contains(souceAnnotationQueryParam)) {
                fieldModel.paramName = field.getAnnotations().get(souceAnnotationQueryParam).getValueForAttribute("value", field.getName());
                fieldModel.isQueryParam = true;
            } else {
                fieldModel.paramName = field.getName();
                if (!request.isCmd) {
                    fieldModel.isQueryParam = true;
                }//else:use json converter
            }
            
            request.addField(fieldModel);
        }
    }
    
    private static List<PathExpression> extractRestPaths(Class<?> requestType) {
        Path restPath = requestType.getAnnotation(Path.class);
        javax.ws.rs.Path path = requestType.getAnnotation(javax.ws.rs.Path.class);
        return extractRestPaths(restPath,path);
    }
    
    private List<PathExpression> extractRestPaths(JType requestType) {
        Path restPath = null;
        JAnnotation restPathNode = requestType.getAnnotations().get(Path.class);
        if (restPathNode != null) {
            restPath = (Path) ctxt.getAnnotationCompiler().toCompiledAnnotation(restPathNode.getAstNode());
        }
        javax.ws.rs.Path javaxPath = null;
        JAnnotation pathNode = requestType.getAnnotations().get(javax.ws.rs.Path.class);
        if (pathNode != null) {
            javaxPath = (javax.ws.rs.Path) ctxt.getAnnotationCompiler().toCompiledAnnotation(pathNode.getAstNode());
        }
        return extractRestPaths(restPath,javaxPath);
    }
    
    private static List<PathExpression> extractRestPaths(Path restPath,javax.ws.rs.Path javaxPath) {
        List<PathExpression> expressions = new ArrayList<>();
        if (restPath != null) {
            for (String expression : restPath.value()) {
                expressions.add(PathExpression.parse(expression));
            }
        }
        if (javaxPath != null) {
            expressions.add(PathExpression.parse(javaxPath.value()));
        }
        return expressions;
    }
    
    private JType createJsonWriterType(RequestModel request) {
        return ctxt
                .newTempSourceTemplate()
                .pl("public static interface " + request.jsonWriterTypeSimple + " extends com.github.nmorel.gwtjackson.client.ObjectWriter<"
                        + request.requestTypeFull + "> {}").asJTypeSnippet();
    }

    private JType createJsonReaderType(RequestModel info) {
        return ctxt
                .newTempSourceTemplate()
                .pl("public static interface " + info.jsonReaderTypeSimple + " extends com.github.nmorel.gwtjackson.client.ObjectReader<" + info.responseTypeSimple
                        + "> {}").asJTypeSnippet();
    }

    private static String getOr(String val, String defaultVal) {
        if (Strings.isNullOrEmpty(val)) {
            return defaultVal;
        }
        return val;
    }

    private void addGeneratedMarkers(SourceTemplate template) {
        String genInfo = CODE_GEN_INFO_CLASS_NAME + "." + getClass().getSimpleName();
        template.var("generator", genInfo);
        template.pl("@" + javax.annotation.Generated.class.getName() + "(${generator})");
        template.pl("@" + IsGenerated.class.getName() + "(generator=${generator})");
    }

    private void createGenInfoIfNotExists() {
        
        RootResource resource = ctxt.getDefaultGenerationRoot().getResource(CODE_GEN_INFO_CLASS_PKG + "." + CODE_GEN_INFO_CLASS_NAME + ".java");
        JSourceFile source;
        if (!resource.exists()) {
            source = ctxt.newSourceTemplate()
                    .var("pkg", CODE_GEN_INFO_CLASS_PKG)
                    .var("className", CODE_GEN_INFO_CLASS_NAME)
                    .pl("package ${pkg};")
                    .pl("public class ${className} {}")
                    .asSourceFileSnippet();
        } else {
            source = JSourceFile.fromResource(resource, ctxt.getParser());
        }
        JField field = ctxt.newSourceTemplate()
                .pl("public static final String " + getClass().getSimpleName() + "=\"" + GeneratorAppInfo.all + " " + getClass().getName() + "\";")
                .asJFieldSnippet();

        ctxt.obtain(InsertFieldTransform.class).target(source.getMainType()).field(field).clashStrategy(ClashStrategy.REPLACE).transform();

        writeToDiskIfChanged(source);
    }

    private void addToInterface(JTypeMutator interfaceType, JMethod m) {
        if (!interfaceType.getJType().isInterface()) {
            throw new JMutateException("expected interface type but instead got " + interfaceType.getJType().getFullName());
        }

        JMethod interfaceMethod = ctxt.newSourceTemplate()
        // .pl("/** Generated method to build an http request */")
                .p(m.toInterfaceMethodSignature()).asJMethodInterfaceSnippet();

        addOrReplaceMethod(interfaceType, interfaceMethod);
    }

    private void addOrReplaceMethod(JTypeMutator targetType, JMethod m) {
        ctxt.obtain(InsertMethodTransform.class).target(targetType.getJType()).method(m.getAstNode()).clashStrategy(ClashStrategy.REPLACE).transform();
    }

    private void addOrReplaceType(JTypeMutator targetType, JType addType) {
        ctxt.obtain(InsertTypeTransform.class).target(targetType.getJType()).setType(addType).clashStrategy(ClashStrategy.REPLACE).transform();
    }

    private void writeToDiskIfChanged(JSourceFile source) {
        if (source != null) {
            cleanupImports(source.getAstNode());
            source = source.asMutator(ctxt).writeModificationsToDisk();
        }
    }

    private void cleanupImports(ASTNode node) {
        ctxt.obtain(CleanImportsTransform.class)
            .addMissingImports(true)
            .nodeToClean(node)
            .transform();
    }

    private void log(String msg) {
        log.debug(msg);
    }

    /**
     * Details about the client being generated
     */
    private static class ServiceModel {
        final GenerateCqrsGwtClient options;
        final String pkg;
        final String serviceTypeFull;
        final String serviceTypeSimple;
        final String baseTypeFull;
        final String baseTypeSimple;
        final String interfaceTypeFull;
        final String interfaceTypeSimple;
        final String optionsDeclaredInTypeFull;
        final String builderTypeFull;
        final String errorTypeFull;
        final boolean generateInterface;
        final boolean generateInterfaceBuilderMethods;

        private final List<RequestModel> requests = new ArrayList<>();
        
        public ServiceModel(JType declaredInNode, GenerateCqrsGwtClient options) {
            this.options = options;
            this.optionsDeclaredInTypeFull = declaredInNode.getFullName();
            this.serviceTypeFull = getOr(options.serviceName(), declaredInNode.getFullName());
            this.serviceTypeSimple = ClassNameUtil.extractSimpleClassNamePart(serviceTypeFull);
            this.pkg = ClassNameUtil.extractPkgPartOrNull(serviceTypeFull);
            this.baseTypeFull = options.serviceBaseClass().getName();
            this.baseTypeSimple = ClassNameUtil.extractSimpleClassNamePart(baseTypeFull);
            this.interfaceTypeFull = getOr(options.serviceInterfaceName(), (pkg == null ? "" : pkg + ".") + "I" + serviceTypeSimple).replace("Abstract", "").replace(
                    "Default", "");
            this.interfaceTypeSimple = ClassNameUtil.extractSimpleClassNamePart(interfaceTypeFull);
            this.errorTypeFull = options.serviceException().getName();
            this.builderTypeFull = AbstractCqrsGwtClient.class.getName() + ".Builder";
            this.generateInterface = options.generateInterface();
            this.generateInterfaceBuilderMethods = options.generateInterfaceBuildMethods();

        }

        void addRequest(RequestModel request){
            this.requests.add(request);
        }
        
    }

    /**
     * Holds the details about an individual request bean
     */
    private static class RequestModel {
        private static final Matcher<Method> validateMethodMatcher = AMethod.with().name("validate").numArgs(0).isPublic();
        private static final Matcher<JMethod> validateMethodMatcherSource = AJMethod.with().name("validate").numArgs(0).isPublic();
        
        final ServiceModel serviceModel;

        final boolean isCmd;
        final String argName;
        final String requestTypeFull;
        final String requestTypeSimple;
        final String jsonWriterTypeSimple;
        final String responseTypeSimple;
        final String jsonReaderTypeSimple;
        final String asyncRequestHandleTypeFull;
        final String validateMethodName;
        final private Map<String, FieldModel> fields = new LinkedHashMap<>();
        
        List<PathExpression> restPaths = new ArrayList<>();
         
        RequestModel(ServiceModel parent, Class<?> requestType) {
            this.serviceModel = parent;
            // TODO:pull from annotations
            this.isCmd = cmdRequestPattern.matches(requestType.getName());
            //this.requestType = requestType;
            this.requestTypeFull = NameUtil.compiledNameToSourceName(requestType);
            this.requestTypeSimple = requestType.getSimpleName();
            this.argName = isCmd ? "cmd" : "query";
            this.responseTypeSimple = requestType.getName() + ".Response";
            this.jsonReaderTypeSimple = requestType.getSimpleName() + "JsonResponseReader";
            this.jsonWriterTypeSimple = requestType.getSimpleName() + "JsonRequestWriter";
            this.asyncRequestHandleTypeFull = NameUtil.compiledNameToSourceName(parent.options.asyncRequestHandle());
            this.validateMethodName = extractValidateMethod(requestType);
            //this.restPaths = extractRestPaths(requestType);
            
        }
        
        RequestModel(ServiceModel parent, JType requestType) {
            this.serviceModel = parent;
            // TODO:pull from annotations
            this.isCmd = cmdRequestPattern.matches(requestType.getSimpleName());
            //this.requestType = requestType;
            this.requestTypeFull = requestType.getFullName();
            this.requestTypeSimple = requestType.getSimpleName();
            this.argName = isCmd ? "cmd" : "query";
            this.responseTypeSimple = requestType.getFullName() + ".Response";
            this.jsonReaderTypeSimple = requestType.getSimpleName() + "JsonResponseReader";
            this.jsonWriterTypeSimple = requestType.getSimpleName() + "JsonRequestWriter";
            this.asyncRequestHandleTypeFull = NameUtil.compiledNameToSourceName(parent.options.asyncRequestHandle());
            this.validateMethodName = extractValidateMethod(requestType);
            //this.restPaths = extractRestPaths(requestType);
        }
        
        void addField(FieldModel field){
            if (hasNamedField(field.paramName)) {
                throw new JMutateException("More than one field with the same param name '%s' on %s", field.paramName, requestTypeFull);
            }
            fields.put(field.paramName, field);
        }
        
        boolean hasNamedField(String name){
            return fields.containsKey(name);
        }
        
        FieldModel getNamedField(String name){
            return fields.get(name);
        }
        
        Collection<FieldModel> getFields(){
            return fields.values();
        }
        
        private static String extractValidateMethod(Class<?> requestType){
            ReflectedClass beanClass = ReflectedClass.from(requestType);
            FindResult<Method> result = beanClass.findMethodsMatching(validateMethodMatcher);
            if (!result.isEmpty()) {
                Method m = result.getFirst();
                return m.getName();
            }
            return null;
        }
        
        private static String extractValidateMethod(JType requestType){
            FindResult<JMethod> result = requestType.findMethodsMatching(validateMethodMatcherSource);
            if (!result.isEmpty()) {
                JMethod m = result.getFirst();
                return m.getName();
            }
            return null;
        }
  
    }

    private static class FieldModel {
        final RequestModel requestModel;
        final String fieldName;
        String fieldGetter;
        String paramName;
        boolean isHeaderParam;
        boolean isQueryParam;
        boolean isBodyParam;
        
        FieldModel(RequestModel parent, String fieldName) {
            this.requestModel = parent;
            this.fieldName = fieldName;
            this.paramName = fieldName;
        }
    }

}