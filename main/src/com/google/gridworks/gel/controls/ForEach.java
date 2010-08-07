package com.google.gridworks.gel.controls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.gridworks.expr.EvalError;
import com.google.gridworks.expr.Evaluable;
import com.google.gridworks.expr.ExpressionUtils;
import com.google.gridworks.gel.Control;
import com.google.gridworks.gel.ControlFunctionRegistry;
import com.google.gridworks.gel.ast.VariableExpr;

public class ForEach implements Control {
    public String checkArguments(Evaluable[] args) {
        if (args.length != 3) {
            return ControlFunctionRegistry.getControlName(this) + " expects 3 arguments";
        } else if (!(args[1] instanceof VariableExpr)) {
            return ControlFunctionRegistry.getControlName(this) + 
                " expects second argument to be a variable name";
        }
        return null;
    }

    public Object call(Properties bindings, Evaluable[] args) {
        Object o = args[0].evaluate(bindings);
        if (ExpressionUtils.isError(o)) {
            return o;
        } else if (!ExpressionUtils.isArrayOrCollection(o) && !(o instanceof JSONArray)) {
            return new EvalError("First argument to forEach is not an array");
        }
        
        String name = ((VariableExpr) args[1]).getName();
        
        Object oldValue = bindings.get(name);
        try {
            List<Object> results = null;
            
            if (o.getClass().isArray()) {
                Object[] values = (Object[]) o;
                
                results = new ArrayList<Object>(values.length);
                for (Object v : values) {
                    bindings.put(name, v);
                    
                    Object r = args[2].evaluate(bindings);
                    
                    results.add(r);
                }
            } else if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                int l = a.length();
                
                results = new ArrayList<Object>(l);
                for (int i = 0; i < l; i++) {
                    try {
                        Object v = a.get(i);
                        
                        bindings.put(name, v);
                        
                        Object r = args[2].evaluate(bindings);
                        
                        results.add(r);
                    } catch (JSONException e) {
                        results.add(new EvalError(e.getMessage()));
                    }
                }
            } else {
                Collection<Object> collection = ExpressionUtils.toObjectCollection(o);
                
                results = new ArrayList<Object>(collection.size());
                
                for (Object v : collection) {
                    bindings.put(name, v);
                    
                    Object r = args[2].evaluate(bindings);
                    
                    results.add(r);
                }
            }
            
            return results.toArray(); 
        } finally {
            /*
             *  Restore the old value bound to the variable, if any.
             */
            if (oldValue != null) {
                bindings.put(name, oldValue);
            } else {
                bindings.remove(name);
            }
        }
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "Evaluates expression a to an array. Then for each array element, binds its value to variable name v, evaluates expression e, and pushes the result onto the result array."
        );
        writer.key("params"); writer.value("expression a, variable v, expression e");
        writer.key("returns"); writer.value("array");
        writer.endObject();
    }
}
