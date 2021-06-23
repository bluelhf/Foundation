package mx.kenzie.foundation;

import mx.kenzie.foundation.error.PropertyCalculationError;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.RETURN;

public class MethodBuilder implements SubBuilder {
    
    protected final ClassBuilder builder;
    protected CodeWriter writer = new CodeWriter();
    protected String name;
    protected Type returnType;
    protected List<Type> parameters = new ArrayList<>();
    protected int modifiers;
    
    public MethodBuilder(ClassBuilder builder, String name) {
        this.builder = builder;
        this.name = name;
        this.returnType = new Type(void.class);
        this.modifiers = Modifier.PUBLIC;
    }
    
    //region Type
    public MethodBuilder setReturnType(Class<?> type) {
        this.returnType = new Type(type);
        return this;
    }
    
    public MethodBuilder setReturnType(Type type) {
        this.returnType = type;
        return this;
    }
    //endregion
    
    //region Parameters
    public MethodBuilder addParameter(Class<?>... classes) {
        for (Class<?> aClass : classes) {
            parameters.add(new Type(aClass));
        }
        return this;
    }
    
    public MethodBuilder addParameter(Type... types) {
        this.parameters.addAll(Arrays.asList(types));
        return this;
    }
    //endregion
    
    //region Modifiers
    @Override
    public MethodBuilder setModifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }
    
    @Override
    public MethodBuilder addModifiers(int... modifiers) {
        for (int modifier : modifiers) {
            this.modifiers = this.modifiers | modifier;
        }
        return this;
    }
    
    @Override
    public MethodBuilder removeModifiers(int... modifiers) {
        for (int modifier : modifiers) {
            this.modifiers = this.modifiers & ~modifier;
        }
        return this;
    }
    //endregion
    
    //region Code
    public MethodBuilder writeCode(WriteInstruction... instructions) {
        this.writer.addInstruction(instructions);
        return this;
    }
    //endregion
    
    //region Inherited
    @Override
    public ClassBuilder finish() {
        return builder;
    }
    //endregion
    
    void compile(ClassWriter writer) {
        final StringBuilder builder = new StringBuilder().append("(");
        for (Type parameter : parameters) {
            builder.append(parameter.descriptor());
        }
        builder.append(")").append(returnType.descriptor());
        final MethodVisitor methodVisitor;
        methodVisitor = writer.visitMethod(modifiers, name, builder.toString(), null, null);
        write_code:
        {
            if (Modifier.isAbstract(modifiers)) break write_code;
            methodVisitor.visitCode();
            if (this.writer.isEmpty()) {
                methodVisitor.visitInsn(RETURN);
            } else {
                this.writer.write(methodVisitor);
            }
            try {
                methodVisitor.visitMaxs(0, 0);
            } catch (NegativeArraySizeException ex) {
                throw new PropertyCalculationError("Potential stack underflow detected while calculating frames.", ex);
            }
        }
        methodVisitor.visitEnd();
    }
}
