package com.uber.cadence.internal.testing;

import com.uber.cadence.testing.TestActivityEnvironment;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import static com.uber.cadence.internal.common.LambdaUtils.getTarget;
import static com.uber.cadence.internal.common.LambdaUtils.toSerializedLambda;

public class TestActivityEnvironmentInternal implements TestActivityEnvironment {

//    private Method findInterfaceMethod(Object function) {
//        SerializedLambda lambda = toSerializedLambda(function);
//        if (lambda == null || lambda.getImplMethodKind() == MethodHandleInfo.REF_invokeInterface) {
//            throw new IllegalArgumentException("Expected activity implementation method reference: " + lambda.getImplMethodSignature())
//        }
//        Object target = getTarget(lambda);
//        if (target == null) {
//            throw new IllegalArgumentException("Expected activity implementation method reference: " + lambda.getImplMethodSignature())
//        }
//
//    }

    @Override
    public <R> R executeActivity(Functions.Func<R> function) {
        return null;
    }

    @Override
    public <A1, R> R executeActivity(Functions.Func1<A1, R> function, A1 arg1) {
        return null;
    }

    @Override
    public <A1, A2, R> R executeActivity(Functions.Func2<A1, A2, R> function, A1 arg1, A2 arg2) {
        return null;
    }

    @Override
    public <A1, A2, A3, R> R executeActivity(Functions.Func3<A1, A2, A3, R> function, A1 arg1, A2 arg2, A3 arg3) {
        return null;
    }

    @Override
    public <A1, A2, A3, A4, R> R executeActivity(Functions.Func4<A1, A2, A3, A4, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
        return null;
    }

    @Override
    public <A1, A2, A3, A4, A5, R> R executeActivity(Functions.Func5<A1, A2, A3, A4, A5, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
        return null;
    }

    @Override
    public <A1, A2, A3, A4, A5, A6, R> R executeActivity(Functions.Func6<A1, A2, A3, A4, A5, A6, R> function, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6) {
        return null;
    }

    @Override
    public Promise<Void> executeActivity(Functions.Proc activity) {
        return null;
    }

    @Override
    public <A1> Promise<Void> executeActivity(Functions.Proc1<A1> activity, A1 arg1) {
        return null;
    }

    @Override
    public <A1, A2> Promise<Void> executeActivity(Functions.Proc2<A1, A2> activity, A1 arg1, A2 arg2) {
        return null;
    }

    @Override
    public <A1, A2, A3> Promise<Void> executeActivity(Functions.Proc3<A1, A2, A3> activity, A1 arg1, A2 arg2, A3 arg3) {
        return null;
    }

    @Override
    public <A1, A2, A3, A4> Promise<Void> executeActivity(Functions.Proc4<A1, A2, A3, A4> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4) {
        return null;
    }

    @Override
    public <A1, A2, A3, A4, A5> Promise<Void> executeActivity(Functions.Proc5<A1, A2, A3, A4, A5> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5) {
        return null;
    }

    @Override
    public <A1, A2, A3, A4, A5, A6> Promise<Void> executeActivity(Functions.Proc6<A1, A2, A3, A4, A5, A6> activity, A1 arg1, A2 arg2, A3 arg3, A4 arg4, A5 arg5, A6 arg6) {
        return null;
    }
}
