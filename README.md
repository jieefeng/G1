<a name="O2w7u"></a>
# 一丶测试程序
**每秒进行一百次的操作（可能为生成对象或者释放对象 巨大对象的操作次数会少于正常对象）**
```java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class G1GCDemo2 {
    private static final List<byte[]> longLivedObjects = new ArrayList<>();
    private static final int NUM_LARGE_OBJECTS = 50;
    private static final int LARGE_OBJECT_SIZE = 2048 * 1024; // 2 MB

    public static void main(String[] args) throws Exception {

        Random random = new Random();

        List<byte[]> objects = new ArrayList<>();

        long start = System.currentTimeMillis();

        long end = start + 60 * 1000; // 运行1分钟

        // 分配一些长生命周期的大对象
        for (int i = 0; i < NUM_LARGE_OBJECTS; i++) {
            longLivedObjects.add(new byte[LARGE_OBJECT_SIZE]);
        }

        while (System.currentTimeMillis() < end) {
            if (random.nextBoolean()) {
                int r = random.nextInt(3);
                // 分配新的对象
                if(r != 1){
                    // 分配大对象
                    if(random.nextBoolean()){
                        longLivedObjects.add(new byte[LARGE_OBJECT_SIZE]);
                    } else {
                        // 释放一些对象
                        longLivedObjects.remove(random.nextInt(longLivedObjects.size()));
                    }
                } else {
                    // 分配随机大小的数组
                    objects.add(new byte[random.nextInt(100 * 1024)]); // 0 - 100k
                }
            } else if (!objects.isEmpty()) {
                // 释放一些对象
                objects.remove(random.nextInt(objects.size()));
            }

            // 短暂休眠以防止CPU过高
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("a");
    }
}
```

<a name="RTbTh"></a>
# 二丶日志分析

<a name="jMpFy"></a>
## 默认参数启动

<a name="ULl6d"></a>
### 启动参数
java -Xlog:gc*:file=gc.log G1GCDemo2.java<br />由日志可得（<br />Heap Region Size: 2M 区域大小<br />Heap Min Capacity: 8M 最小的堆大小<br />Heap Initial Capacity: 244M 初始化的堆大小<br />Heap Max Capacity: 3890M	最大的堆大小<br />）<br />**由官方文档可得**<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721228186539-01c9e9be-11ad-47c7-840d-29f495ac492b.png#averageHue=%23f6f6f5&clientId=uf890166e-d3f1-4&from=paste&height=817&id=u75e6f934&originHeight=1634&originWidth=1860&originalType=binary&ratio=2&rotation=0&showTitle=false&size=342497&status=done&style=none&taskId=u7bc8dd0b-991b-4551-b651-d0d8031b563&title=&width=930)
<a name="whcLE"></a>
### 日志分析

<a name="fKpDt"></a>
#### 初始化
```css
[0.012s][info][gc,init] Version: 17.0.11+1-LTS (release)
[0.012s][info][gc,init] CPUs: 16 total, 16 available
[0.012s][info][gc,init] Memory: 15556M
[0.012s][info][gc,init] Large Page Support: Disabled
[0.012s][info][gc,init] NUMA Support: Disabled
[0.012s][info][gc,init] Compressed Oops: Enabled (Zero based)
[0.012s][info][gc,init] Heap Region Size: 2M
[0.012s][info][gc,init] Heap Min Capacity: 8M
[0.012s][info][gc,init] Heap Initial Capacity: 244M
[0.012s][info][gc,init] Heap Max Capacity: 3890M
[0.012s][info][gc,init] Pre-touch: Disabled
[0.012s][info][gc,init] Parallel Workers: 13
[0.012s][info][gc,init] Concurrent Workers: 3
[0.012s][info][gc,init] Concurrent Refinement Workers: 13
[0.012s][info][gc,init] Periodic GC: Disabled
```
Heap Region Size: 2M 每个区域大小为2M<br />Heap Min Capacity: 8M 总区域最小为8M<br />Heap Initial Capacity: 244M 总区域初始化为244M<br />Heap Max Capacity: 3890M 总区域最大为3890M

<a name="AGdxj"></a>
#### GC0
```css
[0.624s][info][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.625s][info][gc,task     ] GC(0) Using 5 workers of 13 for evacuation
[0.634s][info][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.1ms
[0.634s][info][gc,phases   ] GC(0)   Merge Heap Roots: 0.2ms
[0.634s][info][gc,phases   ] GC(0)   Evacuate Collection Set: 8.7ms
[0.634s][info][gc,phases   ] GC(0)   Post Evacuate Collection Set: 0.7ms
[0.634s][info][gc,phases   ] GC(0)   Other: 0.7ms
[0.634s][info][gc,heap     ] GC(0) Eden regions: 11->0(12)
[0.634s][info][gc,heap     ] GC(0) Survivor regions: 0->2(2)
[0.634s][info][gc,heap     ] GC(0) Old regions: 0->1
[0.634s][info][gc,heap     ] GC(0) Archive regions: 0->0
[0.634s][info][gc,heap     ] GC(0) Humongous regions: 0->0
[0.634s][info][gc,metaspace] GC(0) Metaspace: 9525K(9728K)->9525K(9728K) NonClass: 8390K(8512K)->8390K(8512K) Class: 1134K(1216K)->1134K(1216K)
[0.635s][info][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 22M->5M(244M) 10.522ms
[0.635s][info][gc,cpu      ] GC(0) User=0.03s Sys=0.03s Real=0.01s
```
Pause Young (Normal) (G1 Evacuation Pause) 这是一个年轻代暂停<br />Eden regions: 11->0(12) eden直接归0<br />Survivor regions: 0->2(2) survivor多出2个对象<br />Old regions: 0->1 old一个对象<br />Archive regions: 0->0 不变<br />Humongous regions: 0->0 不变<br />Pause Young (Normal) (G1 Evacuation Pause) 22M->5M(244M) 10.522ms 堆内存从22m减少到5m 耗时10.522ms<br />GC(0) User=0.03s Sys=0.03s Real=0.01s CPU资源消耗少和应用程序暂停时间都较少

<a name="zBzf1"></a>
#### GC1
```css
[0.704s][info][gc,start    ] GC(1) Pause Young (Concurrent Start) (G1 Humongous Allocation)
[0.704s][info][gc,task     ] GC(1) Using 5 workers of 13 for evacuation
[0.708s][info][gc,phases   ] GC(1)   Pre Evacuate Collection Set: 0.1ms
[0.708s][info][gc,phases   ] GC(1)   Merge Heap Roots: 0.0ms
[0.708s][info][gc,phases   ] GC(1)   Evacuate Collection Set: 3.5ms
[0.708s][info][gc,phases   ] GC(1)   Post Evacuate Collection Set: 0.4ms
[0.708s][info][gc,phases   ] GC(1)   Other: 0.3ms
[0.708s][info][gc,heap     ] GC(1) Eden regions: 1->0(27)
[0.708s][info][gc,heap     ] GC(1) Survivor regions: 2->1(2)
[0.708s][info][gc,heap     ] GC(1) Old regions: 1->3
[0.708s][info][gc,heap     ] GC(1) Archive regions: 0->0
[0.708s][info][gc,heap     ] GC(1) Humongous regions: 54->54
[0.708s][info][gc,metaspace] GC(1) Metaspace: 9661K(9920K)->9661K(9920K) NonClass: 8503K(8640K)->8503K(8640K) Class: 1158K(1280K)->1158K(1280K)
[0.708s][info][gc          ] GC(1) Pause Young (Concurrent Start) (G1 Humongous Allocation) 113M->113M(244M) 4.564ms
[0.708s][info][gc,cpu      ] GC(1) User=0.05s Sys=0.02s Real=0.01s
```
因为大对象过多 所以说垃圾回收阶段的Concurrent阶段开始<br />Pause Young (Concurrent Start) (G1 Humongous Allocation)   这种类型的收集除了执行正常的年轻收集外，还会启动标记过程。并发标记确定旧生成区域中所有当前可访问的（活动）对象，以保留用于下一个空间回收阶段。虽然收集标记尚未完全完成，但可能会发生正常的年轻收集。标记以两个特殊的停顿结束：备注（Remark）和清理（Cleanup）。（原文如下：Concurrent Start : This type of collection starts the marking process in addition to performing a Normal young collection. Concurrent marking determines all currently reachable (live) objects in the old generation regions to be kept for the following space-reclamation phase. While collection marking hasn’t completely finished, Normal young collections may occur. Marking finishes with two special stop-the-world pauses: Remark and Cleanup. ）<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721213489636-77e3fb48-3c26-4838-a9d5-73ade646a49c.png#averageHue=%23fefefd&clientId=uf890166e-d3f1-4&from=paste&height=368&id=u6377594a&originHeight=736&originWidth=1305&originalType=binary&ratio=2&rotation=0&showTitle=false&size=120494&status=done&style=none&taskId=ua89be210-62a6-43c6-9ba5-9c185f26f86&title=&width=652.5)
<a name="qk7tb"></a>
#### GC2
```css
[0.709s][info][gc          ] GC(2) Concurrent Mark Cycle
[0.709s][info][gc,marking  ] GC(2) Concurrent Clear Claimed Marks
[0.709s][info][gc,marking  ] GC(2) Concurrent Clear Claimed Marks 0.024ms
[0.709s][info][gc,marking  ] GC(2) Concurrent Scan Root Regions
[0.711s][info][gc,marking  ] GC(2) Concurrent Scan Root Regions 1.922ms
[0.711s][info][gc,marking  ] GC(2) Concurrent Mark
[0.711s][info][gc,marking  ] GC(2) Concurrent Mark From Roots
[0.711s][info][gc,task     ] GC(2) Using 3 workers of 3 for marking
[0.712s][info][gc,marking  ] GC(2) Concurrent Mark From Roots 1.580ms
[0.712s][info][gc,marking  ] GC(2) Concurrent Preclean
[0.712s][info][gc,marking  ] GC(2) Concurrent Preclean 0.158ms
[0.712s][info][gc,start    ] GC(2) Pause Remark
[0.713s][info][gc          ] GC(2) Pause Remark 125M->125M(244M) 0.876ms
[0.713s][info][gc,cpu      ] GC(2) User=0.00s Sys=0.00s Real=0.00s
[0.713s][info][gc,marking  ] GC(2) Concurrent Mark 2.880ms
[0.713s][info][gc,marking  ] GC(2) Concurrent Rebuild Remembered Sets
[0.715s][info][gc,marking  ] GC(2) Concurrent Rebuild Remembered Sets 1.249ms
[0.716s][info][gc,start    ] GC(2) Pause Cleanup
[0.716s][info][gc          ] GC(2) Pause Cleanup 133M->133M(244M) 0.076ms
[0.716s][info][gc,cpu      ] GC(2) User=0.00s Sys=0.00s Real=0.00s
[0.716s][info][gc,marking  ] GC(2) Concurrent Cleanup for Next Mark
[0.717s][info][gc,marking  ] GC(2) Concurrent Cleanup for Next Mark 1.359ms
[0.717s][info][gc          ] GC(2) Concurrent Mark Cycle 8.806ms
```


Concurrent Mark Cycle  表示GC操作进入了并发标记周期  

第一阶段<br />Concurrent Clear Claimed Marks 0.024ms: 表示并发清除之前标记的对象 耗时相当少<br />Concurrent Scan Root Regions 1.922ms: 表示并发扫描根区域的操作 耗时1.922毫秒。  <br />Concurrent Mark: 表示进入并发标记阶段。  <br />Concurrent Mark From Roots: 表示从根开始并发标记  <br />Using 3 workers of 3 for marking: 表示有3个工作线程参与标记操作。  <br />Concurrent Mark From Roots 1.580ms: 表示从根开始并发标记的操作耗时1.580毫秒。  

第二阶段<br />Concurrent Preclean 0.158ms: 表示并发预清理的操作耗时0.158毫秒  

第三阶段<br />Pause Remark: 表示暂停并进行重新标记（Remark）  <br />Pause Remark 125M->125M(244M) 0.876ms: 表示重新标记阶段将堆内存从125MB处理为125MB（总容量244MB），耗时0.876毫秒  <br />Concurrent Rebuild Remembered Sets 1.249ms: 表示并发重建记忆集的操作耗时1.249毫秒

第四阶段<br />Pause Cleanup 133M->133M(244M) 0.076ms: 表示清理阶段将堆内存从133MB处理为133MB（总容量244MB），耗时0.076毫秒。  

第五阶段<br />Concurrent Cleanup for Next Mark 1.359ms: 表示并发清理的操作耗时1.359毫秒  	

第六阶段<br />Concurrent Cleanup for Next Mark: 表示并发清理以准备下一次标记  

Concurrent Mark Cycle 8.806ms: 表示整个并发标记周期耗时8.806毫秒  

至此GC日志里面每种类型的日志都已讲述完毕 

<a name="HNub9"></a>
#### 吞吐量与最大暂停时间
![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721226710494-1805be8d-2adc-456f-9673-019a9cf9c1bb.png#averageHue=%23f2f2f2&clientId=uf890166e-d3f1-4&from=paste&height=555&id=u49c46809&originHeight=1109&originWidth=2454&originalType=binary&ratio=2&rotation=0&showTitle=false&size=174322&status=done&style=none&taskId=ub529396a-389d-4453-9b93-b2b6d0ba010&title=&width=1227)<br />借助GCeasy 地址：[https://gceasy.io/index.jsp#features](https://gceasy.io/index.jsp#features)<br />可见我们的吞吐量为99.824% 最大暂停时间为10ms <br />所以说我们的程序目前性能是极其优秀的 

所以说我们接下来修改JVM参数以展示不同参数对吞吐量和最大暂停时间的影响

<a name="Xble2"></a>
## 修改参数
影响垃圾回收器性能的因素主要有两类，一类是总堆，一类是年轻一代

<a name="Dgpf2"></a>
### 修改后的测试程序
```java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class G1GCDemo2 {
    private static final List<byte[]> longLivedObjects = new ArrayList<>();
    private static final int NUM_LARGE_OBJECTS = 50;
    private static final int LARGE_OBJECT_SIZE = 2048 * 1024; // 2 MB

    public static void main(String[] args) throws Exception {

        Random random = new Random();

        List<byte[]> objects = new ArrayList<>();

        long start = System.currentTimeMillis();

        long end = start + 60 * 1000; // 运行1分钟
        while (System.currentTimeMillis() < end) {
            if (random.nextBoolean()) {
                int r = random.nextInt(3);
                // 分配新的对象
                if(r == 1){
                    if(random.nextBoolean()){
                        // 释放一些对象
                        if(longLivedObjects.size() != 0)
                            longLivedObjects.remove(random.nextInt(longLivedObjects.size()));

                    } else {
                        // 分配大对象
                        longLivedObjects.add(new byte[LARGE_OBJECT_SIZE]);
                    }
                } else {
                    // 分配随机大小的数组
                    objects.add(new byte[random.nextInt(100 * 1024)]); // 0 - 100k
                }
            } else if (!objects.isEmpty()) {
                // 释放一些对象
                if(objects.size() != 0)
                    objects.remove(random.nextInt(objects.size()));
            }

            // 短暂休眠以防止CPU过高
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("a");
    }
}
```

<a name="SdEcg"></a>
### 总堆
<a name="q03d8"></a>
#### 堆的大小
许多参数会影响生成规模。下图说明了堆中已提交空间和虚拟空间之间的区别。初始化虚拟机时，将保留堆的整个空间。可以使用该 `-Xmx` 选项指定保留空间的大小。如果 `-Xms` 参数的值小于 `-Xmx` 参数的值，则不会立即将所有保留的空间提交到虚拟机。在此图中，未提交的空间被标记为“虚拟”。堆的不同部分，即老一代和年轻一代，可以根据需要增长到虚拟空间的极限。<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721228725701-0afc0089-294f-462d-9471-94e8a90eefa6.png#averageHue=%23f7f7f7&clientId=uf890166e-d3f1-4&from=paste&height=221&id=uce935363&originHeight=442&originWidth=1490&originalType=binary&ratio=2&rotation=0&showTitle=false&size=110490&status=done&style=none&taskId=uf60d236a-ce8f-42ad-9d6f-e5a47f24ecc&title=&width=745)

<a name="xpfyT"></a>
##### java -Xms128m -Xmx128m -Xlog:gc*:file=gc1.log G1GCDemo2.java
![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721234552144-2bd24d7c-532c-48cc-9381-b04cff080cbd.png#averageHue=%23f4f4f4&clientId=uf890166e-d3f1-4&from=paste&height=696&id=u726dc7ee&originHeight=1392&originWidth=2614&originalType=binary&ratio=2&rotation=0&showTitle=false&size=162657&status=done&style=none&taskId=u2d41e821-a7b7-4925-887c-7425a23b553&title=&width=1307)<br />吞吐量为99.154%<br />最大暂停时间为10.0ms

<a name="qDXmw"></a>
##### java -Xms256m -Xmx256m -Xlog:gc*:file=gc2.log G1GCDemo2.java
![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721234957563-2fea15d8-e534-4a23-a6d7-c9b2aa9588c1.png#averageHue=%23f4f4f3&clientId=uf890166e-d3f1-4&from=paste&height=620&id=u0a48694b&originHeight=1239&originWidth=2557&originalType=binary&ratio=2&rotation=0&showTitle=false&size=144183&status=done&style=none&taskId=u08f355e5-eca5-4399-8a4d-40f14fde63b&title=&width=1278.5)<br />吞吐量为99.94% 这几乎已经无法提升了<br />最大暂停时间仍然为10ms 且平均暂停时间提升了

<a name="ceNkD"></a>
##### java -Xms512m -Xmx512m -Xlog:gc*:file=gc3.log G1GCDemo2.java
![image.png](https://cdn.nlark.com/yuque/0/2024/png/38451585/1721235207607-e7f35d08-2a2d-4945-89ac-d74caa5092e2.png#averageHue=%23f4f3f3&clientId=uf890166e-d3f1-4&from=paste&height=590&id=u8fd9abd8&originHeight=1180&originWidth=2568&originalType=binary&ratio=2&rotation=0&showTitle=false&size=133852&status=done&style=none&taskId=ucded3f6a-5472-4a3a-a1d2-d6119cb8752&title=&width=1284)<br />吞吐量为99.96% 提升十分少<br />最大暂停时间仍然为10ms 且平均暂停时间提升了

由此可见 堆大小的提升 可以提升吞吐量但是相应的也增加了平均暂停时间

<a name="oMae1"></a>
### 
