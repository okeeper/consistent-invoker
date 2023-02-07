package com.okeeper.consistentinvoker.utils;

/**
 * 生成斐波拉契的进阶数字
 * @author zhangyue
 */
public class FibonacciUtils {

    public static int fib(int n)
    {
        if (n <= 1){
            return n;
        }
        //return fib(n-1) + fib(n-2);
        return Double.valueOf(
                (Math.pow((1 + Math.sqrt(5)) / 2, n) - Math.pow((1 - Math.sqrt(5)) / 2, n))
                        / Math.sqrt(5)
        ).intValue();
    }

    public static void main (String args[])
    {
        int t = 0;
        for(int i=0;i<5000;i++) {
            t = t + fib(i) * 2;
            System.out.println(i + ":" + fib(i));
        }
        System.out.println(t);
    }
}
