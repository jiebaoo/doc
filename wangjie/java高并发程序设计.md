#JAVA高并发程序设计《学习笔记》
##第一章 走入并行世界
1. 并行计算适用范围：图像处理，服务端编程
2. 基本概念：同步、异步、并发、并行、临界区、阻塞、非阻塞、死锁、饥饿、活锁
3. 并发级别：阻塞、无饥饿、无障碍、无锁、无等待
4. Amdahl定律（代码并行化）与Gustafson定律（CPU数）
5. JAVA内存模型JMM多线程设计：原子性、可见性、有序性
##第二章 java并行程序基础
###一、线程生命周期
1. NEW 新建
2. RUNNABLE 执行
3. BLOCKED 阻塞
4. WAITING 等待
5. TIMED_WAITING 限时等待
6. TERMINATED 结束
###二、线程基本操作
1. 新建线程
<br>`Thread t = new Thread();`</br>
`Thread t = new Thread(){@override public void run{}}`</br>
`Thread t = new Thread(Runnable target);`</br>
`t.start();//创建线程，并让线程执行run方法`</br>
`t.run();//在当前线程串行执行run方法`
2. 终止线程
<br>`Thread.stop() //废弃`</br>
在线程中自定义变量判断是否需要终止线程 无法响应wait,sleep
3. 线程中断
<br>`public void Thread.interrupt //中断线程`</br>
`public boolean Thread.isInterrupted //判断线程是否中断`</br>
`public static boolean Thread.interrupted //判断线程是否中断，并清除中断状态`</br>
4. 等待和通知
<br>`public finall void wait() throws InterruptedException //等待`</br>
`public final native void notify() //通知`</br>
`public final native void notifyAll() //通知全部`</br>
sleep不释放任何资源，wait会释放资源
5. 挂起和继续执行
<br>`Thread.suspend() //废弃`</br>
`Thread.resume() //废弃`</br>
suspend不释放资源，resume如果在suspend之前执行了，挂起的线程很难释放
6. 等待线程结束和谦让
</br>`public final void join() throws InterruptedException //等待线程结束`</br>
</br>`public final synchronized void join(long millis) throws InterruptedException //限时等待线程结束`</br>
`public static native void yield() //谦让`</br>
###三、volatile与java内存模型JMM
Volatile 变量具有 synchronized 的可见性特性，但是不具备原子特性。
###四、线程组
ThreadGroup
</br>`new Thread(ThreadGroup group, Runnable target, String name)`</br>
###五、守护线程
`Thread.setDaemon(true) //用户线程都结束后，只剩下守护线程，java虚拟机自然退出`
###六、线程优先级
`Thread.setPriority(Thread.MAX_PRIORITY) //非公平锁优先级高先执行的概率大`
###七、线程安全
1. synchronized</br>
对象：指定对象加锁</br>
实例方法：当前对象实例加锁</br>
静态方法：当前类加锁</br>
2. Vector代替ArrayList
3. ConcurrentHashMap代替HashMap
4. Integer不能作为锁对象
##第三章 JDK并行包
###一、同步控制
1. ReenterLock重入锁</br>
同一线程可以多次获得同一个锁</br>
`ReenterLock lock = new ReenterLock();//创建`</br>
`ReenterLock lock = new ReenterLock(true);//创建公平锁`</br>
`lock.lock();//加锁`</br>
`lock.lockInterruptibly();//加锁，优先响应中断`</br>
`lock.tryLock();//尝试加锁`</br>
`lock.unlock();//解锁`</br>
2. Condition条件</br>
`Condition condition = lock.newCondition;//创建重入锁条件`</br>
`condition.await();//等待`</br>
`condition.awaitUninterruptibly();//等待，不响应中断`</br>
`condition.signal();//唤醒`</br>
`condition.signalAll();//唤醒全部`</br>
3. Semaphore信号量</br>
允许多个线程同时访问</br>
`public Semaphore(int permits) //指定同时访问数量`</br>
`public Semaphore(int permits, boolean fair) //fair指定是否公平`</br>
`public void acquire() //获取一个准入许可`</br>
`public void acquireUninterruptibly() //获取一个准入许可 不响应中断`</br>
`public boolean tryAcquire() //尝试获取一个准入许可`</br>
`public boolean tryAcquire(long timeout, TimeUnit unit) //定时尝试获取一个准入许可`</br>
`public void release() //释放一个准入许可`</br>
4. ReadWriteLock读写锁</br>