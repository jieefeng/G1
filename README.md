# 一丶模拟内存分配与垃圾回收场景
**每分钟进行100次操作（可能是分配或者释放）**
```java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class G1GCDemo2 {
    private static final List<byte[]> longLivedObjects = new ArrayList<>();
    private static final int NUM_LARGE_OBJECTS = 50;
    private static final int LARGE_OBJECT_SIZE = 2048 * 1024; // 1 MB

    public static void main(String[] args) throws Exception {

        Random random = new Random();

        List<byte[]> objects = new ArrayList<>();

        long start = System.currentTimeMillis();

        long end = start + 60 * 1000 * 10; // 运行10分钟

        while (System.currentTimeMillis() < end) {
            if (random.nextBoolean()) {
                int r = random.nextInt(3);
                // 分配新的对象
                if(r != 1){
                    // 分配大对象
                    objects.add(new byte[LARGE_OBJECT_SIZE]);
                } else {
                    // 分配随机大小的数组
                    objects.add(new byte[random.nextInt(100 * 1024)]);
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

        System.out.println("程序运行结束");
    }
}
```
## 
# 二丶日志分析

## 默认启动条件

### 启动参数
-XX:+UseG1GC "<br />-XX:+PrintGCDetails<br />-javaagent:"D:\Program Files\JetBrains\IntelliJ IDEA 2023.2.2\lib\idea_rt.jar=57461:D:\Program Files\JetBrains\IntelliJ IDEA 2023.2.2\bin" -Dfile.encoding=UTF-8 <br />-classpath D:\AAComputerCourse\AACode\G1\target\classes G1GCDemo 

### 日志分析
#### 初始参数
```java
[0.004s][warning][gc] -XX:+PrintGCDetails is deprecated. Will use -Xlog:gc* instead.
[0.009s][info   ][gc] Using G1
[0.011s][info   ][gc,init] Version: 17.0.11+1-LTS (release)
[0.011s][info   ][gc,init] CPUs: 16 total, 16 available
[0.011s][info   ][gc,init] Memory: 15556M
[0.011s][info   ][gc,init] Large Page Support: Disabled
[0.011s][info   ][gc,init] NUMA Support: Disabled
[0.011s][info   ][gc,init] Compressed Oops: Enabled (Zero based)
[0.011s][info   ][gc,init] Heap Region Size: 2M
[0.011s][info   ][gc,init] Heap Min Capacity: 8M
[0.011s][info   ][gc,init] Heap Initial Capacity: 244M
[0.011s][info   ][gc,init] Heap Max Capacity: 3890M
[0.011s][info   ][gc,init] Pre-touch: Disabled
[0.011s][info   ][gc,init] Parallel Workers: 13
[0.011s][info   ][gc,init] Concurrent Workers: 3
[0.011s][info   ][gc,init] Concurrent Refinement Workers: 13
[0.011s][info   ][gc,init] Periodic GC: Disabled
[0.019s][info   ][gc,metaspace] CDS archive(s) mapped at: [0x000001f8d8000000-0x000001f8d8bc0000-0x000001f8d8bc0000), size 12320768, SharedBaseAddress: 0x000001f8d8000000, ArchiveRelocationMode: 1.
[0.019s][info   ][gc,metaspace] Compressed class space mapped at: 0x000001f8d9000000-0x000001f919000000, reserved size: 1073741824
[0.019s][info   ][gc,metaspace] Narrow klass base: 0x000001f8d8000000, Narrow klass shift: 0, Narrow klass range: 0x100000000
```
**Heap Region Size: 2M** 每个区域大小为2M<br />**Heap Min Capacity: 8M **总区域最小为8M<br />**Heap Initial Capacity: 244M ** 总区域初始化为244M<br />**Heap Max Capacity: 3890M **总区域最大为3890M<br />**[0.011s][info   ][gc,init] Concurrent Refinement Workers: 13 **13个并发标记线程

#### GC日志分析
##### 总日志
```java
"D:\Program Files\Java\TencentKona-17.0.11.b1\bin\java.exe" -XX:+PrintGCDetails "-javaagent:D:\Program Files\JetBrains\IntelliJ IDEA 2023.2.2\lib\idea_rt.jar=57461:D:\Program Files\JetBrains\IntelliJ IDEA 2023.2.2\bin" -Dfile.encoding=UTF-8 -classpath D:\AAComputerCourse\AACode\G1\target\classes G1GCDemo "-javaagent:D:\Program Files\JetBrains\IntelliJ IDEA 2023.2.2\lib\idea_rt.jar=61479:D:\Program Files\JetBrains\IntelliJ IDEA 2023.2.2\bin" G1GCDemo3
[0.004s][warning][gc] -XX:+PrintGCDetails is deprecated. Will use -Xlog:gc* instead.
[0.009s][info   ][gc] Using G1
[0.011s][info   ][gc,init] Version: 17.0.11+1-LTS (release)
[0.011s][info   ][gc,init] CPUs: 16 total, 16 available
[0.011s][info   ][gc,init] Memory: 15556M
[0.011s][info   ][gc,init] Large Page Support: Disabled
[0.011s][info   ][gc,init] NUMA Support: Disabled
[0.011s][info   ][gc,init] Compressed Oops: Enabled (Zero based)
[0.011s][info   ][gc,init] Heap Region Size: 2M
[0.011s][info   ][gc,init] Heap Min Capacity: 8M
[0.011s][info   ][gc,init] Heap Initial Capacity: 244M
[0.011s][info   ][gc,init] Heap Max Capacity: 3890M
[0.011s][info   ][gc,init] Pre-touch: Disabled
[0.011s][info   ][gc,init] Parallel Workers: 13
[0.011s][info   ][gc,init] Concurrent Workers: 3
[0.011s][info   ][gc,init] Concurrent Refinement Workers: 13
[0.011s][info   ][gc,init] Periodic GC: Disabled
[0.019s][info   ][gc,metaspace] CDS archive(s) mapped at: [0x000001f8d8000000-0x000001f8d8bc0000-0x000001f8d8bc0000), size 12320768, SharedBaseAddress: 0x000001f8d8000000, ArchiveRelocationMode: 1.
[0.019s][info   ][gc,metaspace] Compressed class space mapped at: 0x000001f8d9000000-0x000001f919000000, reserved size: 1073741824
[0.019s][info   ][gc,metaspace] Narrow klass base: 0x000001f8d8000000, Narrow klass shift: 0, Narrow klass range: 0x100000000
[10.849s][info   ][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[10.849s][info   ][gc,task     ] GC(0) Using 5 workers of 13 for evacuation
[10.851s][info   ][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.1ms
[10.852s][info   ][gc,phases   ] GC(0)   Merge Heap Roots: 0.2ms
[10.852s][info   ][gc,phases   ] GC(0)   Evacuate Collection Set: 1.5ms
[10.852s][info   ][gc,phases   ] GC(0)   Post Evacuate Collection Set: 0.2ms
[10.852s][info   ][gc,phases   ] GC(0)   Other: 0.4ms
[10.852s][info   ][gc,heap     ] GC(0) Eden regions: 11->0(18)
[10.852s][info   ][gc,heap     ] GC(0) Survivor regions: 0->1(2)
[10.852s][info   ][gc,heap     ] GC(0) Old regions: 0->0
[10.852s][info   ][gc,heap     ] GC(0) Archive regions: 0->0
[10.852s][info   ][gc,heap     ] GC(0) Humongous regions: 0->0
[10.852s][info   ][gc,metaspace] GC(0) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[10.852s][info   ][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 21M->1M(244M) 2.483ms
[10.852s][info   ][gc,cpu      ] GC(0) User=0.00s Sys=0.00s Real=0.00s
[30.704s][info   ][gc,start    ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[30.704s][info   ][gc,task     ] GC(1) Using 5 workers of 13 for evacuation
[30.707s][info   ][gc,phases   ] GC(1)   Pre Evacuate Collection Set: 0.1ms
[30.707s][info   ][gc,phases   ] GC(1)   Merge Heap Roots: 0.0ms
[30.707s][info   ][gc,phases   ] GC(1)   Evacuate Collection Set: 2.5ms
[30.707s][info   ][gc,phases   ] GC(1)   Post Evacuate Collection Set: 0.3ms
[30.707s][info   ][gc,phases   ] GC(1)   Other: 0.2ms
[30.707s][info   ][gc,heap     ] GC(1) Eden regions: 18->0(70)
[30.707s][info   ][gc,heap     ] GC(1) Survivor regions: 1->3(3)
[30.707s][info   ][gc,heap     ] GC(1) Old regions: 0->1
[30.707s][info   ][gc,heap     ] GC(1) Archive regions: 0->0
[30.707s][info   ][gc,heap     ] GC(1) Humongous regions: 0->0
[30.708s][info   ][gc,metaspace] GC(1) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[30.708s][info   ][gc          ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 37M->6M(244M) 3.115ms
[30.708s][info   ][gc,cpu      ] GC(1) User=0.00s Sys=0.00s Real=0.00s
[115.575s][info   ][gc,start    ] GC(2) Pause Young (Normal) (G1 Evacuation Pause)
[115.575s][info   ][gc,task     ] GC(2) Using 5 workers of 13 for evacuation
[115.577s][info   ][gc,phases   ] GC(2)   Pre Evacuate Collection Set: 0.1ms
[115.578s][info   ][gc,phases   ] GC(2)   Merge Heap Roots: 0.1ms
[115.578s][info   ][gc,phases   ] GC(2)   Evacuate Collection Set: 2.1ms
[115.578s][info   ][gc,phases   ] GC(2)   Post Evacuate Collection Set: 0.3ms
[115.578s][info   ][gc,phases   ] GC(2)   Other: 0.2ms
[115.578s][info   ][gc,heap     ] GC(2) Eden regions: 70->0(70)
[115.578s][info   ][gc,heap     ] GC(2) Survivor regions: 3->3(10)
[115.578s][info   ][gc,heap     ] GC(2) Old regions: 1->1
[115.578s][info   ][gc,heap     ] GC(2) Archive regions: 0->0
[115.578s][info   ][gc,heap     ] GC(2) Humongous regions: 0->0
[115.578s][info   ][gc,metaspace] GC(2) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[115.578s][info   ][gc          ] GC(2) Pause Young (Normal) (G1 Evacuation Pause) 146M->6M(244M) 2.751ms
[115.578s][info   ][gc,cpu      ] GC(2) User=0.00s Sys=0.00s Real=0.00s
[209.980s][info   ][gc,start    ] GC(3) Pause Young (Normal) (G1 Evacuation Pause)
[209.980s][info   ][gc,task     ] GC(3) Using 5 workers of 13 for evacuation
[209.983s][info   ][gc,phases   ] GC(3)   Pre Evacuate Collection Set: 0.1ms
[209.983s][info   ][gc,phases   ] GC(3)   Merge Heap Roots: 0.0ms
[209.983s][info   ][gc,phases   ] GC(3)   Evacuate Collection Set: 2.0ms
[209.983s][info   ][gc,phases   ] GC(3)   Post Evacuate Collection Set: 0.3ms
[209.983s][info   ][gc,phases   ] GC(3)   Other: 0.2ms
[209.983s][info   ][gc,heap     ] GC(3) Eden regions: 70->0(69)
[209.983s][info   ][gc,heap     ] GC(3) Survivor regions: 3->4(10)
[209.983s][info   ][gc,heap     ] GC(3) Old regions: 1->1
[209.983s][info   ][gc,heap     ] GC(3) Archive regions: 0->0
[209.983s][info   ][gc,heap     ] GC(3) Humongous regions: 0->0
[209.983s][info   ][gc,metaspace] GC(3) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[209.983s][info   ][gc          ] GC(3) Pause Young (Normal) (G1 Evacuation Pause) 146M->7M(244M) 2.700ms
[209.983s][info   ][gc,cpu      ] GC(3) User=0.00s Sys=0.00s Real=0.00s
[298.284s][info   ][gc,start    ] GC(4) Pause Young (Normal) (G1 Evacuation Pause)
[298.284s][info   ][gc,task     ] GC(4) Using 5 workers of 13 for evacuation
[298.287s][info   ][gc,phases   ] GC(4)   Pre Evacuate Collection Set: 0.1ms
[298.287s][info   ][gc,phases   ] GC(4)   Merge Heap Roots: 0.1ms
[298.287s][info   ][gc,phases   ] GC(4)   Evacuate Collection Set: 1.8ms
[298.287s][info   ][gc,phases   ] GC(4)   Post Evacuate Collection Set: 0.4ms
[298.287s][info   ][gc,phases   ] GC(4)   Other: 0.2ms
[298.287s][info   ][gc,heap     ] GC(4) Eden regions: 69->0(70)
[298.287s][info   ][gc,heap     ] GC(4) Survivor regions: 4->3(10)
[298.287s][info   ][gc,heap     ] GC(4) Old regions: 1->1
[298.287s][info   ][gc,heap     ] GC(4) Archive regions: 0->0
[298.287s][info   ][gc,heap     ] GC(4) Humongous regions: 0->0
[298.287s][info   ][gc,metaspace] GC(4) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[298.287s][info   ][gc          ] GC(4) Pause Young (Normal) (G1 Evacuation Pause) 145M->5M(244M) 2.736ms
[298.287s][info   ][gc,cpu      ] GC(4) User=0.02s Sys=0.00s Real=0.00s
[389.875s][info   ][gc,start    ] GC(5) Pause Young (Normal) (G1 Evacuation Pause)
[389.875s][info   ][gc,task     ] GC(5) Using 5 workers of 13 for evacuation
[389.876s][info   ][gc,phases   ] GC(5)   Pre Evacuate Collection Set: 0.1ms
[389.876s][info   ][gc,phases   ] GC(5)   Merge Heap Roots: 0.1ms
[389.876s][info   ][gc,phases   ] GC(5)   Evacuate Collection Set: 1.3ms
[389.876s][info   ][gc,phases   ] GC(5)   Post Evacuate Collection Set: 0.3ms
[389.876s][info   ][gc,phases   ] GC(5)   Other: 0.2ms
[389.876s][info   ][gc,heap     ] GC(5) Eden regions: 70->0(72)
[389.876s][info   ][gc,heap     ] GC(5) Survivor regions: 3->1(10)
[389.876s][info   ][gc,heap     ] GC(5) Old regions: 1->1
[389.876s][info   ][gc,heap     ] GC(5) Archive regions: 0->0
[389.876s][info   ][gc,heap     ] GC(5) Humongous regions: 0->0
[389.876s][info   ][gc,metaspace] GC(5) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[389.877s][info   ][gc          ] GC(5) Pause Young (Normal) (G1 Evacuation Pause) 145M->2M(244M) 2.013ms
[389.877s][info   ][gc,cpu      ] GC(5) User=0.00s Sys=0.00s Real=0.00s
[472.390s][info   ][gc,start    ] GC(6) Pause Young (Normal) (G1 Evacuation Pause)
[472.390s][info   ][gc,task     ] GC(6) Using 5 workers of 13 for evacuation
[472.392s][info   ][gc,phases   ] GC(6)   Pre Evacuate Collection Set: 0.1ms
[472.392s][info   ][gc,phases   ] GC(6)   Merge Heap Roots: 0.1ms
[472.392s][info   ][gc,phases   ] GC(6)   Evacuate Collection Set: 1.5ms
[472.392s][info   ][gc,phases   ] GC(6)   Post Evacuate Collection Set: 0.3ms
[472.392s][info   ][gc,phases   ] GC(6)   Other: 0.3ms
[472.392s][info   ][gc,heap     ] GC(6) Eden regions: 72->0(72)
[472.392s][info   ][gc,heap     ] GC(6) Survivor regions: 1->1(10)
[472.392s][info   ][gc,heap     ] GC(6) Old regions: 1->1
[472.392s][info   ][gc,heap     ] GC(6) Archive regions: 0->0
[472.392s][info   ][gc,heap     ] GC(6) Humongous regions: 0->0
[472.392s][info   ][gc,metaspace] GC(6) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[472.392s][info   ][gc          ] GC(6) Pause Young (Normal) (G1 Evacuation Pause) 146M->2M(244M) 2.320ms
[472.392s][info   ][gc,cpu      ] GC(6) User=0.00s Sys=0.00s Real=0.00s
[555.378s][info   ][gc,start    ] GC(7) Pause Young (Normal) (G1 Evacuation Pause)
[555.378s][info   ][gc,task     ] GC(7) Using 5 workers of 13 for evacuation
[555.380s][info   ][gc,phases   ] GC(7)   Pre Evacuate Collection Set: 0.1ms
[555.380s][info   ][gc,phases   ] GC(7)   Merge Heap Roots: 0.1ms
[555.380s][info   ][gc,phases   ] GC(7)   Evacuate Collection Set: 1.5ms
[555.380s][info   ][gc,phases   ] GC(7)   Post Evacuate Collection Set: 0.3ms
[555.380s][info   ][gc,phases   ] GC(7)   Other: 0.2ms
[555.380s][info   ][gc,heap     ] GC(7) Eden regions: 72->0(70)
[555.380s][info   ][gc,heap     ] GC(7) Survivor regions: 1->3(10)
[555.380s][info   ][gc,heap     ] GC(7) Old regions: 1->1
[555.380s][info   ][gc,heap     ] GC(7) Archive regions: 0->0
[555.380s][info   ][gc,heap     ] GC(7) Humongous regions: 0->0
[555.380s][info   ][gc,metaspace] GC(7) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[555.380s][info   ][gc          ] GC(7) Pause Young (Normal) (G1 Evacuation Pause) 146M->5M(244M) 2.123ms
[555.380s][info   ][gc,cpu      ] GC(7) User=0.00s Sys=0.00s Real=0.00s
程序运行结束
[600.085s][info   ][gc,heap,exit] Heap
[600.085s][info   ][gc,heap,exit]  garbage-first heap   total 249856K, used 90021K [0x000000070ce00000, 0x0000000800000000)
[600.085s][info   ][gc,heap,exit]   region size 2048K, 44 young (90112K), 3 survivors (6144K)
[600.085s][info   ][gc,heap,exit]  Metaspace       used 999K, committed 1216K, reserved 1114112K
[600.085s][info   ][gc,heap,exit]   class space    used 82K, committed 192K, reserved 1048576K

进程已结束，退出代码为 0

```
##### GC0
```java
[10.849s][info   ][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[10.849s][info   ][gc,task     ] GC(0) Using 5 workers of 13 for evacuation
[10.851s][info   ][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.1ms
[10.852s][info   ][gc,phases   ] GC(0)   Merge Heap Roots: 0.2ms
[10.852s][info   ][gc,phases   ] GC(0)   Evacuate Collection Set: 1.5ms
[10.852s][info   ][gc,phases   ] GC(0)   Post Evacuate Collection Set: 0.2ms
[10.852s][info   ][gc,phases   ] GC(0)   Other: 0.4ms
[10.852s][info   ][gc,heap     ] GC(0) Eden regions: 11->0(18)
[10.852s][info   ][gc,heap     ] GC(0) Survivor regions: 0->1(2)
[10.852s][info   ][gc,heap     ] GC(0) Old regions: 0->0
[10.852s][info   ][gc,heap     ] GC(0) Archive regions: 0->0
[10.852s][info   ][gc,heap     ] GC(0) Humongous regions: 0->0
[10.852s][info   ][gc,metaspace] GC(0) Metaspace: 997K(1152K)->997K(1152K) NonClass: 915K(960K)->915K(960K) Class: 82K(192K)->82K(192K)
[10.852s][info   ][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 21M->1M(244M) 2.483ms
[10.852s][info   ][gc,cpu      ] GC(0) User=0.00s Sys=0.00s Real=0.00s
```
**GC(0) Eden regions: 11->0(18)  **eden直接归0<br />**GC(0) Survivor regions: 0->1(2) **survivor多出1个对象<br />**GC(0) Old regions: 0->0 **old不变<br />**GC(0) Archive regions: 0->0 **archive 不变<br />**GC(0) Humongous regions: 0->0 **humongous 不变<br />**[10.852s][info   ][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 21M->1M(244M) 2.483ms  **堆内存从21M减少到1M<br />**[10.852s][info   ][gc,cpu      ] GC(0) User=0.00s Sys=0.00s Real=0.00s **结合上面几个阶段的耗时来看不过来2ms的耗时 十分的短 可以忽略不计 

**其他GC阶段的日志大同小异 不再赘述**

##### 堆结束
```java
[600.085s][info   ][gc,heap,exit] Heap
[600.085s][info   ][gc,heap,exit]  garbage-first heap   total 249856K, used 90021K [0x000000070ce00000, 0x0000000800000000)
[600.085s][info   ][gc,heap,exit]   region size 2048K, 44 young (90112K), 3 survivors (6144K)
[600.085s][info   ][gc,heap,exit]  Metaspace       used 999K, committed 1216K, reserved 1114112K
[600.085s][info   ][gc,heap,exit]   class space    used 82K, committed 192K, reserved 1048576K
```

**[600.085s][info   ][gc,heap,exit]  garbage-first heap   total 249856K, used 90021K [0x000000070ce00000, 0x0000000800000000)**<br />**[600.085s][info   ][gc,heap,exit]   region size 2048K, 44 young (90112K), 3 survivors (6144K)  **区域大小为2M，44个区域用于young（eden），3个区域用于survivor<br />**[600.085s][info   ][gc,heap,exit]  Metaspace       used 999K, committed 1216K, reserved 1114112K **  由于我的测试程序几乎没有 类加载或卸载   所以说元空间基本没有使用多少和变化<br />**[600.085s][info   ][gc,heap,exit]   class space    used 82K, committed 192K, reserved 1048576K **类也是几乎没有新增和变化 所以说类空间使用较少和没有变化














