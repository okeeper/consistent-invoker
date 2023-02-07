package com.okeeper.consistentinvoker.core.dto;

/**
 * @author zhangyue
 */
public class AfterCommitResult {

    private Object result;

    private Throwable throwable;

    public boolean hasException() {
        return throwable != null;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
