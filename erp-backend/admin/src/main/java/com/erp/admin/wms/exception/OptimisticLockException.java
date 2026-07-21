package com.erp.admin.wms.exception;

/**
 * 乐观锁冲突异常
 *
 * @author erp
 */
public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException() {
        super("乐观锁冲突，请重试");
    }

    public OptimisticLockException(String message) {
        super(message);
    }

}
