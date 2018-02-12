/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.uber.cadence.internal.dispatcher;

import com.uber.cadence.workflow.CancellationScope;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

class CancellationScopeImpl implements CancellationScope {

    private static ThreadLocal<Stack<CancellationScopeImpl>> scopeStack = ThreadLocal.withInitial(Stack::new);
    private boolean ignoreParentCancellation;

    static CancellationScopeImpl current() {
        if (scopeStack.get().empty()) {
            return null;
        }
        return scopeStack.get().peek();
    }

    private static void pushCurrent(CancellationScopeImpl scope) {
        scopeStack.get().push(scope);
    }

    private static void popCurrent(CancellationScopeImpl expected) {
        CancellationScopeImpl current = scopeStack.get().pop();
        if (current != expected) {
            throw new Error("Unexpected scope");
        }
        if (!current.ignoreParentCancellation) {
            current.parent.removeChild(current);
        }
    }

    private final Runnable runnable;
    private CancellationScopeImpl parent;
    private final Set<CancellationScopeImpl> children = new HashSet<>();
    /**
     * When disconnected scope has no parent and thus doesn't receive cancellation requests from it.
     */
    private boolean cancelRequested;
    private String reason;

    CancellationScopeImpl(Runnable runnable) {
        this.runnable = runnable;
        setParent(current());
    }

    private void setParent(CancellationScopeImpl parent) {
        if (parent == null) {
            setIgnoreParentCancellation(true);
            return;
        }
        this.parent = parent;
        parent.addChild(this);
        if (parent.isCancelRequested()) {
            cancel(parent.getCancellationReason());
        }
    }

    void run() {
        try {
            pushCurrent(this);
            runnable.run();
        } finally {
            popCurrent(this);
        }
    }

    @Override
    public void setIgnoreParentCancellation(boolean flag) {
        if (flag) {
            ignoreParentCancellation = true;
            if (parent != null) {
                parent.removeChild(this);
            }
        } else {
            parent.addChild(this);
        }
    }

    @Override
    public boolean isIgnoreParentCancellation() {
        return ignoreParentCancellation;
    }

    @Override
    public void cancel() {
        cancelRequested = true;
        reason = null;
        for (CancellationScopeImpl child : children) {
            child.cancel();
        }
    }

    @Override
    public void cancel(String reason) {
        cancelRequested = true;
        this.reason = reason;
        for (CancellationScopeImpl child : children) {
            child.cancel(reason);
        }
    }

    @Override
    public String getCancellationReason() {
        return reason;
    }

    @Override
    public boolean resetCanceled() {
        boolean result = cancelRequested;
        cancelRequested = false;
        for (CancellationScopeImpl child : children) {
            child.resetCanceled();
        }
        return result;
    }

    @Override
    public boolean isCancelRequested() {
        return cancelRequested;
    }

    @Override
    public boolean isDone(boolean skipChildren) {
        return false;
    }

    private void addChild(CancellationScopeImpl scope) {
        if (!children.add(scope)) {
            throw new Error("Already a child");
        }
    }

    private void removeChild(CancellationScopeImpl scope) {
        if (!children.remove(scope)) {
            throw new Error("Not a child");
        }
    }
}
