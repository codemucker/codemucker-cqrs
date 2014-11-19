package org.codemucker.cqrs;

import java.util.ArrayList;
import java.util.List;

public class PathExpression {
    
    public final String expression;
    private final List<Var> vars = new ArrayList<>();
    private final List<Object> parts = new ArrayList<>();

    public static PathExpression parse(String s) {
        if(!s.startsWith("/")){
            s = "/" + s;
        }
        int max = s.length() - 1;
        PathExpression result = new PathExpression(s);
        StringBuilder sb = new StringBuilder();
        int pathPartCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '$' && i < max && s.charAt(i + 1) == '{') {
                i += 2;
                int start = i;
                // look for end
                while (i < max && s.charAt(i) != '}') {
                    i++;
                }
                if (sb.length() > 0) {
                    result.addStaticPart(sb.toString());
                    sb.setLength(0);
                }
                int end = i;
                String varName = s.substring(start, end);
                result.addVarPart(varName,pathPartCount-1);
            } else if(c=='/'){
                pathPartCount++;
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            result.addStaticPart(sb.toString());
        }
        return result;
    }

    private PathExpression(String expression) {
        this.expression = expression;
    }

    private void addStaticPart(String s) {
        parts.add(s);
    }

    private void addVarPart(String varName, int pathPartCount) {
        Var var = new Var(varName,pathPartCount);
        this.parts.add(var);
        this.vars.add(var);
    }

    public Var getVarNamed(String name){
        if(!vars.isEmpty()){
            for(Var var:vars){
                if(var.name.equals(name)){
                    return var;
                }
            }
        }
        return null;
    }
    
    public List<Var> getVars() {
        return vars;
    }

    public List<Object> getAllParts() {
        return parts;
    }

    public static class Var {
        public final String name;
        public final int pathPartCount;

        Var(String name,int pathPartCount) {
            this.name = name;
            this.pathPartCount = pathPartCount;
        }
    }
}